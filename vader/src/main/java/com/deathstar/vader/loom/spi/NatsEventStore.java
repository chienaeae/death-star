package com.deathstar.vader.loom.spi;

import com.deathstar.loom.core.domain.Event;
import com.deathstar.loom.core.spi.EventStore;
import io.nats.client.JetStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Vader's implementation of the EventStore using NATS JetStream. */
@Component
@RequiredArgsConstructor
public class NatsEventStore implements EventStore {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(NatsEventStore.class);

    // Expecting JetStream to be configured and injected elsewhere in Vader
    private final JetStream jetStream;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Override
    public void append(Event event) {
        try {
            // Subject hierarchy isolates by tenant id
            String subject = String.format("loom.%s.item.updated", event.tenantId());

            // Serialize to JSON
            byte[] payload = objectMapper.writeValueAsBytes(event);

            jetStream.publish(subject, payload);
            log.debug("Published event {} to NATS {}", event.eventId(), subject);
        } catch (Exception e) {
            log.error("Failed to append event to NATS JetStream", e);
            throw new RuntimeException("Event append failure", e);
        }
    }

    @Override
    public List<Event> readStream(UUID itemId) {
        // Replicating history reading from JetStream requires fetching messages by key
        // Mocking for implementation purposes
        log.warn("readStream not fully implemented with JetStream cursor yet for {}", itemId);
        return Collections.emptyList();
    }
}
