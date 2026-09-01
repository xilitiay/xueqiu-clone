import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// 开发时把 /api 代理到 Spring Boot 后端（:8080），避免跨域
export default defineConfig({
  plugins: [react()],
  // 某些沙箱环境重写了删除操作（safe-delete），清空 dist 会失败；
  // 关闭后由构建直接覆盖产物，不影响本地正常机器。
  build: { emptyOutDir: false },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // SockJS 握手与 WebSocket 升级走 /ws，需开启 ws 转发
      '/ws': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
