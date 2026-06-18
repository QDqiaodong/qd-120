import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

// ========================================================
// Vite 开发服务器配置
// 严格遵守端口绑定规则：
// - 绑定 127.0.0.1，禁止使用 0.0.0.0 / localhost
// - strictPort: true，端口被占用时报错，不自动换端口
// - 前后端端口从 .env 读取（本地开发时固定值）
// ========================================================

// 开发端口（与 .env 中 FRONTEND_PORT 保持一致：18120）
const DEV_PORT = 18120
// 后端端口（与 .env 中 BACKEND_PORT 保持一致：19120）
const BACKEND_PORT = 19120

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src')
    }
  },
  // 开发服务器
  server: {
    host: '127.0.0.1',
    port: DEV_PORT,
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${BACKEND_PORT}`,
        changeOrigin: true
      }
    }
  },
  // 预览服务器（构建后本地预览）
  preview: {
    host: '127.0.0.1',
    port: DEV_PORT,
    strictPort: true
  },
  // 生产构建
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          'vue-vendor': ['vue', 'vue-router', 'pinia']
        }
      }
    }
  }
})
