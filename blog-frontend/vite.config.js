import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  server: {
    proxy: {
      // Article service (port 8082)
      "/api/articles": {
        target: "http://localhost:8082",
        changeOrigin: true,
      },
      // User service (port 8083) - 用户注册和信息查询
      "/api/users": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
      "/api/notifications": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
      // Static uploads served by user service
      "/uploads": {
        target: "http://localhost:8083",
        changeOrigin: true,
      },
      // Auth service (port 8081) - JWT认证、登录、登出
      "/api/auth": {
        target: "http://localhost:8081",
        changeOrigin: true,
      },
    },
  },
});
