package com.deathstar.vader.loom.core.domain;

import java.util.UUID;

/**
 * The Fact. An immutable record of change.
 *
 * @param eventId The unique identifier of this event facts.
 * @param tenantId The isolated tenant context.
 * @param itemId The aggregate root being modified.
 * @param fieldId The field being modified.
 * @param eventType the type of the event (e.g., UPDATED, CREATED, DELETED)
 * @param oldValue The state before the event, used for compensating transactions (Undo).
 * @param newValue The state after the event.
 * @param baseVersion The Optimistic Concurrency Control (OCC) guard.
 */
public record Event(
        UUID eventId,
        String tenantId,
        UUID itemId,
        UUID fieldId,
        String eventType,
        Object oldValue,
        Object newValue,
        long baseVersion) {}
