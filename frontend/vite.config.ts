import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // The SPA and the API share one origin in development, so session and CSRF cookies just work.
      '/api': 'http://localhost:8080',
    },
  },
})
