# Vue 3 迁移任务书 — 给 Trae

**交付日期**: 2026年7月7日前
**当前版本**: v24（Vue 2 + Element UI + webpack）
**目标版本**: v25（Vue 3 + Element Plus + Vite）

---

## 一、目标

将 egg-esports 前端从 Vue 2 生态完整迁移到 Vue 3 生态，**外观和功能 100% 保持一致**。

---

## 二、明确范围

### ✅ 必须做

| 项目 | 从 | 到 |
|------|----|----|
| 框架 | Vue 2.x | Vue 3.x（Option API 继续用，不强制 Composition API） |
| UI 库 | Element UI | Element Plus（最新稳定版） |
| 构建工具 | webpack + vue-cli-service | Vite |
| 路由 | Vue Router 3 | Vue Router 4 |
| 状态管理 | Vuex 3 | Pinia（推荐）或 Vuex 4 |

### ❌ 不做

- **不做 Nuxt / SSR** — 纯 SPA，保留 `index.html` 作为入口
- 不碰后端代码（Spring Boot 完全不改）
- 不改变任何用户可见的功能行为
- 不改变暗色电竞风视觉风格（CSS 变量、颜色、间距、动画全部保留）
- 不加新功能、不改业务逻辑

---

## 三、项目结构

```
D:\egg-esports\
├── egg-server/          ← 后端，完全不碰
└── egg-web/             ← 前端，全部改动在这里
    ├── package.json
    ├── src/
    │   ├── App.vue
    │   ├── main.js
    │   ├── router/index.js
    │   ├── store/              ← Vuex store
    │   ├── utils/
    │   │   ├── auth.js
    │   │   ├── request.js      ← axios 封装
    │   │   └── websocket.js
    │   ├── components/         ← 5个公共组件
    │   │   ├── AnnouncementBar.vue
    │   │   ├── AnnouncementPopup.vue
    │   │   ├── BrokeModal.vue
    │   │   ├── OnboardingCard.vue
    │   │   └── ...
    │   ├── views/              ← 14个页面组件
    │   │   ├── Home.vue
    │   │   ├── MatchList.vue
    │   │   ├── MatchDetail.vue  ← 最复杂（41KB，押注+评论+WebSocket+轮询）
    │   │   ├── Leaderboard.vue
    │   │   ├── Login.vue
    │   │   ├── Bank.vue
    │   │   ├── About.vue
    │   │   ├── UserCenter.vue
    │   │   ├── Notifications.vue
    │   │   ├── PublicProfile.vue
    │   │   ├── NotFound.vue
    │   │   └── admin/
    │   │       ├── AdminDashboard.vue
    │   │       ├── MatchCreate.vue
    │   │       └── MatchEdit.vue
    │   └── assets/             ← 静态资源
    ├── public/
    │   └── index.html
    └── vue.config.js           ← 替换为 vite.config.js
```

---

## 四、关键技术要点（Trae 自检清单）

### 4.1 Vite 初始化
- 用 `npm create vite@latest` 或手动建 `vite.config.js`
- 配置 `@` 别名 → `src/`
- 配置开发代理：`/api` → `http://localhost:8088`
- `index.html` 从 `public/` 移到根目录
- `process.env` → `import.meta.env`

### 4.2 Vue 3 破坏性变更（这些是坑，必须全过一遍）
- `v-model` 默认 prop: `value` → `modelValue`，event: `input` → `update:modelValue`
- `.sync` 修饰符移除 → 改用 `v-model:propName`
- `filters` 移除 → 模板里 `{{ val | format }}` 全改为 computed 或 methods
- `$listeners` 移除 → 合并到 `$attrs`
- `$on/$off/$once` 移除 → 事件总线需要用 `mitt`
- `Vue.prototype.xxx` → `app.config.globalProperties.xxx`
- `beforeDestroy` → `beforeUnmount`
- `destroyed` → `unmounted`
- `v-if` 和 `v-for` 同元素时，`v-if` 优先级高于 `v-for`（Vue 2 相反）
- `$children` 移除
- `this.$set()` 不再需要（响应式自动追踪）

### 4.3 Element Plus 差异
- 图标系统完全不同：`el-icon-xxx` → `@element-plus/icons-vue` 按需引入
- 部分组件 prop 改名（如 `el-input` 的 `icon` → `prefix-icon`/`suffix-icon`）
- 暗色模式：Element Plus 有内置暗色模式（`html.dark`），但你的项目是手写 CSS 变量，建议保留手写方式

### 4.4 Vue Router 4
- `new VueRouter({ mode: 'history' })` → `createRouter({ history: createWebHistory() })`
- `router-link` 的 `tag` prop 移除
- 路由守卫 API 不变（`beforeEach` 语法相同）

### 4.5 Pinia（替代 Vuex）
- `this.$store.state.xxx` → `store.xxx`
- `this.$store.dispatch('action')` → `store.action()`（直接调用）
- 没有 mutations（actions 直接改 state）

### 4.6 axios/request.js
- 不需要改动（axios 和 Vue 版本无关）
- `Message.error()` 改用 ElMessage（Element Plus 按需引入）

---

## 五、成功标准

全部通过才算完成：

- [ ] `npm run dev` 启动无报错
- [ ] `npm run build` 构建成功
- [ ] 14 个页面全部正常渲染，无白屏
- [ ] 登录/注册正常
- [ ] 押注全流程（选择队伍 → 确认弹窗 → 提交 → 结算）正常
- [ ] BrokeModal 积分耗尽弹窗正常
- [ ] AnnouncementPopup 公告弹窗正常
- [ ] OnboardingCard 新手引导正常显示/关闭
- [ ] 赔率 tooltip 正常
- [ ] 排行榜切换 tab 正常
- [ ] 通知铃铛 + 轮询正常
- [ ] WebSocket 评论实时推送正常
- [ ] 银行中心贷款/还贷/低保全流程正常
- [ ] 管理员功能（赛事创建/编辑/批量导入）正常
- [ ] 暗色主题视觉 100% 一致（颜色、间距、圆角、阴影、动画）
- [ ] 移动端适配正常（≤767px）
- [ ] 元素 UI 暗色覆盖（表格、对话框、下拉菜单、分页等）正常
- [ ] 无 console 报错（Vue warn 也不行）

---

## 六、禁止事项

- ❌ 不引入 TypeScript
- ❌ 不改任何 CSS 变量定义（`--bg-page`, `--accent-primary` 等 20+ 个变量）
- ❌ 不改 `request.js` 的 axios 拦截器逻辑
- ❌ 不改 `auth.js` 的 token/user 存取逻辑
- ❌ 不改 `websocket.js` 的 WebSocket 连接逻辑
- ❌ 不删除任何现有功能
- ❌ 不加新依赖（除迁移必需的 Vue3/ElementPlus/Vite/Pinia/Router 外）
- ❌ 不把 Option API 强行重构成 Composition API（能用就行）

---

## 七、执行方式

由 Trae 自主规划、自主拆分子任务、自主分配 agent 执行。你只需要：
1. 读取此文档理解目标和约束
2. 分析项目现有代码结构
3. 制定迁移步骤和顺序
4. 按步骤执行（可并行的工作自行并行）
5. 每完成一个模块自行验证
6. 全部完成后对照成功标准自查

本项目工作目录：`D:\egg-esports\egg-web`

开始。
