package com.deathstar.loom.core.domain;

/**
 * Defines the physical storage routing for an attribute to combat PostgreSQL MVCC write
 * amplification.
 */
public enum BucketType {
    /** For low-frequency changes (e.g., Title, Description). */
    STATIC,

    /**
     * For high-frequency changes (e.g., LexRank, Status). This bucket is kept small to ensure
     * atomic updates are extremely fast.
     */
    DYNAMIC
}
