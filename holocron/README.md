# Holocron

**Single Source of Truth: API Contract and Generated Types**

Holocron serves as the foundational shared library and the Single Source of Truth for the API contract across the Death Star project's microservices architecture. It ensures that all services - whether frontend or backend - adhere to the exact same data contracts, seamlessly preventing schema drift.

## Overview

At its core, Holocron heavily utilizes a spec-first approach to manage API definitions. By maintaining the central OpenAPI specification (`openapi.yaml`) here, we can automatically generate and share strictly typed TypeScript interfaces and runtime utilities across the entire workspace.

### Key Responsibilities

1. **API Specifications**: Houses the official, canonical OpenAPI specification for the Death Star API.
2. **Type Generation**: Automatically generates static TypeScript definitions from the OpenAPI spec, ensuring type safety with zero runtime overhead.
3. **Runtime Utilities**: Provides standardized runtime constants (e.g., routing paths, SSE event names) and pure type-guard functions for network payload boundary validation.

This unified approach guarantees architectural consistency, enhances developer workflow, and streamlines cross-team development across the Death Star project.
