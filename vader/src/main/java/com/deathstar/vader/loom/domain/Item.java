package com.deathstar.vader.loom.domain;

import java.util.Map;
import java.util.UUID;

/**
 * The atomic entity of the Loom Core Engine. Represents projecting the facts/events into a concrete
 * state, stored with property bucketing.
 */
public record Item(
        String tenantId,
        UUID itemId,
        long version,
        Map<UUID, Object> staticAttributes,
        Map<UUID, Object> dynamicAttributes) {}
