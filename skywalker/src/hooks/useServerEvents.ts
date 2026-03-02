import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { fetchEventSource } from "@microsoft/fetch-event-source";
import { isEventMessage, isTodoPayload } from "@death-star/holocron";
import type { components } from "@death-star/holocron";
import { apiClient } from "../api/client";

type Todo = components["schemas"]["Todo"];

const SSE_ENDPOINT = "/api/v1/events";

/**
 * Custom hook to manage the Server-Sent Events (SSE) lifecycle.
 * Uses @microsoft/fetch-event-source to bypass native EventSource limitations,
 * allowing us to inject Authorization headers and seamlessly handle 401 token refreshes.
 */
export function useServerEvents() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const abortController = new AbortController();

    const connect = async () => {
      await fetchEventSource(SSE_ENDPOINT, {
        method: "GET",
        // --- DEPENDENCY INJECTION (IoC) ---
        // Injecting our Armed Fetch wrapper. This ensures SSE requests
        // carry the JWT in memory and participate in the Mutex refresh sequence on 401.
        fetch: apiClient.customFetch,
        signal: abortController.signal,

        onmessage(event) {
          try {
            const rawData = JSON.parse(event.data);

            if (!isEventMessage(rawData)) {
              console.warn(
                "[SSE] Received invalid event message envelope:",
                rawData,
              );
              return;
            }

            const message = rawData;
            if (!isTodoPayload(message.payload)) {
              console.warn(
                "[SSE] Event payload does not match Todo schema:",
                message.payload,
              );
              return;
            }

            const todo = message.payload;

            switch (message.eventType) {
              case "TODO_CREATED":
                queryClient.setQueryData<Todo[]>(["todos"], (old) => {
                  if (!old) return [todo];
                  if (old.some((t) => t.id === todo.id)) return old;
                  return [todo, ...old];
                });
                break;
              case "TODO_UPDATED":
                queryClient.setQueryData<Todo[]>(
                  ["todos"],
                  (old) => old?.map((t) => (t.id === todo.id ? todo : t)) ?? [],
                );
                break;
              case "TODO_DELETED":
                queryClient.setQueryData<Todo[]>(
                  ["todos"],
                  (old) => old?.filter((t) => t.id !== todo.id) ?? [],
                );
                break;
              default:
                console.warn(
                  `[SSE] Unhandled event type: ${message.eventType}`,
                );
            }
          } catch (error) {
            console.error("[SSE] Parse error", error);
          }
        },

        onclose() {
          console.warn("[SSE] Connection closed by server. Will retry...");
          // Return nothing to trigger auto-reconnect strategy of the library
        },

        onerror(err) {
          console.error("[SSE] Connection Error.", err);
          // If the error is fatal (e.g., Auth completely failed after refresh attempt),
          // throw the error to stop retrying. Otherwise, return nothing to retry.
          throw err;
        },
      });
    };

    connect();

    return () => {
      abortController.abort(); // Clean up connection on unmount
    };
  }, [queryClient]);
}
