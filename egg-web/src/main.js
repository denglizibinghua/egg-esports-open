// Vue 3 入口：Element Plus 按需引入
import { createApp } from 'vue'
// 暗色主题 CSS 变量（体积小，全量引入无妨）
import 'element-plus/theme-chalk/dark/css-vars.css'
// 服务式组件按需引入（模板中的 <el-xxx> 由 unplugin-vue-components 自动导入）
import { ElMessage, ElMessageBox, ElLoading } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/loading/style/css'
// 图标按需引入：仅注册项目中实际使用的图标（grep src 目录得出）
import {
  Menu, Bell, ArrowDown, Close, Trophy, InfoFilled, Message,
  Edit, Lock, Link, Wallet, ArrowRight, QuestionFilled, Back,
  Loading, ChatDotSquare, User, SwitchButton, UserFilled, ArrowLeft,
  ZoomIn, ZoomOut, RefreshLeft, RefreshRight, Plus, Refresh
} from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)

// 按需注册 Element Plus 图标为全局组件（仅项目实际使用的 26 个，避免全量注册 200+ 个）
const icons = [
  Menu, Bell, ArrowDown, Close, Trophy, InfoFilled, Message,
  Edit, Lock, Link, Wallet, ArrowRight, QuestionFilled, Back,
  Loading, ChatDotSquare, User, SwitchButton, UserFilled, ArrowLeft,
  ZoomIn, ZoomOut, RefreshLeft, RefreshRight, Plus, Refresh
]
icons.forEach(icon => app.component(icon.name, icon))

// 注册服务式组件为全局属性（替代 app.use(ElementPlus)）
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.use(ElLoading) // 注册 v-loading 指令

// 全局错误处理：捕获未处理的组件异常，避免白屏
app.config.errorHandler = (err, instance, info) => {
  console.error('[Vue Error]', info, err)
}

app.use(router)
app.mount('#app')
