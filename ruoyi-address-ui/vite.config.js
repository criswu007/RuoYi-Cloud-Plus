import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue2';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const runtimeMode = env.VITE_ADDRESS_RUNTIME_MODE || 'standalone';
  const nginxBase = env.VITE_NGINX_BASE || 'http://127.0.0.1:80';
  const standaloneAddressBase = env.VITE_ADDRESS_STANDALONE_BASE || env.VITE_API_BASE || 'http://127.0.0.1:9206';
  const standaloneWorkflowBase = env.VITE_WORKFLOW_STANDALONE_BASE || env.VITE_WORKFLOW_BASE || 'http://127.0.0.1:9205';
  const isMicroserviceMode = runtimeMode === 'microservice';
  const proxy = isMicroserviceMode
    ? {
        '/prod-api': {
          target: nginxBase,
          changeOrigin: true
        }
      }
    : {
        '/address': {
          target: standaloneAddressBase,
          changeOrigin: true
        },
        '/workflow': {
          target: standaloneWorkflowBase,
          changeOrigin: true,
          rewrite: path => path.replace(/^\/workflow/, '')
        }
      };

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy
    }
  };
});
