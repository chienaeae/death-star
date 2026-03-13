import { isEventMessage } from '@death-star/holocron';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import { useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';
import { apiClient } from '../api/client';

const SSE_ENDPOINT = '/api/v1/events';

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
        method: 'GET',
        // --- DEPENDENCY INJECTION (IoC) ---
        // Injecting our Armed Fetch wrapper. This ensures SSE requests
        // carry the JWT in memory and participate in the Mutex refresh sequence on 401.
        fetch: apiClient.customFetch,
        signal: abortController.signal,

        onmessage(event: any) {
          try {
            const rawData = JSON.parse(event.data);

            if (!isEventMessage(rawData)) {
              console.warn('[SSE] Received invalid event message envelope:', rawData);
              return;
            }

            console.debug(`[SSE] Received event: ${rawData.eventType}`);
          } catch (error) {
            console.error('[SSE] Parse error', error);
          }
        },

        onclose() {
          console.warn('[SSE] Connection closed by server. Will retry...');
          // Return nothing to trigger auto-reconnect strategy of the library
        },

        onerror(err: any) {
          console.error('[SSE] Connection Error.', err);
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
