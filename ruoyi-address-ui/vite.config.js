import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue2';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const runtimeMode = env.VITE_ADDRESS_RUNTIME_MODE || 'standalone';
  const gatewayBase = env.VITE_GATEWAY_BASE || 'http://127.0.0.1:8080';
  const standaloneAddressBase = env.VITE_ADDRESS_STANDALONE_BASE || env.VITE_API_BASE || 'http://127.0.0.1:9206';
  const standaloneWorkflowBase = env.VITE_WORKFLOW_STANDALONE_BASE || env.VITE_WORKFLOW_BASE || 'http://127.0.0.1:9205';
  const isMicroserviceMode = runtimeMode === 'microservice';
  const addressTarget = isMicroserviceMode ? gatewayBase : standaloneAddressBase;
  const workflowTarget = isMicroserviceMode ? gatewayBase : standaloneWorkflowBase;
  const workflowProxy = {
    target: workflowTarget,
    changeOrigin: true
  };
  if (!isMicroserviceMode) {
    workflowProxy.rewrite = path => path.replace(/^\/workflow/, '');
  }

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/address': {
          target: addressTarget,
          changeOrigin: true
        },
        '/workflow': workflowProxy
      }
    }
  };
});
