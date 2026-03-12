// ---------------------------------------------------------------------------
// Death Star - Skywalker Secure API Client (BFF Pattern)
// ---------------------------------------------------------------------------
import type { components, paths } from '@death-star/holocron';

type TodosResponse = paths['/todos']['get']['responses']['200']['content']['application/json'];
type CreateTodoRequest = paths['/todos']['post']['requestBody']['content']['application/json'];
type TokenResponse = components['schemas']['TokenResponse'];

// --- FIX: Add IAM Request Type ---
type AuthRequest = paths['/auth/login']['post']['requestBody']['content']['application/json'];

const GATEWAY_PREFIX = '/api/v1';

// ============================================================================
// 1. The Closure Sanctuary (Absolute Memory Isolation)
// ============================================================================
let accessToken: string | null = null;
let refreshTokenPromise: Promise<string | null> | null = null; // Same-tab Mutex

// ============================================================================
// 2. Cross-Tab Synchronization (The Follower Listener)
// ============================================================================
const authChannel = new BroadcastChannel('auth_sync_channel');

authChannel.onmessage = (event) => {
  if (event.data.type === 'TOKEN_REFRESHED') {
    console.debug('[Follower] Received new token from Leader tab.');
    accessToken = event.data.payload;
  } else if (event.data.type === 'SESSION_TERMINATED') {
    console.warn('[Follower] Received termination signal. Clearing token.');
    accessToken = null;
    // Note: In Phase 4, we will dispatch a custom event here to force the UI to redirect to /login
  }
};

// ============================================================================
// 3. The Distributed Lock & Refresh Logic (Leader Election)
// ============================================================================
async function performRefresh(failedToken: string | null): Promise<string | null> {
  // Same-tab Mutex: If another request in THIS tab already started the refresh, await it.
  if (refreshTokenPromise) return refreshTokenPromise;

  // FIX: Replace 'new Promise(async ...)' with an internal async function.
  // Async functions inherently return a Promise, completely eliminating the need for
  // the manual resolve/reject constructor pattern which Biome strictly prohibits.
  const executeRefresh = async (): Promise<string | null> => {
    try {
      // Cross-tab Mutex: Web Locks API ensures only ONE tab becomes the Leader
      return await navigator.locks.request('auth-refresh-lock', async () => {
        // --- DOUBLE-CHECKED LOCKING PATTERN ---
        // Did a Leader tab already refresh the token while we were waiting outside the lock?
        if (accessToken !== failedToken) {
          console.debug(
            '[Leader] Token was updated by another tab while waiting. Skipping API call.',
          );
          return accessToken;
        }

        console.debug('[Leader] Lock acquired. Executing silent refresh...');
        const response = await fetch(`${GATEWAY_PREFIX}/auth/refresh`, {
          method: 'POST',
          // Cookie is sent automatically (HttpOnly Refresh Token)
        });

        if (!response.ok) {
          // If 401, the Refresh Token is dead (expired, or RTR replay attack triggered Kill Switch)
          accessToken = null;
          authChannel.postMessage({ type: 'SESSION_TERMINATED' });
          throw new Error('Session Expired');
        }

        const data: TokenResponse = await response.json();
        accessToken = data.accessToken;

        // Broadcast the new token to all sleeping Follower tabs
        authChannel.postMessage({
          type: 'TOKEN_REFRESHED',
          payload: accessToken,
        });

        return accessToken;
      });
    } finally {
      // Always release the same-tab mutex when the process finishes (success or fail)
      refreshTokenPromise = null;
    }
  };

  // Assign the Promise returned by the async function to our Mutex variable
  refreshTokenPromise = executeRefresh();

  return refreshTokenPromise;
}

// ============================================================================
// 4. The Armed Fetch Wrapper (Native Interceptor)
// ============================================================================
export const customFetch = async (
  input: RequestInfo | URL,
  init?: RequestInit,
): Promise<Response> => {
  // Pre-flight: Inject Access Token if available
  const currentToken = accessToken;
  const headers = new Headers(init?.headers);
  if (currentToken) {
    headers.set('Authorization', `Bearer ${currentToken}`);
  }

  let response = await fetch(input, { ...init, headers });

  // Post-flight: Handle 401 Unauthorized
  if (response.status === 401) {
    console.warn('[API] 401 Unauthorized. Initiating refresh sequence...');
    try {
      // Pass the token that failed to the refresh function for double-checking
      const newToken = await performRefresh(currentToken);

      if (newToken) {
        // Retry the original request with the new token
        headers.set('Authorization', `Bearer ${newToken}`);
        response = await fetch(input, { ...init, headers });
      }
    } catch (error) {
      console.error('[API] Refresh sequence failed. User must log in manually.');
      // Pass the 401 down to the caller (e.g., React Query) to handle UI changes
    }
  }

  return response;
};

// ============================================================================
// 5. Public API Surface
// ============================================================================
export const apiClient = {
  // We expose customFetch so 3rd-party libs (like SSE) can use our intercepted fetch
  customFetch,

  // Hydration function used during App Initialization (F5 Refresh)
  hydrateSession: async (): Promise<boolean> => {
    try {
      await performRefresh(null);
      return !!accessToken;
    } catch {
      return false;
    }
  },

  // --- IAM Methods ---

  register: async (payload: AuthRequest): Promise<TokenResponse> => {
    // CRITICAL: Use raw 'fetch' here, NOT 'customFetch'.
    // We don't want the interceptor to trigger a refresh loop on a 401 Bad Credentials.
    const response = await fetch(`${GATEWAY_PREFIX}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) throw new Error('Registration failed');

    const data: TokenResponse = await response.json();
    accessToken = data.accessToken;
    // Broadcast to other tabs so they wake up logged in
    authChannel.postMessage({ type: 'TOKEN_REFRESHED', payload: accessToken });
    return data;
  },

  login: async (payload: AuthRequest): Promise<TokenResponse> => {
    // CRITICAL: Use raw 'fetch' here.
    const response = await fetch(`${GATEWAY_PREFIX}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });

    if (!response.ok) throw new Error('Invalid credentials');

    const data: TokenResponse = await response.json();
    accessToken = data.accessToken;
    authChannel.postMessage({ type: 'TOKEN_REFRESHED', payload: accessToken });
    return data;
  },

  logout: async (): Promise<void> => {
    // We CAN use customFetch here because logout is an authenticated endpoint
    await customFetch(`${GATEWAY_PREFIX}/auth/logout`, { method: 'POST' });

    accessToken = null;
    authChannel.postMessage({ type: 'SESSION_TERMINATED' });
  },

  // --- Business Methods ---

  getTodos: async (): Promise<TodosResponse> => {
    const response = await customFetch(`${GATEWAY_PREFIX}/todos`);
    if (!response.ok) throw new Error('Failed to fetch todos');
    return response.json();
  },

  createTodo: async (payload: CreateTodoRequest): Promise<components['schemas']['Todo']> => {
    const response = await customFetch(`${GATEWAY_PREFIX}/todos`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!response.ok) throw new Error('Failed to create todo');
    return response.json();
  },
};
