package com.deathstar.vader.sse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.deathstar.vader.event.spi.EventSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SseBroadcasterServiceTest {

    private EventSubscriber eventSubscriber;
    private ObjectMapper objectMapper;
    private SseBroadcasterService service;

    @BeforeEach
    void setUp() {
        eventSubscriber = mock(EventSubscriber.class);
        objectMapper = new ObjectMapper();

        service = new SseBroadcasterService(objectMapper, eventSubscriber);
        service.initSubscriber();
    }

    @Test
    void shouldRegisterNewSseEmitter() {
        var emitter = service.registerClient();

        assertThat(emitter).isNotNull();
        // The default timeout should be Long.MAX_VALUE based on our implementation
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }
}
