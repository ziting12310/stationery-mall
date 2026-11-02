import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 配置
 * - 开发端口固定 5173（与后端 CORS 放行地址一致）
 * - 不使用 server.proxy：开发阶段通过 VITE_API_BASE_URL 指向 Apifox Mock / 本地后端
 */
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    open: true
  }
})
