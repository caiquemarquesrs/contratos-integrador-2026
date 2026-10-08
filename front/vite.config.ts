import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Ao copiar: troque pelo código do seu módulo e pela porta do Contrato §13.3.
const CODIGO = 'contratos'
const PORTA = 3004

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  return {
    // Servido na mesma origem da casca, sob /modulos/{codigo}/ (Contrato §12.8)
    base: `/modulos/${CODIGO}/`,
    plugins: [react(), tailwindcss()],
    server: {
      port: PORTA,
      strictPort: true,
      // Em modo direto, as chamadas /api vão para o gateway, como em produção.
      proxy: { '/api': { target: env.VITE_GATEWAY || 'http://localhost:8080', changeOrigin: true } },
    },
  }
})
