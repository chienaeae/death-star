package com.deathstar.vader.loom.spi;

import com.deathstar.vader.loom.domain.FieldDefinition;
import java.util.UUID;

/** SPI for resolving FieldDefinitions from ID. */
public interface FieldRegistry {

    /** Get the authoritative definition for a field. */
    FieldDefinition getField(UUID fieldId);
}
