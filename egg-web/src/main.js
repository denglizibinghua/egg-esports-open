// Vue 3 入口：Element Plus 按需引入
import { createApp } from 'vue'
// 暗色主题 CSS 变量（体积小，全量引入无妨）
import 'element-plus/theme-chalk/dark/css-vars.css'
// 服务式组件按需引入（模板中的 <el-xxx> 由 unplugin-vue-components 自动导入）
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全量注册 Element Plus 图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 注册服务式组件为全局属性（替代 app.use(ElementPlus)）
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.use(ElLoading) // 注册 v-loading 指令

app.use(router)
app.mount('#app')
