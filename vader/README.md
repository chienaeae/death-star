# 🗡️ Vader - Backend Service

Vader is the core backend microservice of the Death Star architecture. It is a high-performance event-driven server built with **Java 23** and **Spring Boot**, heavily utilizing Virtual Threads (Project Loom) for high concurrency and efficient blocking I/O handling.

## 🚀 Tech Stack

- **Framework**: Spring Boot (WebMVC)
- **Runtime**: Java 23 (Virtual Threads enabled)
- **Database**: PostgreSQL (via Spring Data JPA)
- **Migrations**: Flyway
- **Messaging**: NATS Core Client
- **Real-time API**: Server-Sent Events (SSE) via `SseEmitter`
- **Security**: Spring Security with JWT & Caffeine caching
- **API Contracts**: OpenAPI Generator (Contract-First approach mapping to `holocron`)

## 🏗️ Architecture & Responsibilities

Vader acts as the central data processor and broadcaster:
1. **Handles Mutations**: Receives REST requests (e.g., POST/PUT/DELETE) from the frontend (`skywalker`).
2. **State Persistence**: Persists changes to the PostgreSQL database.
3. **Event Publishing**: Publishes state-change events (e.g., `TODO_CREATED`) to the NATS EventBus.
4. **Real-time Broadcast**: Subscribes to NATS events and broadcasts them down to connected frontend clients via zero-latency Server-Sent Events (SSE).

## 🛠️ Code Generation

Vader follows a **Contract-First** approach. The OpenAPI spec located in the `holocron` workspace (`../holocron/openapi.yaml`) is the single source of truth.

During the build process, the OpenAPI Generator plugin:
- Auto-generates server stubs and DTOs into `com.deathstar.vader.api` and `com.deathstar.vader.dto.generated`.
- Customizes generation to support `SseEmitter` and avoids generating default interfaces, keeping full header control in the Spring Controllers.

## 🔧 Local Development

Vader is designed to be run via the monorepo's `just` command runner.

Make sure you have all prerequisites installed (Java 23, Docker).

1. **Start Infrastructure** (PostgreSQL, NATS) from the monorepo root:
   ```bash
   just dev-deps
   ```

2. **Run Vader locally:**
   ```bash
   just dev-vader
   ```

## 🧪 Testing

Vader extensively uses **Testcontainers** for integration and E2E testing. When running tests, ephemeral Docker containers (like PostgreSQL) are automatically spun up and torn down, ensuring isolation and consistency without relying on local host infrastructure.
