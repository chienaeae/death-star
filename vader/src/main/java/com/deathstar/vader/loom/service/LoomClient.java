package com.deathstar.vader.loom.service;

import java.util.Map;
import java.util.UUID;

public interface LoomClient {

    /**
     * Creates a new Item with a generated UUID. Emits an ITEM_CREATED event, followed by property
     * update events.
     */
    UUID createItem(String itemType, Map<UUID, Object> initialProperties);

    /**
     * Creates a new Item with a specific UUID. Emits an ITEM_CREATED event, followed by property
     * update events.
     */
    UUID createItemWithId(UUID itemId, String itemType, Map<UUID, Object> initialProperties);

    /**
     * Updates an existing item with multiple property patches. Uses optimistic concurrency control
     * based on the currentVersion.
     *
     * @param itemId The item to update
     * @param updates A map of Field UUIDs to their new values
     * @param currentVersion The expected version for OCC validation
     */
    void updateItem(UUID itemId, Map<UUID, Object> updates, long currentVersion);
}
