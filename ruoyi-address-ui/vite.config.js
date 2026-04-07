import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue2';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const target = env.VITE_API_BASE || 'http://localhost:9206';
  const workflowTarget = env.VITE_WORKFLOW_BASE || 'http://localhost:9205';

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/address': {
          target,
          changeOrigin: true
        },
        '/workflow': {
          target: workflowTarget,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/workflow/, '')
        }
      }
    }
  };
});
