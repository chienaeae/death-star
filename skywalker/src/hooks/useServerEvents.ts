import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
// Import our defined Type Guards for runtime type safety
import { isEventMessage, isTodoPayload } from "@death-star/holocron";
import type { components } from "@death-star/holocron";

type Todo = components["schemas"]["Todo"];

const SSE_ENDPOINT = "/api/v1/events";

/**
 * Custom hook to manage the Server-Sent Events (SSE) lifecycle.
 * It listens to broadcasted events from the backend (via NATS) and
 * immutably updates the TanStack Query cache directly.
 */
export function useServerEvents() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const eventSource = new EventSource(SSE_ENDPOINT);

    eventSource.onmessage = (event) => {
      try {
        // 1. Parse network string into unknown JS object (Don't trust the network)
        const rawData = JSON.parse(event.data);

        // 2. Runtime type guard: verify EventMessage envelope
        if (!isEventMessage(rawData)) {
          console.warn(
            "[SSE] Received invalid event message envelope:",
            rawData,
          );
          return;
        }

        // After passing the Type Guard, TypeScript narrows rawData to EventMessage.
        // With openapi.yaml oneOf, rawData.payload is typed as Todo
        const message = rawData;

        // 3. Runtime type guard: verify payload matches Todo schema
        if (!isTodoPayload(message.payload)) {
          console.warn(
            "[SSE] Event payload does not match Todo schema:",
            message.payload,
          );
          return;
        }

        // At this point, todo is type-safe and runtime-validated
        const todo = message.payload;

        // Cache Patching Pattern
        switch (message.eventType) {
          case "TODO_CREATED": {
            queryClient.setQueryData<Todo[]>(["todos"], (oldData) => {
              if (!oldData) return [todo];
              if (oldData.some((t) => t.id === todo.id)) return oldData;
              return [todo, ...oldData];
            });
            break;
          }
          case "TODO_UPDATED": {
            queryClient.setQueryData<Todo[]>(["todos"], (oldData) => {
              if (!oldData) return [];
              return oldData.map((t) => (t.id === todo.id ? todo : t));
            });
            break;
          }
          case "TODO_DELETED": {
            queryClient.setQueryData<Todo[]>(["todos"], (oldData) => {
              if (!oldData) return [];
              return oldData.filter((t) => t.id !== todo.id);
            });
            break;
          }
          default:
            console.warn(`[SSE] Unhandled event type: ${message.eventType}`);
        }
      } catch (error) {
        console.error("[SSE] Failed to parse SSE message", error);
      }
    };

    eventSource.onerror = (error) => {
      console.error(
        "[SSE] Connection Error. Attempting to reconnect...",
        error,
      );
    };

    return () => {
      eventSource.close();
    };
  }, [queryClient]);
}
