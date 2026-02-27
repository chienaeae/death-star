package com.deathstar.vader.service;

import com.deathstar.vader.dto.EventMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Bridges the NATS event bus with standard HTTP Server-Sent Events. */
@Service
public class SseBroadcasterService {

    private static final Logger log = LoggerFactory.getLogger(SseBroadcasterService.class);
    private static final String SUBJECT = "todos.events";

    private final Connection natsConnection;
    private final ObjectMapper objectMapper;

    // CopyOnWriteArrayList is chosen based on the 80/20 rule:
    // We iterate (read) to broadcast vastly more often than clients connect/disconnect (write).
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseBroadcasterService(Connection natsConnection, ObjectMapper objectMapper) {
        this.natsConnection = natsConnection;
        this.objectMapper = objectMapper;
    }

    /**
     * Subscribe to NATS core topics upon service startup. When a message arrives from NATS,
     * broadcast it to all connected SSE clients.
     */
    @PostConstruct
    public void initSubscriber() {
        Dispatcher dispatcher =
                natsConnection.createDispatcher(
                        (msg) -> {
                            String jsonPayload = new String(msg.getData());
                            broadcastToClients(jsonPayload);
                        });
        dispatcher.subscribe(SUBJECT);
        log.info("NATS subscriber initialized on subject: {}", SUBJECT);
    }

    /** Register a new client connection for SSE. */
    public SseEmitter registerClient() {
        // Use a virtually infinite timeout for long-lived connections
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError((e) -> emitters.remove(emitter));

        emitters.add(emitter);
        return emitter;
    }

    /** Publish a domain event to the NATS cluster. */
    public void publishEvent(EventMessage event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            natsConnection.publish(SUBJECT, payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event message", e);
        }
    }

    /**
     * Iterates through all connected clients and pushes the JSON string. With Virtual Threads, the
     * blocking I/O inside emitter.send() is virtually free.
     */
    private void broadcastToClients(String jsonPayload) {
        for (SseEmitter emitter : emitters) {
            try {
                // The SseEmitter.event().data() payload structure natively matches
                // the `text/event-stream` contract required by browsers.
                emitter.send(SseEmitter.event().data(jsonPayload));
            } catch (IOException e) {
                // Client disconnected ungracefully; remove them.
                emitters.remove(emitter);
            }
        }
    }
}
