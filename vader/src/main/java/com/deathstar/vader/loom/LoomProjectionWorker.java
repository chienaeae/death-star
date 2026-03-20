package com.deathstar.vader.loom;

import com.deathstar.vader.event.domain.DomainEvent;
import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventPublisher;
import com.deathstar.vader.loom.domain.Event;
import com.deathstar.vader.loom.engine.LoomEngine;
import com.deathstar.vader.loom.infrastructure.PostgresStateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.deathstar.vader.event.domain.EventMessage;
import com.deathstar.vader.event.spi.EventSubscriber;
import jakarta.annotation.PostConstruct;

import java.time.Instant;
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

    private final ObjectMapper objectMapper;
    private final LoomEngine loomEngine;
    private final PostgresStateRepository stateRepository;
    private final EventPublisher eventPublisher;
    private final EventSubscriber eventSubscriber;

    // Striped Locking map to ensure Virtual Threads modifying the same Item are serialized.
    private final ConcurrentHashMap<UUID, Lock> itemLocks = new ConcurrentHashMap<>();

    // Executor for Virtual Threads
    private final java.util.concurrent.Executor virtualThreadExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    public LoomProjectionWorker(
            ObjectMapper objectMapper,
            LoomEngine loomEngine,
            PostgresStateRepository stateRepository,
            EventPublisher eventPublisher,
            EventSubscriber eventSubscriber) {
        this.objectMapper = objectMapper;
        this.loomEngine = loomEngine;
        this.stateRepository = stateRepository;
        this.eventPublisher = eventPublisher;
        this.eventSubscriber = eventSubscriber;
    }

    @PostConstruct
    public void init() {
        try {
            eventSubscriber.subscribe(EventRoute.LOOM, ">", "loom-projection-worker", eventMessage -> {
                // Hand off to Virtual Thread immediately for high-throughput
                virtualThreadExecutor.execute(() -> projectEventPayload(eventMessage));
            });
            log.info("LoomProjectionWorker initialized using EventSubscriber");
        } catch (Exception e) {
            log.error("Failed to subscribe to Loom events via EventSubscriber", e);
        }
    }

    private void projectEventPayload(EventMessage eventMessage) {
        Event event;
        try {
            DomainEvent domainEvent = eventMessage.domainEvent();
            event = objectMapper.convertValue(domainEvent.payload(), Event.class);
        } catch (Exception e) {
            log.error("Failed to deserialize loom event payload", e);
            eventMessage.ack(); // Poison pill, ack it to avoid getting stuck
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
                    eventMessage.ack();
                } else {
                    // Event is from the future (Out of order). NACK to let NATS retry later.
                    log.warn(
                            "Out of order event {} for item {} (event base={}, db={}). NACKing for retry.",
                            event.eventId(),
                            event.itemId(),
                            event.baseVersion(),
                            currentDBVersion);
                    eventMessage.nackWithDelay(java.time.Duration.ofSeconds(1));
                }
                return;
            }

            // Successfully projected to DB.
            eventMessage.ack();
            log.debug(
                    "Successfully projected event {} for item {}", event.eventId(), event.itemId());

            // 4. Query (The View): Real-time Sync back to the application via SSE
            DomainEvent sseMessage =
                    new DomainEvent(
                            event.eventId(),
                            "ITEM_UPDATED",
                            Instant.now(),
                            event.itemId().toString(),
                            event);
            eventPublisher.publish(EventRoute.SYSTEM, "", sseMessage);
            log.debug("Propagated loom projection event via SSE: ITEM_UPDATED");
        } catch (Exception e) {
            log.error("Unexpected error projecting event {}", event.eventId(), e);
            eventMessage.nack(); // Retry on transient failures
        } finally {
            lock.unlock();
        }
    }
}
