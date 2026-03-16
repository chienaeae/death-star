package com.deathstar.vader.config;

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
    private static final String AUDIT_STREAM_NAME = "AUDIT";
    private static final String AUDIT_STREAM_SUBJECTS = "audit.events.>";

    @PostConstruct
    public void init() {
        try {
            boolean streamExists = false;
            try {
                StreamInfo streamInfo = jsm.getStreamInfo(AUDIT_STREAM_NAME);
                if (streamInfo != null) {
                    streamExists = true;
                    log.info("JetStream '{}' already exists.", AUDIT_STREAM_NAME);
                }
            } catch (Exception e) {
                // Throws an exception if not found, we will create it
            }

            if (!streamExists) {
                StreamConfiguration streamConfig =
                        StreamConfiguration.builder()
                                .name(AUDIT_STREAM_NAME)
                                .subjects(AUDIT_STREAM_SUBJECTS)
                                .storageType(StorageType.File)
                                .build();

                jsm.addStream(streamConfig);
                log.info(
                        "Successfully provisioned JetStream '{}' for subjects '{}'",
                        AUDIT_STREAM_NAME,
                        AUDIT_STREAM_SUBJECTS);
            }
        } catch (Exception e) {
            log.error("Failed to initialize JetStream '{}'", AUDIT_STREAM_NAME, e);
        }
    }
}
