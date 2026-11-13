import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, ".", "");
  return {
    // Servido na mesma origem da casca, sob /modulos/contratos/ (Contrato §12.8 e modulos/contratos.json)
    base: "/modulos/contratos/",
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
      },
    },
    server: {
      port: 5173,
      // Em modo direto, toda chamada /api vai para o gateway, como em produção (Contrato §9.1):
      // /api/identity (login de teste) e /api/contratos saem do mesmo endereço.
      proxy: {
        "/api": {
          target: env.VITE_GATEWAY || "http://localhost:8080",
          changeOrigin: true,
        },
      },
    },
  };
});
