package com.deathstar.vader.event.service;

import com.deathstar.vader.core.tracing.NatsTracingPropagator;
import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.impl.NatsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedEventPublisher implements EventPublisher {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final NatsTracingPropagator tracingPropagator;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(EventRoute route, String subjectSuffix, DomainEvent event) {
        try {
            byte[] eventData = objectMapper.writeValueAsBytes(event);
            String fullSubject = route.subject(subjectSuffix);

            if (route.isDurable()) {
                Message msg = NatsMessage.builder().subject(fullSubject).data(eventData).build();
                jetStream.publish(msg);
                log.debug(
                        "Published durable event to stream [{}] on subject [{}]",
                        route.stream(),
                        fullSubject);
            } else {
                natsConnection.publish(fullSubject, eventData);
                log.debug("Published ephemeral event to subject [{}]", fullSubject);
            }
        } catch (Exception e) {
            log.error(
                    "Failed to publish event (durable={}): {}", route.isDurable(), event.type(), e);
        }
    }
}
