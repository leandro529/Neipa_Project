import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Servido por defecto en :5173 (ver CorsConfig del backend, que ya lo
// permite por defecto). El puerto del backend se toma de VITE_API_BASE_URL
// (ver src/api/client.js), no hace falta proxy para desarrollo.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
})
