import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: path.resolve(__dirname, '../src/main/resources/static'),
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/events': 'http://localhost:8080',
      '/admin': 'http://localhost:8080',
      '/users': 'http://localhost:8080',
      '/health': 'http://localhost:8080',
      '/actuator': 'http://localhost:8080',
    },
  },
});
