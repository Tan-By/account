import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        // 关键：模拟 Nginx 行为，去掉 /api/ 前缀
        // Nginx 配置: proxy_pass http://127.0.0.1:8080/; (结尾有斜杠，会自动去掉 /api/)
        // 所以 Vite 代理也应该去掉 /api 前缀，直接转发路径
        // 例如：请求 /api/auth/login -> 转发到 http://localhost:8080/auth/login
        rewrite: (path) => {
          // path 参数已经是去掉 /api 后的路径，直接返回
          const targetPath = path.replace(/^\/api/, '') || '/';
          console.log('🔄 Vite 代理重写:', path, '->', targetPath);
          return targetPath;
        },
        configure: (proxy, options) => {
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('📤 代理请求:', req.method, req.url, '->', options.target + proxyReq.path);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('📥 代理响应:', req.method, req.url, '状态码:', proxyRes.statusCode);
          });
        }
      }
    }
  }
});


