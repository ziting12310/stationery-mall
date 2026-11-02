import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'

// 创建 Vue 应用：注册 Element Plus 与路由
createApp(App).use(ElementPlus).use(router).mount('#app')
