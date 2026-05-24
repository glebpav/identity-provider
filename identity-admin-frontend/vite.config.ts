import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const identityApiUrl = env.VITE_IDENTITY_API_BASE_URL || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/identity-api': {
          target: identityApiUrl,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/identity-api/, '/api/v1')
        }
      }
    }
  };
});
