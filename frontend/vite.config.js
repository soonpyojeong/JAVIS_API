import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import fs from 'fs'

const pkg = JSON.parse(fs.readFileSync('./package.json', 'utf-8'))
const buildVersion = pkg.version

export default defineConfig({
  plugins: [vue()],
  define: {
    global: 'window',
    process: { env: {} },
    __BUILD_VERSION__: JSON.stringify(buildVersion),
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 8812,
    proxy: {
      '/api': {
        target: 'http://10.90.4.60:8813',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://10.90.4.60:8813',
        ws: true,
        changeOrigin: true,
      }
    }
  },
  build: {
    outDir: '../src/main/resources/static', // Spring Boot 연동시
    emptyOutDir: true
  }
})
