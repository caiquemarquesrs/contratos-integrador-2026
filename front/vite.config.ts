import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 5173,
    proxy: {
      "/api/identity": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/api": {
        target: "http://localhost:8084",
        changeOrigin: true,
      },
    },
  },
});


