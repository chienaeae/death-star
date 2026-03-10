package com.deathstar.vader.audit.listener;

import com.deathstar.vader.audit.AuditEvent;
import com.deathstar.vader.audit.AuditEventFactory;
import com.deathstar.vader.audit.AuditEventPayload;
import com.deathstar.vader.audit.service.AuditEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditEventFactory auditEventFactory;
    private final AuditEventPublisher auditEventPublisher;

    /**
     * Intercepts AuditEvents autonomously, transforms them into standardized payloads via the
     * Factory, and streams them to the NATS broker publisher asynchronously without blocking the
     * main caller threads.
     */
    @Async
    @EventListener
    public void handleDomainAuditEvent(AuditEvent<?> event) {
        try {
            AuditEventPayload payload = auditEventFactory.createPayload(event);
            auditEventPublisher.publish(payload);
        } catch (Exception e) {
            log.error("Failed to process AuditEvent and publish to NATS", e);
        }
    }
}
