package com.deathstar.loom.core.sync;

import com.deathstar.loom.core.domain.Item;

/** Result of a Drift & Sync operation, yielding whether the sync was successful or conflicting. */
public record SyncResult(Status status, Item serverTruth, LoomDiff diff) {
    public enum Status {
        SUCCESS,
        CONFLICT,
        NOT_FOUND
    }
}
