package com.deathstar.vader.core.config;

import com.deathstar.vader.event.domain.EventRoute;
import io.nats.client.JetStreamManagement;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import io.nats.client.api.StreamInfo;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JetStreamInitializer {

    private final JetStreamManagement jsm;

    @PostConstruct
    public void init() {
        try {
            boolean streamExists = false;
            try {
                StreamInfo streamInfo = jsm.getStreamInfo(EventRoute.AUDIT.stream());
                if (streamInfo != null) {
                    streamExists = true;
                    log.info("JetStream '{}' already exists.", EventRoute.AUDIT.stream());
                }
            } catch (Exception e) {
                // Throws an exception if not found, we will create it
            }

            if (!streamExists) {
                StreamConfiguration streamConfig =
                        StreamConfiguration.builder()
                                .name(EventRoute.AUDIT.stream())
                                .subjects(EventRoute.AUDIT.wildcardSubject())
                                .storageType(StorageType.File)
                                .build();

                jsm.addStream(streamConfig);
                log.info(
                        "Successfully provisioned JetStream '{}' for subjects '{}'",
                        EventRoute.AUDIT.stream(),
                        EventRoute.AUDIT.wildcardSubject());
            }

            // Provision LOOM Stream
            boolean loomStreamExists = false;
            try {
                StreamInfo loomStreamInfo = jsm.getStreamInfo(EventRoute.LOOM.stream());
                if (loomStreamInfo != null) {
                    loomStreamExists = true;
                    log.info("JetStream '{}' already exists.", EventRoute.LOOM.stream());
                }
            } catch (Exception e) {
                // Throws an exception if not found, we will create it
            }

            if (!loomStreamExists) {
                StreamConfiguration streamConfig =
                        StreamConfiguration.builder()
                                .name(EventRoute.LOOM.stream())
                                .subjects(EventRoute.LOOM.wildcardSubject())
                                .storageType(StorageType.File)
                                .build();

                jsm.addStream(streamConfig);
                log.info(
                        "Successfully provisioned JetStream '{}' for subjects '{}'",
                        EventRoute.LOOM.stream(),
                        EventRoute.LOOM.wildcardSubject());
            }
        } catch (Exception e) {
            log.error("Failed to initialize JetStreams", e);
        }
    }
}
