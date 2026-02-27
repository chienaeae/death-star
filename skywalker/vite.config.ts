import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      // Route all /api requests to the Vader backend
      // This seamlessly resolves CORS issues during local development
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
      // Proxy configuration for Server-Sent Events (SSE)
      // SSE requires long-lived connections; Vite's http-proxy handles this natively
      "/api/v1/events": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
