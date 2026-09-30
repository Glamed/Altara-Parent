import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  // Loaded with an empty prefix so these stay server-side: nothing here is exposed to the
  // browser bundle (only VITE_* variables are).
  const env = loadEnv(mode, process.cwd(), '');
  const apiUrl = env.ALTARA_API_URL || 'http://localhost:25001';

  return {
    plugins: [react()],
    optimizeDeps: {
      // Pre-bundle the icon set once instead of re-walking thousands of icon modules on every HMR change.
      include: ['@tabler/icons-react'],
    },
    server: {
      proxy: {
        // The Altara Web API authenticates with the shared backendKey. The dev server attaches
        // it here so the key never reaches the browser — in production, put the same thing
        // behind a real backend with staff login.
        '/api': {
          target: apiUrl,
          changeOrigin: true,
          headers: { Authorization: env.ALTARA_BACKEND_KEY ?? '' },
        },
      },
    },
  };
});
