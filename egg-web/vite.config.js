import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// Vite 配置：替代原 vue.config.js
// - @ 别名指向 src/
// - 开发服务器 8080 端口，代理 /api 与 /uploads 到后端 8088
// - 生产构建关闭 sourcemap
export default defineConfig({
  plugins: [vue()],
  // sockjs-client 等老库引用了 Node.js 的 global 全局变量，
  // Vite 不自动 polyfill，这里把 global 重写为 globalThis（浏览器原生支持）
  define: {
    global: 'globalThis'
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8088',
        changeOrigin: true
      },
      '/ws': {
        target: 'http://localhost:8088',
        changeOrigin: true,
        ws: true
      }
    }
  },
  build: {
    sourcemap: false,
    chunkSizeWarningLimit: 1500
  }
})
