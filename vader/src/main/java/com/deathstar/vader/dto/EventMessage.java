package com.deathstar.vader.dto;

/** Standardized event contract, aligning strictly with holocron/openapi.yaml */
public record EventMessage(String eventType, Object payload, long timestamp) {

    public static EventMessage created(Object payload) {
        return new EventMessage("TODO_CREATED", payload, System.currentTimeMillis());
    }

    public static EventMessage updated(Object payload) {
        return new EventMessage("TODO_UPDATED", payload, System.currentTimeMillis());
    }

    public static EventMessage deleted(Object payload) {
        return new EventMessage("TODO_DELETED", payload, System.currentTimeMillis());
    }
}
