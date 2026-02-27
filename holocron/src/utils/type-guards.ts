import type { components } from "../generated/api";

type EventMessage = components["schemas"]["EventMessage"];
type Todo = components["schemas"]["Todo"];

/**
 * Runtime Type Guard for EventMessage.
 * Safely verifies if an unknown payload from SSE/NATS conforms to our contract.
 */
export function isEventMessage(data: unknown): data is EventMessage {
  if (typeof data !== "object" || data === null) return false;

  const msg = data as Record<string, unknown>;

  return (
    typeof msg.eventType === "string" &&
    "payload" in msg &&
    typeof msg.timestamp === "number"
  );
}

/**
 * Runtime Type Guard for Todo Payload.
 * Ensures the nested payload object matches the Todo schema before we inject it into the UI cache.
 */
export function isTodoPayload(data: unknown): data is Todo {
  if (typeof data !== "object" || data === null) return false;

  const todo = data as Record<string, unknown>;

  return (
    typeof todo.id === "string" &&
    typeof todo.title === "string" &&
    typeof todo.completed === "boolean" &&
    typeof todo.createdAt === "string"
  );
}
