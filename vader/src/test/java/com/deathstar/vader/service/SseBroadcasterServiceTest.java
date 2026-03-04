package com.deathstar.vader.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deathstar.vader.domain.Todo;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.tracing.NatsTracingPropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SseBroadcasterServiceTest {

    private Connection natsConnection;
    private ObjectMapper objectMapper;
    private NatsTracingPropagator natsTracingPropagator;
    private SseBroadcasterService service;

    @BeforeEach
    void setUp() {
        natsConnection = mock(Connection.class);
        objectMapper = new ObjectMapper();
        natsTracingPropagator = mock(NatsTracingPropagator.class);

        // Mock Dispatcher creation for @PostConstruct init()
        Dispatcher dispatcher = mock(Dispatcher.class);
        when(natsConnection.createDispatcher(any())).thenReturn(dispatcher);

        when(natsTracingPropagator.injectContext()).thenReturn(new io.nats.client.impl.Headers());

        service = new SseBroadcasterService(natsConnection, objectMapper, natsTracingPropagator);
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

        // Verify if properly published to the specified Subject via NATS Connection
        verify(natsConnection).publish(eq("todos.events"), any(io.nats.client.impl.Headers.class), eq(expectedJson));
    }
}
