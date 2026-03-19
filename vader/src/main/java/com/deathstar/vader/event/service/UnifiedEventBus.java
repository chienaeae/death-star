package com.deathstar.vader.event.service;

import com.deathstar.vader.core.tracing.NatsTracingPropagator;
import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventBus;
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
public class UnifiedEventBus implements EventBus {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final NatsTracingPropagator tracingPropagator;
    private final ObjectMapper objectMapper;

    @Override
    public void publishDurable(EventRoute route, String subjectSuffix, DomainEvent event) {
        try {
            Message msg =
                    NatsMessage.builder()
                            .subject(route.subject(subjectSuffix))
                            .data(objectMapper.writeValueAsBytes(event))
                            .build();

            // Store event directly into the JetStream
            jetStream.publish(msg);
            log.debug(
                    "Published durable event to stream [{}] on subject [{}]",
                    route.stream(),
                    route.subject(subjectSuffix));
        } catch (Exception e) {
            log.error("Failed to publish durable event: {}", event.type(), e);
        }
    }

    @Override
    public void publishEphemeral(EventRoute route, DomainEvent event) {
        try {
            byte[] eventData = objectMapper.writeValueAsBytes(event);
            natsConnection.publish(route.subject(""), eventData);
            log.debug("Published ephemeral event to subject [{}]", route.subject(""));
        } catch (Exception e) {
            log.error("Failed to publish ephemeral event: {}", event.type(), e);
        }
    }
}
