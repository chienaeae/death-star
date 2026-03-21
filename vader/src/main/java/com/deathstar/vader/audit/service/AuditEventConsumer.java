package com.deathstar.vader.audit.service;

import com.deathstar.vader.event.domain.EventRoute;
import com.deathstar.vader.event.spi.EventSubscriber;
import io.opentelemetry.api.trace.Span;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuditEventConsumer {

    private final AuditService auditService;
    private final EventSubscriber eventSubscriber;

    // Batching mechanisms
    private final List<MapSqlParameterSource> batchBuffer = new ArrayList<>();
    private final int BATCH_SIZE = 1000;
    private final ReentrantLock lock = new ReentrantLock();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AuditEventConsumer(AuditService auditService, EventSubscriber eventSubscriber) {
        this.auditService = auditService;
        this.eventSubscriber = eventSubscriber;
    }

    @PostConstruct
    public void init() {
        try {
            eventSubscriber.subscribe(
                    EventRoute.AUDIT,
                    ">",
                    "audit-event-consumers",
                    eventMessage -> {
                        Span currentSpan = Span.current();
                        String traceId =
                                currentSpan.getSpanContext().isValid()
                                        ? currentSpan.getSpanContext().getTraceId()
                                        : "";
                        String spanId =
                                currentSpan.getSpanContext().isValid()
                                        ? currentSpan.getSpanContext().getSpanId()
                                        : "";

                        try {
                            com.deathstar.vader.event.domain.DomainEvent domainEvent =
                                    eventMessage.domainEvent();

                            @SuppressWarnings("unchecked")
                            java.util.Map<String, Object> payloadMap =
                                    (java.util.Map<String, Object>) domainEvent.payload();

                            MapSqlParameterSource params =
                                    new MapSqlParameterSource()
                                            .addValue(
                                                    "timestamp",
                                                    java.sql.Timestamp.from(
                                                            domainEvent.timestamp()))
                                            .addValue("eventId", domainEvent.eventId())
                                            .addValue("traceId", traceId)
                                            .addValue("spanId", spanId)
                                            .addValue("actorId", domainEvent.aggregateId())
                                            .addValue("action", payloadMap.get("action"))
                                            .addValue(
                                                    "resourceType", payloadMap.get("resourceType"))
                                            .addValue("resourceId", payloadMap.get("resourceId"))
                                            .addValue("status", payloadMap.get("status"))
                                            // Simplified for demo: In production we use actual IP
                                            // and context retrieval
                                            .addValue("clientIp", "127.0.0.1")
                                            .addValue("userAgent", "vader-internal")
                                            .addValue("metadata", payloadMap.get("metadata"));

                            addToBatch(params);
                            eventMessage.ack(); // Acknowledge successful processing
                        } catch (Exception e) {
                            log.error("Error processing audit event", e);
                            eventMessage.nack();
                        }
                    });
        } catch (Exception e) {
            log.error("Failed to subscribe to JetStream audit events", e);
        }

        // Flush buffer every 2 seconds if not reaching max batch size
        scheduler.scheduleAtFixedRate(this::flushBatch, 2, 2, TimeUnit.SECONDS);
    }

    private void addToBatch(MapSqlParameterSource params) {
        lock.lock();
        try {
            batchBuffer.add(params);
            if (batchBuffer.size() >= BATCH_SIZE) {
                flushBatch();
            }
        } finally {
            lock.unlock();
        }
    }

    private void flushBatch() {
        List<MapSqlParameterSource> toFlush = new ArrayList<>();
        lock.lock();
        try {
            if (batchBuffer.isEmpty()) return;
            toFlush.addAll(batchBuffer);
            batchBuffer.clear();
        } finally {
            lock.unlock();
        }

        try {
            auditService.sinkEventsToClickHouse(toFlush);
        } catch (Exception e) {
            log.error("Failed to route audit batch to AuditService", e);
        }
    }
}
