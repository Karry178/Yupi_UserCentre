import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
// 自动导入Vue API
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { VantResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    // 自动导入Vue API
    AutoImport({
      imports: [
        'vue',
        'vue-router',
      ],
      dts: true, // 生成类型声明文件
    }),
    // 自动导入组件
    Components({
      resolvers: [VantResolver()],
      dts: true, // 生成类型声明文件
    }),
  ],
  server: {
    port: 3000,
    strictPort: true,  // 如果端口被占用则报错，而不是自动切换
  }
})



