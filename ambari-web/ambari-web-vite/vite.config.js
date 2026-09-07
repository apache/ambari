import { defineConfig } from "vite";
import legacy from "@vitejs/plugin-legacy";

export default defineConfig({
  root: "../public",
  plugins: [
    legacy({
      targets: ["ie >= 11"],
      additionalLegacyPolyfills: ["regenerator-runtime/runtime"],
    }),
  ],
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["jquery", "backbone", "handlebars"],
        },
      },
    },
  },
  server: {
    proxy: {
      // 将所有 /api/ 的请求代理到远程服务器 http://121.37.30.227:8080/api
      "/api/": {
        target: `http://121.37.30.227:8080/api`,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      // 将 WebSocket 请求代理到远程服务器
      "/api/stomp/v1/websocket": {
        target: `ws://121.37.30.227:8080/api`,
        changeOrigin: true,
        ws: true,
      },
      configure: (proxy) => {
        proxy.on("proxyReq", (proxyReq, req, res, options) => {
          console.log("请求:", req.method, req.url);
        });
        proxy.on("proxyRes", (proxyRes, req, res) => {
          console.log("响应状态:", proxyRes.statusCode);
        });
      },
    },
    watch: {
      //   usePolling: true, // 在某些系统上，可能需要启用 polling 来侦测文件变化
    },
  },
});
