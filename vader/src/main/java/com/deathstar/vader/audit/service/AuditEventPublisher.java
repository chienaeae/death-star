package com.deathstar.vader.audit.service;

import com.deathstar.vader.audit.AuditEventPayload;
import com.deathstar.vader.tracing.NatsTracingPropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.JetStream;
import io.nats.client.Message;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {
    private final JetStream jetStream;
    private final NatsTracingPropagator tracingPropagator;
    private final ObjectMapper objectMapper;

    public void publish(AuditEventPayload payload) {
        try {
            // Include contextual traceId implicitly in headers
            Headers headers = tracingPropagator.injectContext();

            Message msg =
                    NatsMessage.builder()
                            .subject("audit.events.vader")
                            .headers(headers)
                            .data(objectMapper.writeValueAsBytes(payload))
                            .build();

            jetStream.publish(msg);
            log.debug(
                    "Published audit event: action={}, actorId={}",
                    payload.action(),
                    payload.actorId());
        } catch (Exception e) {
            log.error("Failed to publish audit event for action={}", payload.action(), e);
        }
    }
}
