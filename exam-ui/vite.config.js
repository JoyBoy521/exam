import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'vendor-element-plus'
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) return 'vendor-vue'
            if (id.includes('axios')) return 'vendor-axios'
            return 'vendor-misc'
          }
          return undefined
        }
      }
    }
  },
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/ws': {
        target: process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true,
        ws: true
      }
    }
  }
})
