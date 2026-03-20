package com.deathstar.vader.loom.spi;

import com.deathstar.vader.loom.domain.Event;
import java.util.List;
import java.util.UUID;

/** SPI for persisting the immutable sequence of facts (e.g., NATS JetStream or Kafka). */
public interface EventStore {

    /** Appends a new fact to the Truth Log. */
    void append(Event event);

    /** Reads the stream of events for a given item, typically to rebuild state. */
    List<Event> readStream(UUID itemId);
}
