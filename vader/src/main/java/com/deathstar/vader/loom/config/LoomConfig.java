package com.deathstar.vader.loom.config;

import com.deathstar.loom.core.domain.BucketType;
import com.deathstar.loom.core.domain.FieldDefinition;
import com.deathstar.loom.core.engine.LoomEngine;
import com.deathstar.loom.core.spi.EventStore;
import com.deathstar.loom.core.spi.FieldRegistry;
import com.deathstar.loom.core.spi.IdentityResolver;
import com.deathstar.loom.core.spi.StateRepository;
import com.deathstar.vader.loom.domain.*;
import java.util.Collections;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring Boot Configuration for bootstrapping the technology-agnostic Loom Core engine. */
@Configuration
public class LoomConfig {

    @Bean
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public LoomEngine loomEngine(
            EventStore natsStore,
            StateRepository pgRepo,
            IdentityResolver identityResolver,
            FieldRegistry fieldRegistry) {
        // Initializes the engine with physical SPIs (JetStream, Postgres, ScopedValues)
        // and an empty/default upcaster chain.
        return new LoomEngine(
                natsStore, pgRepo, identityResolver, fieldRegistry, Collections.emptyList());
    }

    @Bean
    public FieldRegistry inMemoryFieldRegistry() {
        return new FieldRegistry() {
            @Override
            public FieldDefinition getField(UUID fieldId) {
                // Hardcoded registry for mock implementation.
                // In production, this would likely be cached from a meta-database.
                if (FieldConstants.STATUS_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId,
                            "Status",
                            FieldDefinition.FieldType.STATUS,
                            BucketType.DYNAMIC);
                }
                if (FieldConstants.LEXRANK_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId,
                            "LexRank",
                            FieldDefinition.FieldType.LEXRANK,
                            BucketType.DYNAMIC);
                }
                if (FieldConstants.TITLE_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId, "Title", FieldDefinition.FieldType.STRING, BucketType.STATIC);
                }
                if (FieldConstants.DESCRIPTION_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId,
                            "Description",
                            FieldDefinition.FieldType.STRING,
                            BucketType.STATIC);
                }
                if (FieldConstants.PRIORITY_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId,
                            "Priority",
                            FieldDefinition.FieldType.STRING,
                            BucketType.STATIC);
                }
                if (FieldConstants.DUE_DATE_ID.equals(fieldId)) {
                    return new FieldDefinition(
                            fieldId,
                            "DueDate",
                            FieldDefinition.FieldType.STRING,
                            BucketType.STATIC);
                }
                // Default fallback
                return new FieldDefinition(
                        fieldId, "Unknown", FieldDefinition.FieldType.STRING, BucketType.STATIC);
            }
        };
    }
}
