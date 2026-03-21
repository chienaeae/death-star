package com.deathstar.vader.event.service;

import com.deathstar.vader.core.tracing.NatsTracingPropagator;
import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventMessage;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedEventSubscriber implements EventSubscriber {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final NatsTracingPropagator tracingPropagator;
    private final ObjectMapper objectMapper;

    @Override
    public void subscribe(
            EventRoute route,
            String subjectSuffix,
            String consumerGroup,
            Consumer<EventMessage> handler) {

        String fullSubject = route.subject(subjectSuffix);
        String subscribeSubject =
                route.isDurable() && ">".equals(subjectSuffix)
                        ? route.wildcardSubject()
                        : fullSubject;

        try {
            if (route.isDurable()) {
                // JetStream requires passing the handler inside the subscribe method natively
                Dispatcher dispatcher = natsConnection.createDispatcher();
                PushSubscribeOptions pso =
                        PushSubscribeOptions.builder().stream(route.stream())
                                .durable(consumerGroup)
                                .build();

                jetStream.subscribe(
                        subscribeSubject,
                        dispatcher,
                        msg -> processMessageWrapper(msg, subscribeSubject, handler),
                        false, // autoAck = false -> Enables manual Ack Control
                        pso);

                log.info(
                        "Initialized durable JetStream subscriber on [{}] with group: [{}]",
                        subscribeSubject,
                        consumerGroup);

            } else {
                // NATS Core allows binding the handler directly to the dispatcher
                Dispatcher dispatcher =
                        natsConnection.createDispatcher(
                                msg -> processMessageWrapper(msg, subscribeSubject, handler));
                dispatcher.subscribe(subscribeSubject, consumerGroup);

                log.info(
                        "Initialized ephemeral NATS Core subscriber on [{}] with group: [{}]",
                        subscribeSubject,
                        consumerGroup);
            }
        } catch (Exception e) {
            log.error("Failed to initialize subscriber for [{}]", subscribeSubject, e);
            throw new RuntimeException("Subscriber initialization failed", e);
        }
    }

    private void processMessageWrapper(
            Message msg, String subscribeSubject, Consumer<EventMessage> handler) {
        tracingPropagator.processMessageWithTracing(
                msg,
                "process_event",
                tracedMsg -> {
                    DomainEvent domainEvent = null;
                    try {
                        domainEvent =
                                objectMapper.readValue(tracedMsg.getData(), DomainEvent.class);
                    } catch (Exception e) {
                        log.error(
                                "Failed to deserialize event payload on [{}]", subscribeSubject, e);
                        tracedMsg.ack(); // Poison pill prevention
                        return;
                    }

                    try {
                        handler.accept(new EventMessage(domainEvent, tracedMsg));
                    } catch (Exception e) {
                        log.error(
                                "Unhandled exception processing subscriber handler for [{}]",
                                subscribeSubject,
                                e);
                        tracedMsg.nak(); // Retry on transient application layer errors
                    }
                });
    }
}
