package com.deathstar.vader.config;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Manages the connection to the NATS messaging nervous system. */
@Configuration
public class NatsConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);

    @Value("${nats.url:nats://localhost:4222}")
    private String natsUrl;

    @Bean
    public Connection natsConnection() throws IOException, InterruptedException {
        Options options =
                new Options.Builder()
                        .server(natsUrl)
                        .connectionName("vader-backend")
                        // Graceful handling of network partitions
                        .maxReconnects(-1)
                        .build();

        Connection connection = Nats.connect(options);
        log.info("Successfully connected to NATS at {}", natsUrl);
        return connection;
    }
}
