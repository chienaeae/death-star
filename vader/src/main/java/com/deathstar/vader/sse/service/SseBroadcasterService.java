package com.deathstar.vader.sse.service;

import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ObjectMapper objectMapper;
    private final EventSubscriber eventSubscriber;

    // CopyOnWriteArrayList is chosen based on the 80/20 rule:
    // We iterate (read) to broadcast vastly more often than clients connect/disconnect (write).
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseBroadcasterService(
            ObjectMapper objectMapper,
            EventSubscriber eventSubscriber) {
        this.objectMapper = objectMapper;
        this.eventSubscriber = eventSubscriber;
    }

    /**
     * Subscribe to NATS core topics upon service startup. When a message arrives from NATS,
     * broadcast it to all connected SSE clients.
     */
    @PostConstruct
    public void initSubscriber() {
        try {
            eventSubscriber.subscribe(EventRoute.SYSTEM, "", "sse-broadcaster", eventMessage -> {
                try {
                    String jsonPayload = objectMapper.writeValueAsString(eventMessage.domainEvent());
                    broadcastToClients(jsonPayload);
                } catch (Exception e) {
                    log.error("Failed to serialize DomainEvent for SSE broadcast", e);
                }
            });
            log.info("NATS SSE subscriber initialized via EventSubscriber");
        } catch (Exception e) {
            log.error("Failed to initialize NATS SSE subscriber", e);
        }
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
