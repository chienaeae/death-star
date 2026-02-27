// ---------------------------------------------------------------------------
// Millennium UI Shared Library Exports
// ---------------------------------------------------------------------------

// 1. Core Utilities
export { cn } from "./lib/utils";

// 2. UI Components (Strictly presentational)
export * from "./components/ui/button";
export * from "./components/ui/card";

// Note: The global CSS variables (src/styles/globals.css) are NOT exported here.
// Consuming applications must import the CSS file directly in their bundler
// entry point (e.g., in skywalker/src/main.tsx: import '@death-star/millennium/styles')
