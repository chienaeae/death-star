
type EventMessage = any; // Quick fix for missing schema

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

