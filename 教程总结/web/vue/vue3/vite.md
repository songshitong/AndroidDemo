
vite   打包构建工具，可以用于ssr
https://cn.vitejs.dev/guide/why.html


vite的常用配置
https://juejin.cn/post/7170843707217412126
```
// vite.config.js
import { defineConfig } from 'vite' // 使用 defineConfig工具函数获取类型提示：
import vue from '@vitejs/plugin-vue'
import { viteMockServe } from 'vite-plugin-mock'

export default defineConfig({
  base: '/foo/', // 开发或生产环境服务的公共基础路径
  
  //node_modules 中的依赖模块构建过一次就会缓存在 node_modules/.vite/deps 文件夹下，下一次会直接使用缓存的文件。而有时候我们想要修改依赖模块的代码，
  //做一些测试或者打个补丁，这时候就要用到强制依赖预构建。
  optimizeDeps: {
    force: true // 强制进行依赖预构建
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@import '/src/assets/styles/variables.scss';` // 引入全局变量文件
      }
    },
    postcss: {//PostCSS使用插件来处理css
      plugins: [
        // viewport 布局适配     将px单位转为 vw 或 vh 
        postcssPxToViewport({
          viewportWidth: 375
        })
      ]
    }
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src') // 路径别名
    },
    extensions: ['.js', '.ts', '.json'] // 导入时想要省略的扩展名列表
  },
  server: {
    host: true, // 监听所有地址
    proxy: { //接口访问会经过替换
      // 字符串简写写法
      '/foo': 'http://localhost:4567',
      // 选项写法
      '/api': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '')
      },
      // 正则表达式写法
      '^/fallback/.*': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/fallback/, '')
      },
      // 使用 proxy 实例
      '/api': {
        target: 'http://jsonplaceholder.typicode.com',
        changeOrigin: true,
        configure: (proxy, options) => {
          // proxy 是 'http-proxy' 的实例
        }
      },
      // Proxying websockets or socket.io
      '/socket.io': {
        target: 'ws://localhost:3000',
        ws: true
      }
    }
  },
  build: {
    outDir: 'build', // 打包文件的输出目录
    assetsDir: 'static', // 静态资源的存放目录
    assetsInlineLimit: 4096 // 图片转 base64 编码的阈值
  },
  plugins: [ //使用插件
    vue(),
    viteMockServe()
  ]
})
```