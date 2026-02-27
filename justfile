# ---------------------------------------------------------------------------
# Death Star - Justfile (Streamlined Command Center)
# ---------------------------------------------------------------------------
set shell := ["bash", "-c"]

# Default to list all available recipes
default:
    @just --list

# ============================================================================
# 1. Initialization
# ============================================================================

# Initialize environment, generate Gradle wrapper, and install all dependencies
init:
    gradle wrapper --gradle-version 8.12 --distribution-type bin
    npm install
    just gen-api

# ============================================================================
# 2. Infrastructure & Contracts (Local Dev)
# ============================================================================

# Spin up local infrastructure (Postgres, NATS)
up:
    docker compose up -d

# Tear down infrastructure and wipe volumes
down:
    docker compose down -v

# Generate frontend TypeScript types from OpenAPI spec
gen-api:
    npm --prefix holocron run generate

# ============================================================================
# 3. Decoupled Development Workflow
# ============================================================================

# [Step 1] Prepare dependencies (Docker + Types)
dev-deps: up gen-api

# [Step 2 - Terminal A] Start Vader Backend (Java 23 Virtual Threads)
dev-vader:
    ./gradlew :vader:bootRun --args='--spring.profiles.active=dev'

# [Step 2 - Terminal B] Start Skywalker Frontend (Vite)
dev-skywalker:
    npx turbo run dev

# [Optional] Integrated development command
dev: up gen-api
    @echo "Starting Vader & Skywalker (Press Ctrl+C to stop all)..."
    @trap 'kill 0' EXIT; \
    ./gradlew :vader:bootRun --args='--spring.profiles.active=dev' & \
    npx turbo run dev

# ============================================================================
# 4. Lifecycle Management
# ============================================================================

# Emergency stop: Shutdown local dev containers and kill processes
stop: down
    @echo "Cleaning up stray processes on 8080 and 3000..."
    @lsof -t -i:8080 | xargs kill -9 2>/dev/null || true
    @lsof -t -i:3000 | xargs kill -9 2>/dev/null || true
    @echo "All clear."

# ============================================================================
# 5. End-to-End (E2E) Test Orchestration
# ============================================================================

# Spin up the fully containerized E2E topology (Forces image rebuild)
e2e-up:
    @echo "Building and starting E2E environment (Postgres, NATS, Vader, Skywalker)..."
    docker compose -f docker-compose.e2e.yml up --build -d
    @echo "\n[Success] E2E Topology deployed."
    @echo "Frontend Gateway: http://localhost:8080"
    @echo "Backend API:      http://localhost:8080/api/"

# Run Hurl Smoke Test
e2e-smoke-test:
    @echo "Running Declarative API Smoke Tests via Hurl..."
    @if ! command -v hurl &> /dev/null; then \
        echo "Hurl not found. Installing via brew/curl..."; \
        curl --location --remote-name https://github.com/Orange-OpenSource/hurl/releases/download/4.3.0/hurl_4.3.0_amd64.deb && sudo dpkg -i hurl_4.3.0_amd64.deb || brew install hurl; \
    fi
    # --retry 15: enable retry and set maximum 15 attempts
    # --retry-interval 2000: 2 seconds between attempts (total wait 30 seconds)
    # --test: run in test mode and output a report
    hurl --test --retry 15 --retry-interval 2000 e2e/smoke.hurl

# Tail logs to debug container startup sequence or SSE connections
e2e-logs:
    docker compose -f docker-compose.e2e.yml logs -f

# Completely destroy the E2E environment and wipe ephemeral state
e2e-down:
    @echo "Tearing down E2E environment and destroying ephemeral volumes..."
    docker compose -f docker-compose.e2e.yml down -v
    @echo "[Success] E2E Sandbox destroyed."