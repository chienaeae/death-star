package com.deathstar.vader.loom.domain;

import java.util.UUID;

/** Constants defining standard structural semantic fields mapped to Loom. */
public final class FieldConstants {
    private FieldConstants() {}

    public static final UUID STATUS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID LEXRANK_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // Explicit standard BoardTask fields (these map to attr_static by default)
    public static final UUID TITLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static final UUID DESCRIPTION_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID PRIORITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
    public static final UUID DUE_DATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000005");
    // Other system-defined fields...
}
