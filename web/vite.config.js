import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
export default defineConfig({
  plugins: [uni()],
  server: {
    host: '127.0.0.1',
    cors: { origin: ['http://localhost:5174', 'http://127.0.0.1:5174'] },
    fs: { strict: true },
  },
})
