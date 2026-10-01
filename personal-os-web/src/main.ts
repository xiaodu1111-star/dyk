import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'
import { initTheme } from '@/composables/useTheme'
import '@/styles/index.scss'

// 首屏同步应用主题，避免闪白（index.html 内联脚本已做一次，这里为权威实现）
initTheme()

const app = createApp(App)

// Pinia 必须在 router 之前安装，路由守卫里会用到 store
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

app.mount('#app')
