import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    open: false,
    proxy: {
      '/api/carts': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err, req, res) => {
            if (!res.headersSent) {
              res.writeHead(503, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({
                status: 503,
                error: 'Service Unavailable',
                message: 'order-service (port 8084) is offline. Falling back to local storage.'
              }));
            }
          });
        },
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('error', (err, req, res) => {
            if (!res.headersSent) {
              res.writeHead(503, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({
                status: 503,
                error: 'Service Unavailable',
                message: 'api-gateway (port 8080) is offline. Falling back to local storage.'
              }));
            }
          });
        },
      },
    },
  },
});
