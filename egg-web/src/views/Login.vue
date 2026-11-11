<template>
  <div class="login-page">
    <div class="login-shell">
      <!-- Brand / visual panel -->
      <aside class="login-brand">
        <div class="brand-top">
          <div class="brand-logo">
            <span class="brand-logo-word">EGG</span>
            <span class="brand-logo-slash">/</span>
            <span class="brand-logo-sub">ESPORTS</span>
          </div>
          <span class="brand-channel">CH · 01</span>
        </div>

        <div class="brand-copy">
          <span class="brand-eyebrow">// PREDICTION ARENA</span>
          <h1 class="brand-slogan">PREDICT<br />THE<br /><em>UPSET</em></h1>
          <p class="brand-desc">校园电竞赛事预测平台。纯彩池、零抽水，用判断力赢下每一分。</p>
        </div>

        <ul class="brand-stats">
          <li>
            <span class="stat-num">0%</span>
            <span class="stat-label">抽水 / RAKE</span>
          </li>
          <li>
            <span class="stat-num">32</span>
            <span class="stat-label">赛事 / SERVED</span>
          </li>
          <li>
            <span class="stat-num">24H</span>
            <span class="stat-label">实时 / LIVE</span>
          </li>
        </ul>
      </aside>

      <!-- Form panel -->
      <section class="login-panel">
        <div class="login-card">
          <div class="card-head">
            <span class="card-eyebrow">// ACCESS TERMINAL</span>
            <h2 class="card-title">进入赛场</h2>
            <p class="card-sub">登录账号，或创建新的预测员档案</p>
          </div>

          <el-tabs v-model="activeTab" stretch class="login-tabs">
            <el-tab-pane label="登录" name="login">
              <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
                <el-form-item prop="username">
                  <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
                </el-form-item>
                <el-form-item class="form-item--submit">
                  <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handleLogin">登 录</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="注册" name="register">
              <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="login-form">
                <el-form-item prop="username">
                  <el-input v-model="registerForm.username" placeholder="用户名" :prefix-icon="User" />
                </el-form-item>
                <el-form-item prop="nickname">
                  <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" :prefix-icon="UserFilled" />
                </el-form-item>
                <el-form-item prop="password">
                  <el-input v-model="registerForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
                </el-form-item>
                <el-form-item prop="confirmPassword">
                  <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" :prefix-icon="Lock" show-password @keyup.enter="handleRegister" />
                </el-form-item>
                <el-form-item prop="email">
                  <el-input v-model="registerForm.email" placeholder="邮箱（选填，用于找回密码）" :prefix-icon="Message" />
                </el-form-item>
                <el-form-item class="form-item--submit">
                  <el-button type="primary" class="submit-btn" :loading="registerLoading" @click="handleRegister">注 册</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>

          <p class="demo-hint">
            <span class="demo-key">DEMO</span>
            <span class="demo-val">demo_player / demo123</span>
          </p>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { setToken, setUser } from '@/utils/auth'
// Element Plus 图标按需引入（用于 :prefix-icon 属性绑定）
import { User, Lock, UserFilled, Message } from '@element-plus/icons-vue'

export default {
  name: 'LoginPage',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.registerForm.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      // 暴露图标组件引用给模板的 :prefix-icon 属性使用（markRaw 避免被响应式化）
      User: markRaw(User),
      Lock: markRaw(Lock),
      UserFilled: markRaw(UserFilled),
      Message: markRaw(Message),
      activeTab: 'login',
      loginForm: {
        username: '',
        password: ''
      },
      registerForm: {
        username: '',
        nickname: '',
        password: '',
        confirmPassword: '',
        email: ''
      },
      loginRules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '用户名长度2-20个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 30, message: '密码长度6-30个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
        ]
      },
      loginLoading: false,
      registerLoading: false
    }
  },
  methods: {
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (!valid) return
        this.loginLoading = true
        request.post('/user/login', this.loginForm).then(res => {
          const data = res.data || res
          setToken(data.token)
          if (data.user) {
            setUser(data.user)
          }
          this.$message.success('登录成功')
          const redirect = this.$route.query.redirect || '/'
          this.$router.push(redirect)
        }).catch(() => {}).finally(() => {
          this.loginLoading = false
        })
      })
    },
    handleRegister() {
      this.$refs.registerForm.validate(valid => {
        if (!valid) return
        this.registerLoading = true
        const params = {
          username: this.registerForm.username,
          password: this.registerForm.password
        }
        if (this.registerForm.nickname) {
          params.nickname = this.registerForm.nickname
        }
        if (this.registerForm.email) {
          params.email = this.registerForm.email
        }
        request.post('/user/register', params).then(() => {
          this.$message.success('注册成功，请登录')
          this.activeTab = 'login'
          this.loginForm.username = this.registerForm.username
          this.loginForm.password = ''
        }).catch(() => {}).finally(() => {
          this.registerLoading = false
        })
      })
    }
  }
}
</script>

<style scoped>
.login-page {
  max-width: 1040px;
  margin: 0 auto;
  padding: clamp(8px, 3.5vh, 44px) 0 clamp(20px, 5vh, 56px);
}

.login-shell {
  position: relative;
  display: grid;
  grid-template-columns: 1.04fr 0.96fr;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  background: var(--bg-surface-1);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: login-rise 0.5s var(--ease-out) both;
}

/* corner ticks */
.login-shell::before,
.login-shell::after {
  content: '';
  position: absolute;
  width: 14px;
  height: 14px;
  border-color: var(--brand-line);
  border-style: solid;
  pointer-events: none;
  z-index: 2;
}

.login-shell::before {
  top: 0;
  left: 0;
  border-width: 1px 0 0 1px;
  border-top-left-radius: var(--radius-xl);
}

.login-shell::after {
  right: 0;
  bottom: 0;
  border-width: 0 1px 1px 0;
  border-bottom-right-radius: var(--radius-xl);
}

@keyframes login-rise {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- Brand panel ---------- */
.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 32px;
  padding: 34px 36px 30px;
  border-right: 1px solid var(--border-subtle);
  background: var(--bg-surface-2);
  background:
    radial-gradient(120% 90% at 0% 0%, color-mix(in srgb, var(--brand) 13%, transparent), transparent 58%),
    linear-gradient(170deg, var(--bg-surface-2) 0%, var(--bg-surface-1) 78%);
  overflow: hidden;
}

.login-brand::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(var(--border-subtle) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(ellipse 92% 78% at 28% 18%, #000 0%, transparent 76%);
  -webkit-mask-image: radial-gradient(ellipse 92% 78% at 28% 18%, #000 0%, transparent 76%);
  opacity: 0.9;
  pointer-events: none;
}

.login-brand::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 96px;
  height: 2px;
  background: linear-gradient(90deg, var(--brand), transparent);
}

.brand-top,
.brand-copy,
.brand-stats {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.brand-logo {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
}

.brand-logo-word {
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0.1em;
  color: var(--text-primary);
  line-height: 1;
}

.brand-logo-slash {
  font-family: var(--font-mono);
  font-size: 14px;
  color: var(--text-faint);
  transform: skewX(-12deg);
}

.brand-logo-sub {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.22em;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.brand-channel {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
  text-transform: uppercase;
}

.brand-copy {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.brand-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--brand);
}

.brand-slogan {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(54px, 6.4vw, 78px);
  line-height: 0.88;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: var(--cream);
  margin: 0;
}

.brand-slogan em {
  font-style: normal;
  color: var(--brand);
}

.brand-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
  max-width: 300px;
}

.brand-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  list-style: none;
  padding-top: 18px;
  border-top: 1px solid var(--border-subtle);
}

.brand-stats li {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding-left: 12px;
  border-left: 1px solid var(--border-default);
  min-width: 0;
}

.brand-stats li:first-child {
  padding-left: 0;
  border-left: 0;
}

.stat-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  color: var(--accent-primary);
}

.stat-label {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  white-space: nowrap;
}

/* ---------- Form panel ---------- */
.login-panel {
  display: flex;
  align-items: center;
  padding: 40px 36px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.015), transparent 40%);
}

.login-card {
  width: 100%;
  max-width: 380px;
  margin: 0 auto;
}

.card-head {
  margin-bottom: 22px;
}

.card-eyebrow {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 10px;
}

.card-title {
  font-family: var(--font-body);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--text-primary);
}

.card-sub {
  margin-top: 6px;
  font-size: 12.5px;
  color: var(--text-tertiary);
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.login-tabs :deep(.el-tabs__item) {
  font-family: var(--font-mono);
  font-size: 12px !important;
  letter-spacing: 0.14em;
  font-weight: 700 !important;
}

.login-tabs :deep(.el-tabs__nav-wrap::after) {
  background: var(--border-subtle) !important;
}

.login-form {
  margin-top: 2px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.login-form :deep(.form-item--submit) {
  margin-bottom: 4px;
  margin-top: 22px;
}

.login-form :deep(.el-input__wrapper) {
  min-height: 44px;
  padding: 0 12px;
  background: var(--bg-inset);
}

.login-form :deep(.el-input__inner) {
  height: 42px;
  font-size: 14px;
}

.login-form :deep(.el-form-item__error) {
  padding-top: 3px;
}

.submit-btn {
  width: 100%;
  height: 48px;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.demo-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 22px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  color: var(--text-faint);
  flex-wrap: wrap;
}

.demo-key {
  padding: 1px 5px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  letter-spacing: 0.12em;
  font-size: 9px;
}

.demo-val {
  color: var(--text-tertiary);
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .login-shell {
    grid-template-columns: 1fr;
    max-width: 460px;
    margin: 0 auto;
  }

  .login-brand {
    display: none;
  }

  .login-panel {
    padding: 30px 24px;
  }
}

@media (max-width: 480px) {
  .login-page {
    padding: 4px 0 10px;
  }

  .login-shell {
    border-radius: var(--radius-lg);
  }

  .login-shell::before {
    border-top-left-radius: var(--radius-lg);
  }

  .login-shell::after {
    border-bottom-right-radius: var(--radius-lg);
  }

  .login-panel {
    padding: 22px 16px 20px;
  }

  .login-card {
    max-width: 100%;
  }

  .card-title {
    font-size: 23px;
  }

  .card-head {
    margin-bottom: 16px;
  }

  .login-form :deep(.el-form-item) {
    margin-bottom: 16px;
  }

  .submit-btn {
    height: 46px;
    letter-spacing: 0.12em;
  }

  .demo-hint {
    flex-direction: column;
    align-items: flex-start;
    gap: 5px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-shell {
    animation: none;
  }
}
</style>
