package com.deathstar.vader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SseBroadcasterServiceTest {

    private Connection natsConnection;
    private ObjectMapper objectMapper;
    private SseBroadcasterService service;

    @BeforeEach
    void setUp() {
        natsConnection = mock(Connection.class);
        objectMapper = new ObjectMapper();

        // Mock Dispatcher creation for @PostConstruct init()
        Dispatcher dispatcher = mock(Dispatcher.class);
        when(natsConnection.createDispatcher(any())).thenReturn(dispatcher);

        service = new SseBroadcasterService(natsConnection, objectMapper);
        service.initSubscriber();
    }

    @Test
    void shouldRegisterNewSseEmitter() {
        var emitter = service.registerClient();

        assertThat(emitter).isNotNull();
        // The default timeout should be Long.MAX_VALUE based on our implementation
        assertThat(emitter.getTimeout()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void shouldPublishEventToNats() throws Exception {
        var event = EventMessage.created(new Todo("Test NATS"));
        var expectedJson = objectMapper.writeValueAsBytes(event);

        service.publishEvent(event);

        // 驗證是否正確透過 NATS Connection 發布到指定的 Subject
        verify(natsConnection).publish(eq("todos.events"), eq(expectedJson));
    }
}
