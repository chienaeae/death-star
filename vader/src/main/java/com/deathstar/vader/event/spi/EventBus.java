package com.deathstar.vader.event.spi;

import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;

/** Unified interface for publishing events abstracting away the transport layer. */
public interface EventBus {

    /**
     * Publish an event that must survive restarts and be replayable. Backed by NATS JetStream. Used
     * for Auditing and Event Sourcing.
     */
    void publishDurable(EventRoute route, String subjectSuffix, DomainEvent event);

    /**
     * Publish a low-latency, transient event. Backed by NATS Core. Used for Real-time SSE
     * broadcasting.
     */
    void publishEphemeral(EventRoute route, DomainEvent event);
}
