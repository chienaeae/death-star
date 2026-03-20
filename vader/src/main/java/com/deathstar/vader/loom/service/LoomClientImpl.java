package com.deathstar.vader.loom.service;

import com.deathstar.vader.loom.api.LoomClient;
import com.deathstar.vader.loom.core.domain.Event;
import com.deathstar.vader.loom.core.engine.LoomEngine;
import com.deathstar.vader.loom.core.spi.EventStore;
import com.deathstar.vader.loom.infrastructure.ScopedValueIdentityResolver;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoomClientImpl implements LoomClient {

    private final LoomEngine loomEngine;
    private final EventStore eventStore;
    private final ScopedValueIdentityResolver identityResolver;

    @Override
    @Transactional
    public UUID createItem(String itemType, Map<UUID, Object> initialProperties) {
        return createItemWithId(UUID.randomUUID(), itemType, initialProperties);
    }

    @Override
    @Transactional
    public UUID createItemWithId(UUID itemId, String itemType, Map<UUID, Object> initialProperties) {
        String tenantId = identityResolver.currentTenantId();

        // Emit Creation Event (baseVersion 0)
        eventStore.append(
                new Event(
                        UUID.randomUUID(),
                        tenantId,
                        itemId,
                        null,
                        "ITEM_CREATED",
                        null,
                        itemType,
                        0L));

        // Now update properties sequentially to initialize it
        long version = 1L;
        if (initialProperties != null) {
            for (Map.Entry<UUID, Object> entry : initialProperties.entrySet()) {
                loomEngine.updateProperty(itemId, entry.getKey(), entry.getValue(), version++);
            }
        }

        return itemId;
    }

    @Override
    @Transactional
    public void updateItem(UUID itemId, Map<UUID, Object> updates, long currentVersion) {
        long version = currentVersion;
        if (updates != null) {
            for (Map.Entry<UUID, Object> entry : updates.entrySet()) {
                loomEngine.updateProperty(itemId, entry.getKey(), entry.getValue(), version++);
            }
        }
    }
}
