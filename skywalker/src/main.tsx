import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./index.css"; // Tailwind base imports

import App from "./App";

/**
 * Configure TanStack Query.
 * We disable refetchOnWindowFocus globally because our SSE hook
 * guarantees that our local cache is always strictly eventually consistent
 * with the server state.
 */
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      staleTime: Number.POSITIVE_INFINITY, // Data is fresh until mutated or updated via SSE
    },
  },
});

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>
  </React.StrictMode>,
);
