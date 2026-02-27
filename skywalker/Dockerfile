# ---------------------------------------------------------------------------
# Stage 1: Monorepo Builder
# ---------------------------------------------------------------------------
FROM node:22-alpine AS builder
WORKDIR /app

# Enable corepack for modern package manager support (if needed)
RUN corepack enable

# 1. Copy Monorepo workspace configurations
COPY package*.json turbo.json ./
COPY holocron/package.json holocron/
COPY millennium/package.json millennium/
COPY skywalker/package.json skywalker/

# 2. Install ALL dependencies at the root level to resolve workspace symlinks
RUN npm ci

# 3. Copy source code for all workspaces
COPY holocron/ holocron/
COPY millennium/ millennium/
COPY skywalker/ skywalker/

# 4. Execute Turborepo build target for skywalker specifically
# Turbo will automatically build dependencies (holocron, millennium) first
RUN npx turbo run build --filter=@death-star/skywalker

# ---------------------------------------------------------------------------
# Stage 2: Nginx Static Server
# ---------------------------------------------------------------------------
FROM nginx:alpine

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

# Copy custom Nginx configuration optimized for SPA and SSE
COPY skywalker/nginx.conf /etc/nginx/conf.d/default.conf

# Copy the built artifacts from the Skywalker dist directory
COPY --from=builder /app/skywalker/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]