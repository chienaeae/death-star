package com.deathstar.vader.event.spi;

import com.deathstar.vader.event.domain.EventMessage;
import com.deathstar.vader.event.domain.EventRoute;
import java.util.function.Consumer;

/**
 * Unified interface for subscribing to events, abstracting away the transport layer.
 */
public interface EventSubscriber {

    /**
     * Subscribe to an event topic. Automatically chooses JetStream (Durable) or NATS Core (Ephemeral)
     * based on the EventRoute's durable setting.
     *
     * @param route         The broad event route
     * @param subjectSuffix The specific subject to append
     * @param consumerGroup The queue group or durable name (for scaling across instances)
     * @param handler       The consumer function to handle deserialized EventMessages
     */
    void subscribe(EventRoute route, String subjectSuffix, String consumerGroup, Consumer<EventMessage> handler);
}
