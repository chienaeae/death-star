package com.deathstar.vader.loom.infrastructure;

import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventPublisher;
import com.deathstar.vader.loom.core.domain.Event;
import com.deathstar.vader.loom.core.spi.EventStore;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Vader's implementation of the EventStore using unified EventPublisher. */
@Component
@RequiredArgsConstructor
public class NatsEventStore implements EventStore {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(NatsEventStore.class);

    private final EventPublisher eventPublisher;

    @Override
    public void append(Event event) {
        try {
            // Subject hierarchy isolates by tenant id
            String subject = String.format("loom.%s.item.updated", event.tenantId());
            // Adapt Loom's Event into the Unified DomainEvent
            DomainEvent domainEvent =
                    new DomainEvent(
                            event.eventId(),
                            event.eventType(),
                            Instant.now(),
                            event.itemId().toString(),
                            event);

            // Assuming 'span' is available in this context, e.g., from a tracing library
            // span.setAttribute("loom.event.type", domainEvent.type());

            eventPublisher.publish(EventRoute.LOOM, domainEvent.type(), domainEvent);
            log.debug("Published event {} via EventPublisher to {}", event.eventId(), subject);
        } catch (Exception e) {
            log.error("Failed to append event via EventPublisher", e);
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
