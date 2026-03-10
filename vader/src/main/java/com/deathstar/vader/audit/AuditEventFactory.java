package com.deathstar.vader.audit;

import org.springframework.stereotype.Component;

@Component
public class AuditEventFactory {

    public AuditEventPayload createPayload(AuditEvent<?> event) {
        return new AuditEventPayload(
                event.getActorId(),
                event.getAction().name(),
                event.getResourceType().name(),
                event.getResourceId(),
                event.getStatus().name(),
                event.getMetadata());
    }
}
