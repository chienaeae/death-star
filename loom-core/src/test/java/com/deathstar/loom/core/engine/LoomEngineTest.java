package com.deathstar.loom.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.deathstar.loom.core.domain.BucketType;
import com.deathstar.loom.core.domain.Event;
import com.deathstar.loom.core.domain.FieldDefinition;
import com.deathstar.loom.core.spi.EventStore;
import com.deathstar.loom.core.spi.FieldRegistry;
import com.deathstar.loom.core.spi.IdentityResolver;
import com.deathstar.loom.core.spi.StateRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoomEngineTest {

    private LoomEngine engine;
    private MockEventStore eventStore;
    private MockStateRepository stateRepository;

    @BeforeEach
    void setUp() {
        eventStore = new MockEventStore();
        stateRepository = new MockStateRepository();
        IdentityResolver mockIdentity =
                new IdentityResolver() {
                    @Override
                    public String currentTenantId() {
                        return "tenant-1";
                    }

                    @Override
                    public String currentUserId() {
                        return "user-1";
                    }
                };
        FieldRegistry mockRegistry =
                fieldId -> {
                    // Mock definitions for test
                    if (fieldId.toString().endsWith("1")) {
                        return new FieldDefinition(
                                fieldId,
                                "Status",
                                FieldDefinition.FieldType.STATUS,
                                BucketType.DYNAMIC);
                    }
                    return new FieldDefinition(
                            fieldId, "Title", FieldDefinition.FieldType.STRING, BucketType.STATIC);
                };

        engine =
                new LoomEngine(
                        eventStore,
                        stateRepository,
                        mockIdentity,
                        mockRegistry,
                        Collections.emptyList());
    }

    @Test
    void testSuccessfulPropertyUpdateRoutesToCorrectBucket() {
        UUID itemId = UUID.randomUUID();
        // Ends with 1 -> DYNAMIC
        UUID dynamicFieldId = UUID.fromString("00000000-0000-0000-0000-000000000001");

        // Command: Update Property
        engine.updateProperty(itemId, dynamicFieldId, "DONE", 1L);

        // Verify Event was published
        assertEquals(1, eventStore.events.size(), "Should have published 1 event");
        Event e = eventStore.events.get(0);
        assertEquals("tenant-1", e.tenantId());
        assertEquals(dynamicFieldId, e.fieldId());
        assertEquals("DONE", e.newValue());

        // Projection Worker: Project Event
        engine.projectEvent(e);

        // Verify State Repository received bucketed update
        assertTrue(
                stateRepository.lastBuckets.containsKey(BucketType.DYNAMIC),
                "Update should have been routed to DYNAMIC bucket");
        assertEquals(
                "DONE", stateRepository.lastBuckets.get(BucketType.DYNAMIC).get(dynamicFieldId));
    }

    @Test
    void testOccFailureThrowsException() {
        UUID itemId = UUID.randomUUID();
        UUID staticFieldId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        // The mock DB returns version 2, but the command requests baseVersion 1.
        stateRepository.mockCurrentVersion = 2L;

        IllegalStateException ex =
                assertThrows(
                        IllegalStateException.class,
                        () -> engine.updateProperty(itemId, staticFieldId, "New Title", 1L));

        assertTrue(ex.getMessage().contains("OCC failed for item"));
        assertEquals(0, eventStore.events.size(), "Should not publish event on OCC failure");
    }

    // Simple manual mocks instead of pulling in Mockito just for these standard tests
    static class MockEventStore implements EventStore {
        List<Event> events = new ArrayList<>();

        @Override
        public void append(Event event) {
            events.add(event);
        }

        @Override
        public List<Event> readStream(UUID itemId) {
            return Collections.emptyList();
        }
    }

    static class MockStateRepository implements StateRepository {
        boolean simulateUpdateSuccess = true;
        Map<BucketType, Map<UUID, Object>> lastBuckets;
        long mockCurrentVersion = 0L;

        @Override
        public boolean partialUpdate(
                UUID itemId,
                String tenantId,
                Map<BucketType, Map<UUID, Object>> bucketedPatches,
                long baseVersion) {
            this.lastBuckets = bucketedPatches;
            return simulateUpdateSuccess;
        }

        @Override
        public long getCurrentVersion(UUID itemId) {
            return mockCurrentVersion;
        }
    }
}
