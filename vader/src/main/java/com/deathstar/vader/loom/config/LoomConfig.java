package com.deathstar.vader.loom.config;

import com.deathstar.vader.loom.core.engine.LoomEngine;
import com.deathstar.vader.loom.core.spi.EventStore;
import com.deathstar.vader.loom.core.spi.FieldRegistry;
import com.deathstar.vader.loom.core.spi.IdentityResolver;
import com.deathstar.vader.loom.core.spi.StateRepository;
import com.deathstar.vader.loom.domain.*;
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
