package com.deathstar.vader.loom;

import com.deathstar.loom.core.domain.Event;
import com.deathstar.loom.core.engine.LoomEngine;
import com.deathstar.vader.dto.EventMessage;
import com.deathstar.vader.loom.spi.PostgresStateRepository;
import com.deathstar.vader.service.SseBroadcasterService;
import com.deathstar.vader.tracing.NatsTracingPropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.PushSubscribeOptions;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoomProjectionWorker {

    private final Connection natsConnection;
    private final JetStream jetStream;
    private final NatsTracingPropagator tracingPropagator;
    private final ObjectMapper objectMapper;
    private final LoomEngine loomEngine;
    private final PostgresStateRepository stateRepository;
    private final SseBroadcasterService sseBroadcasterService;

    // Striped Locking map to ensure Virtual Threads modifying the same Item are serialized.
    private final ConcurrentHashMap<UUID, Lock> itemLocks = new ConcurrentHashMap<>();

    // Executor for Virtual Threads
    private final java.util.concurrent.Executor virtualThreadExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    public LoomProjectionWorker(
            Connection natsConnection,
            JetStream jetStream,
            NatsTracingPropagator tracingPropagator,
            ObjectMapper objectMapper,
            LoomEngine loomEngine,
            PostgresStateRepository stateRepository,
            SseBroadcasterService sseBroadcasterService) {
        this.natsConnection = natsConnection;
        this.jetStream = jetStream;
        this.tracingPropagator = tracingPropagator;
        this.objectMapper = objectMapper;
        this.loomEngine = loomEngine;
        this.stateRepository = stateRepository;
        this.sseBroadcasterService = sseBroadcasterService;
    }

    @PostConstruct
    public void init() {
        try {
            PushSubscribeOptions pso =
                    PushSubscribeOptions.builder().stream("LOOM")
                            .durable("loom-projection-worker")
                            .build();

            jetStream.subscribe(
                    "loom.>",
                    natsConnection.createDispatcher(),
                    msg -> {
                        // Hand off to Virtual Thread immediately for high-throughput
                        virtualThreadExecutor.execute(
                                () -> {
                                    tracingPropagator.processMessageWithTracing(
                                            msg,
                                            "project_loom_event",
                                            message -> projectEventPayload(message));
                                });
                    },
                    false, // autoAck = false
                    pso);
            log.info("LoomProjectionWorker initialized for subject loom.>");
        } catch (Exception e) {
            log.error("Failed to subscribe to JetStream LOOM events", e);
        }
    }

    private void projectEventPayload(io.nats.client.Message message) {
        Event event;
        try {
            event = objectMapper.readValue(message.getData(), Event.class);
        } catch (Exception e) {
            log.error(
                    "Failed to deserialize loom event payload: {}",
                    new String(message.getData(), StandardCharsets.UTF_8),
                    e);
            message.ack(); // Poison pill, ack it to avoid getting stuck
            return;
        }

        Lock lock = itemLocks.computeIfAbsent(event.itemId(), k -> new ReentrantLock());
        lock.lock();

        try {
            // 2. Distributed Safety (The Truth)
            // Atomic SQL Update using CAS (WHERE version = eventSeq - 1)
            boolean updated = loomEngine.projectEvent(event);

            if (!updated) {
                // Idempotency Check: if rows affected == 0, check the current DB version
                long currentDBVersion = stateRepository.getCurrentVersion(event.itemId());

                if (currentDBVersion >= event.baseVersion()) {
                    // Event is old or already applied. Idempotent ACK.
                    log.debug(
                            "Ignoring already projected event {} for item {} (event base={}, db={})",
                            event.eventId(),
                            event.itemId(),
                            event.baseVersion(),
                            currentDBVersion);
                    message.ack();
                } else {
                    // Event is from the future (Out of order). NACK to let NATS retry later.
                    log.warn(
                            "Out of order event {} for item {} (event base={}, db={}). NACKing for retry.",
                            event.eventId(),
                            event.itemId(),
                            event.baseVersion(),
                            currentDBVersion);
                    message.nakWithDelay(java.time.Duration.ofSeconds(1));
                }
                return;
            }

            // Successfully projected to DB.
            message.ack();
            log.debug(
                    "Successfully projected event {} for item {}", event.eventId(), event.itemId());

            // 4. Query (The View): Real-time Sync back to the application via SSE
            EventMessage sseMessage =
                    new EventMessage("ITEM_UPDATED", event, System.currentTimeMillis());
            sseBroadcasterService.publishEvent(sseMessage);

        } catch (Exception e) {
            log.error("Unexpected error projecting event {}", event.eventId(), e);
            message.nak(); // Retry on transient failures
        } finally {
            lock.unlock();
        }
    }
}
