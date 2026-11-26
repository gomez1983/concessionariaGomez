import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ mode }) => ({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: loadEnv(mode, '.', '').API_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
        // Como o Nginx em produção: a API monta os links HATEOAS com o endereço que o navegador vê
        // (http://localhost:5173/api/...), não com o do backend
        // xfwd envia X-Forwarded-Host/Port/Proto com o endereço original (localhost:5173)
        xfwd: true,
        headers: { 'X-Forwarded-Prefix': '/api' },
      },
    },
  },
}));
