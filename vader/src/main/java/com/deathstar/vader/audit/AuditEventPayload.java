package com.deathstar.vader.audit;

import java.util.Map;

/** Event payload for Audit events sent over NATS. */
public record AuditEventPayload(
        String actorId, // JSON Maps to ClickHouse ActorId
        String action,
        String resourceType,
        String resourceId,
        String status,
        Map<String, String> metadata) {}
