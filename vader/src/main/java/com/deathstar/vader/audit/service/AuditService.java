package com.deathstar.vader.audit.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AuditService {

    private final NamedParameterJdbcTemplate auditJdbcTemplate;

    public AuditService(
            @Qualifier("auditJdbcTemplate") NamedParameterJdbcTemplate auditJdbcTemplate) {
        this.auditJdbcTemplate = auditJdbcTemplate;
    }

    /**
     * Bulk inserts a batch of audit events directly into the underlying datastore (ClickHouse).
     *
     * @param batch The collection of event parameters to sink
     */
    public void sinkEventsToClickHouse(List<MapSqlParameterSource> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }

        try {
            String sql =
                    "INSERT INTO default.audit_events (Timestamp, EventId, TraceId, SpanId, ActorId, Action, ResourceType, ResourceId, Status, ClientIp, UserAgent, Metadata) "
                            + "VALUES (:timestamp, :eventId, :traceId, :spanId, :actorId, :action, :resourceType, :resourceId, :status, :clientIp, :userAgent, :metadata)";

            auditJdbcTemplate.batchUpdate(sql, batch.toArray(new MapSqlParameterSource[0]));
            log.debug("Flushed {} audit events to ClickHouse", batch.size());
        } catch (Exception e) {
            log.error("Failed to flush audit batch to ClickHouse", e);
        }
    }
}
