package com.deathstar.vader.loom.core.domain;

import java.util.UUID;

/**
 * Soft Schema metadata mapping dictating validation, semantic meaning (e.g., LexRank), and physical
 * storage routing.
 */
public record FieldDefinition(
        UUID fieldId, String name, FieldType type, BucketType targetBucket // Optional override
        ) {
    public enum FieldType {
        LEXRANK,
        STATUS,
        LONG_TEXT,
        MARKDOWN,
        STRING,
        INTEGER,
        BOOLEAN
    }

    /**
     * Determine the effective bucket for this field. Use targetBucket if explicitly overridden,
     * otherwise infer from the FieldType.
     */
    public BucketType effectiveBucket() {
        if (targetBucket != null) {
            return targetBucket;
        }

        // Type-Heuristic Inferred Routing
        return switch (type) {
            case LEXRANK, STATUS, BOOLEAN -> BucketType.DYNAMIC;
            case LONG_TEXT, MARKDOWN, STRING, INTEGER -> BucketType.STATIC;
        };
    }
}
