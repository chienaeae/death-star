import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      "/api/v1": {
        target: "http://localhost:8080",
        changeOrigin: true,
        secure: false,
        // [Crucial Magic] This line is equivalent to K8s nginx.ingress.kubernetes.io/rewrite-target: /$2
        rewrite: (path) => path.replace(/^\/api\/v1/, ""),
      },
    },
  },
});
