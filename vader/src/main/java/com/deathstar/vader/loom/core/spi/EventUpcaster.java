package com.deathstar.vader.loom.core.spi;

import com.deathstar.vader.loom.core.domain.Event;

/**
 * SPI for Event Evolution. When old events are read from the log, they are run through a chain of
 * upcasters to migrate them to the latest schema before the engine processes them.
 */
public interface EventUpcaster {

    /** Determines if this upcaster can act on the given event. */
    boolean canUpcast(Event event);

    /** Transforms the obsolete event into a current event format. */
    Event upcast(Event event);
}
