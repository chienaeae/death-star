package com.deathstar.vader.event.spi;

import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;

/** Unified interface for publishing events abstracting away the transport layer. */
public interface EventPublisher {

    /**
     * Publish an event. Automatically chooses JetStream (Durable) or NATS Core (Ephemeral) based on
     * the EventRoute's durable setting.
     */
    void publish(EventRoute route, String subjectSuffix, DomainEvent event);
}
