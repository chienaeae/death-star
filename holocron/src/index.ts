// ---------------------------------------------------------------------------
// Holocron Public API Surface
// ---------------------------------------------------------------------------

// 1. Static Types (Zero Runtime Overhead)
// Re-export all strictly generated types from the OpenAPI specification
export type * from "./generated/api";

// 2. Runtime Constants
// Standardized strings for Routing, SSE Events, etc.
export * from "./utils/constants";

// 3. Runtime Type Guards
// Pure functions to validate unknown network payloads at the boundary
export * from "./utils/type-guards";
