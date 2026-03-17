# Loom Core Engine

Loom is a technology-agnostic core engine designed to manage dynamic entities through immutable event streams. It operates on the principle that state is a derivative of time. Traditional systems store snapshots; Loom stores the facts (Events) and projects them into high-performance views.

## Design Goals

1. **Self-Contained & Lean**: Pure Java 23 library with zero hard dependencies on specific DB or Message Brokers (via SPI).
2. **Structural Efficiency (Property Bucketing)**: Optimized physical storage for JSONB from day one to combat MVCC write amplification.
3. **Concurrency-First**: Leveraging Project Loom (Virtual Threads) for high-throughput I/O and projection, synchronized via Striped Locking on the entity ID.
4. **Event-Driven DNA**: Native support for Undo, Audit Trails, and Schema Evolution (Upcasting).

## Core Concepts

* **Item**: The atomic entity.
* **FieldDefinition**: Soft schema dictating semantic meaning and physical storage routing.
* **Property Bucketing**: Attributes route to either `STATIC` (low-frequency) or `DYNAMIC` (high-frequency) buckets.
* **Optimistic Concurrency Control (OCC)**: Enforced via a `baseVersion` on every update.
* **LexRank**: Utility for infinite split-point sorting logic without full-list re-indexing.

## SPI (Service Provider Interface)

To integrate Loom into an application, you must implement its SPIs:

* `StateRepository`: Physical projection persistence (e.g., PostgreSQL JSONB).
* `EventStore`: The append-only fact log (e.g., NATS JetStream, Kafka).
* `IdentityResolver`: Multi-tenant isolation context (e.g., via Java 23 ScopedValues).
* `FieldRegistry`: The authoritative dictionary of `FieldDefinition` metadata.
* `EventUpcaster`: For schema evolution of historical events.
