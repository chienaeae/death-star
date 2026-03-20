package com.deathstar.vader.audit.service;

import com.deathstar.vader.audit.AuditEvent;
import com.deathstar.vader.audit.AuditEventFactory;
import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventPublisher;
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
    private final EventPublisher eventPublisher;

    /**
     * Intercepts Spring ApplicationEvents, maps to DomainEvent, and publishes durably via EventPublisher.
     */
    @Async
    @EventListener
    public void handleDomainAuditEvent(AuditEvent<?> event) {
        try {
            DomainEvent domainEvent = auditEventFactory.createPayload(event);
            eventPublisher.publish(EventRoute.AUDIT, "vader", domainEvent);
        } catch (Exception e) {
            log.error("Failed to process AuditEvent and publish to EventPublisher", e);
        }
    }
}
