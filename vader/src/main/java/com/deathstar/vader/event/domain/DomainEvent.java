package com.deathstar.vader.event.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;

/** Standarized asynchronous message primitive for the unified event bus. */
public record DomainEvent(
        UUID eventId,
        @JsonProperty("eventType") String type,
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC") Instant timestamp,
        String aggregateId,
        Object payload) {}
