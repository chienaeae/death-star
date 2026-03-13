package com.deathstar.vader.dto;

/** Standardized event contract, aligning strictly with holocron/openapi.yaml */
public record EventMessage(String eventType, Object payload, long timestamp) {}
