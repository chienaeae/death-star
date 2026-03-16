# @death-star/skywalker

Skywalker is the frontend application for the Death Star system. Currently serving as the **IAM Access Portal** and dashboard, it provides a secure interface for authenticating personnel.

## Tech Stack

This project is built with a modern frontend stack:
- **Framework**: React 18
- **Build Tool**: Vite
- **Language**: TypeScript
- **Styling**: Tailwind CSS
- **Data Fetching & State**: TanStack React Query (`@tanstack/react-query`)
- **Routing**: React Router v7 (`react-router`)
- **Real-time Updates**: Server-Sent Events (SSE) via `@microsoft/fetch-event-source`
- **Icons**: Lucide React

## Architecture & Features

### Routing & Navigation
The application uses **React Router v7** for standard URL-based navigation and route protection:
- **`/auth`**: Displays the authentication portal. Redirects to `/` if the user is already authenticated.
- **`/` (Root)**: Displays the main dashboard. Redirects to `/auth` if the user lacks a valid session.

### Authentication State Machine
The application manages authentication through a robust, first-principles state machine with three explicit states:
- `PENDING`: Initializing auth state.
- `AUTHENTICATED`: User is logged in.
- `UNAUTHENTICATED`: User needs to log in.

### Session Management
- **Silent Authentication**: Implements a hydration pattern. On application load (e.g., F5 refresh), it implicitly exchanges HttpOnly cookies for memory-based access tokens without user intervention.
- **Cross-Tab Synchronization**: Utilizes the `BroadcastChannel` API (`auth_sync_channel`) to ensure that login and logout events are synchronized across all open browser tabs simultaneously.

## Monorepo Integration

Skywalker is part of the broader Death Star Turborepo monorepo. It relies on local packages:
- `@death-star/holocron`: Shared OpenAPI schemas and types.
- `@death-star/millennium`: Shared utilities or configurations.

## Development

To spin up the skywalker frontend development server, you should use the `just` command runner from the root of the monorepo:

```bash
# From the root of the monorepo
just dev-skywalker
```

You can also run the local build and validation commands directly within the `skywalker` directory:

```bash
cd skywalker
npm run format
npm run lint
npm run build
```

> **Note**: For full local development, you must also ensure the backend services are running (e.g., using `just dev-vader` in a separate terminal or `just dev` to run both).

Skywalker's development server runs on port `5173` and proxies API requests (`/api/v1`) to the local backend on port `8080`.

## Production Deployment

The application is deployed using a multi-stage Docker build:
1. **Builder Stage**: Uses Node.js and Turborepo to build the application and its local dependencies.
2. **Server Stage**: Uses Nginx to serve the static built SPA.
   - SPA fallback routing is configured (`try_files $uri $uri/ /index.html`).
   - Acts as an API reverse proxy, forwarding `/api` traffic to the backend service `vader` on port `8080`.
   - Nginx is explicitly configured to support persistent Server-Sent Events (SSE) connections by disabling proxy buffering and configuring long timeouts.


