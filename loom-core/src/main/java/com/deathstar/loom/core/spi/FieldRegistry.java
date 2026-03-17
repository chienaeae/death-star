package com.deathstar.loom.core.spi;

import com.deathstar.loom.core.domain.FieldDefinition;
import java.util.UUID;

/** SPI for resolving FieldDefinitions from ID. */
public interface FieldRegistry {

    /** Get the authoritative definition for a field. */
    FieldDefinition getField(UUID fieldId);
}
