import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 仅兼容共享工具和旧 settings 的 CommonJS；应用入口和构建已使用 ESM。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), 'VUE_APP_')
  return {
    plugins: [vue(), { name: 'legacy-local-exports', enforce: 'pre', transform(code, id) {
      if (id.endsWith('/src/settings.js') || id.endsWith('/frontend-shared/src/index.js')) return code.replace('module.exports =', 'export default')
    } }],
    resolve: { extensions: ['.mjs', '.js', '.mts', '.ts', '.jsx', '.tsx', '.json', '.vue'], alias: { '@': fileURLToPath(new URL('./src', import.meta.url)), '@baituan/frontend-shared': fileURLToPath(new URL('../frontend-shared', import.meta.url)) }, dedupe: ['vue', 'pinia'] },
    define: Object.fromEntries(Object.entries(env).map(([key, value]) => [`process.env.${key}`, JSON.stringify(value)])),
    server: { port: 9532, strictPort: true, fs: { allow: ['..'] } },
    test: { globals: true, environment: 'jsdom', include: ['tests/unit/**/*.spec.js'], clearMocks: true },
    build: { target: 'es2022', sourcemap: false }
  }
})
