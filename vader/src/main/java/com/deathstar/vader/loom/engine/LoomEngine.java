package com.deathstar.vader.loom.engine;

import com.deathstar.vader.loom.domain.BucketType;
import com.deathstar.vader.loom.domain.Event;
import com.deathstar.vader.loom.spi.EventStore;
import com.deathstar.vader.loom.spi.EventUpcaster;
import com.deathstar.vader.loom.spi.FieldRegistry;
import com.deathstar.vader.loom.spi.IdentityResolver;
import com.deathstar.vader.loom.spi.StateRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** The Brain. Coordinates Property Bucketing, OCC, Striped Locking, and SPI orchestration. */
public class LoomEngine {

    private final EventStore eventStore;
    private final StateRepository stateRepository;
    private final IdentityResolver identityResolver;
    private final FieldRegistry fieldRegistry;
    private final List<EventUpcaster> upcasters;

    public LoomEngine(
            EventStore eventStore,
            StateRepository stateRepository,
            IdentityResolver identityResolver,
            FieldRegistry fieldRegistry,
            List<EventUpcaster> upcasters) {
        this.eventStore = eventStore;
        this.stateRepository = stateRepository;
        this.identityResolver = identityResolver;
        this.fieldRegistry = fieldRegistry;
        this.upcasters = upcasters;
    }

    /**
     * Processes a property update intent. Validates the version and appends to the EventStore.
     * Database updates are decoupled to the projection worker.
     *
     * @param itemId The item to update.
     * @param fieldId The semantic field.
     * @param newValue The new value.
     * @param baseVersion The client's expected version for OCC.
     */
    public void updateProperty(UUID itemId, UUID fieldId, Object newValue, long baseVersion) {
        long currentVersion = stateRepository.getCurrentVersion(itemId);

        // For this hybrid system, we allow baseVersion 1 (item creation intent) or strict OCC
        // Because of CQRS, currentVersion might lag behind baseVersion during bulk synchronous
        // updates.
        // However, if baseVersion < currentVersion, we strictly KNOW the client has a stale view.
        if (currentVersion != 0 && baseVersion < currentVersion) {
            throw new IllegalStateException(
                    "View out of sync: OCC failed for item "
                            + itemId
                            + " (Expected at least "
                            + currentVersion
                            + ", got "
                            + baseVersion
                            + ")");
        }

        String tenantId = identityResolver.currentTenantId();

        // Note: In a real system, you'd fetch the oldValue to put into this Event
        Object previousValue = null;

        Event event =
                new Event(
                        UUID.randomUUID(),
                        tenantId,
                        itemId,
                        fieldId,
                        "UPDATED",
                        previousValue,
                        newValue,
                        baseVersion);

        // The "Intent" becomes a "Fact"
        eventStore.append(event);
    }

    /**
     * Projects an event into the queryable state representation (Postgres). Called by the
     * background Projection Worker.
     *
     * @param event The event fact to project.
     * @return true if the database was successfully updated (or ACK'd via idempotency).
     */
    public boolean projectEvent(Event event) {
        // 1. Wrap the single attribute update into our routing structure
        Map<BucketType, Map<UUID, Object>> bucketedPatches = new java.util.HashMap<>();
        if (event.fieldId() != null) {
            Map<UUID, Object> singlePatch = new java.util.HashMap<>();
            singlePatch.put(event.fieldId(), event.newValue());
            bucketedPatches = routeAttributes(singlePatch);
        }

        // 3. Update Database (Optimistic Concurrency Control / CAS)
        // We pass event.baseVersion() directly to enforce WHERE version = eventSeq - 1
        return stateRepository.partialUpdate(
                event.itemId(), event.tenantId(), bucketedPatches, event.baseVersion());
    }

    /** Routing Logic using Field Definition Heuristics. */
    private Map<BucketType, Map<UUID, Object>> routeAttributes(Map<UUID, Object> updates) {
        Map<BucketType, Map<UUID, Object>> result = new java.util.HashMap<>();
        for (Map.Entry<UUID, Object> entry : updates.entrySet()) {
            BucketType bucket = fieldRegistry.getField(entry.getKey()).effectiveBucket();
            result.computeIfAbsent(bucket, k -> new java.util.HashMap<>())
                    .put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    /** Example method for querying and upcasting old events. */
    public List<Event> getHistory(UUID itemId) {
        List<Event> rawEvents = eventStore.readStream(itemId);

        return rawEvents.stream()
                .map(
                        event -> {
                            Event processedEvent = event;
                            for (EventUpcaster upcaster : upcasters) {
                                if (upcaster.canUpcast(processedEvent)) {
                                    processedEvent = upcaster.upcast(processedEvent);
                                }
                            }
                            return processedEvent;
                        })
                .collect(Collectors.toList());
    }
}
