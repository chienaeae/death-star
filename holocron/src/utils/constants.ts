// ---------------------------------------------------------------------------
// API & Event Constants
// ---------------------------------------------------------------------------
// Eliminates "Magic Strings" across the entire frontend application.
// Whenever skywalker needs to switch on an event type, it must use these constants.

export const EVENT_TYPES = {
  TODO_CREATED: "TODO_CREATED",
  TODO_UPDATED: "TODO_UPDATED",
  TODO_DELETED: "TODO_DELETED",
} as const;

// Derived type for strict type-checking
export type EventType = (typeof EVENT_TYPES)[keyof typeof EVENT_TYPES];
