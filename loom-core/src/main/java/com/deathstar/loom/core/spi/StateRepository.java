package com.deathstar.loom.core.spi;

import com.deathstar.loom.core.domain.BucketType;
import java.util.Map;
import java.util.UUID;

/**
 * SPI for connecting to the physical state store (e.g., PostgreSQL). Implementations are
 * responsible for performing efficient partial JSONB updates.
 */
public interface StateRepository {

    /**
     * Performs a partial update against the physical database.
     *
     * @param itemId The aggregate root being modified.
     * @param bucketedPatches A map of updates routed by their target BucketType.
     * @param baseVersion The expected base version (for OCC).
     * @return true if the update was successful, false if it failed the baseVersion OCC check.
     */
    boolean partialUpdate(
            UUID itemId,
            String tenantId,
            Map<BucketType, Map<UUID, Object>> bucketedPatches,
            long baseVersion);

    /**
     * Retrieves the current highest version for the given item. Used for initial intent validation
     * and idempotency checks.
     *
     * @param itemId The aggregate root identifier.
     * @return The current version, or 0 if it doesn't exist.
     */
    long getCurrentVersion(UUID itemId);
}
