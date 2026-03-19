package com.deathstar.vader.audit;

import com.deathstar.vader.event.domain.DomainEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuditEventFactory {

    public DomainEvent createPayload(AuditEvent<?> event) {
        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("action", event.getAction().name());
        payloadMap.put("resourceType", event.getResourceType().name());
        payloadMap.put("resourceId", event.getResourceId() != null ? event.getResourceId() : "");
        payloadMap.put("status", event.getStatus().name());
        payloadMap.put("metadata", event.getMetadata());

        return new DomainEvent(
                UUID.randomUUID(), "AUDIT_EVENT", Instant.now(), event.getActorId(), payloadMap);
    }
}
