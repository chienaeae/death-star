package com.deathstar.vader.loom.config;

import com.deathstar.vader.loom.engine.LoomEngine;
import com.deathstar.vader.loom.spi.EventStore;
import com.deathstar.vader.loom.spi.FieldRegistry;
import com.deathstar.vader.loom.spi.IdentityResolver;
import com.deathstar.vader.loom.spi.StateRepository;

import java.util.Collections;
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
}
