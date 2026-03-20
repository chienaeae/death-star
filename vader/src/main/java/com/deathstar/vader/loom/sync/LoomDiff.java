package com.deathstar.vader.loom.sync;

import java.util.Map;
import java.util.UUID;

/**
 * Utility for providing the frontend a "Mine vs. Theirs" UI without the engine knowing about UI
 * semantics.
 */
public record LoomDiff(Map<UUID, ValueDiff> fieldDifferences) {
    /** Represents a conflicting value. */
    public record ValueDiff(
            Object clientValue, // "Mine"
            Object serverValue // "Theirs"
            ) {}
}
