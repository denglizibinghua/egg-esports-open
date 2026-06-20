<template>
  <div class="login-container">
    <div class="login-card">
      <h1 class="login-title">EGG ESPORTS</h1>
      <p class="login-subtitle">赛事竞猜平台</p>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="el-icon-user" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleLogin" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="loginLoading" @click="handleLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form ref="registerForm" :model="registerForm" :rules="registerRules" class="login-form">
            <el-form-item prop="username">
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="el-icon-user" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="registerForm.nickname" placeholder="昵称（选填）" prefix-icon="el-icon-s-custom" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="el-icon-lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="el-icon-lock" show-password @keyup.enter.native="handleRegister" />
            </el-form-item>
            <el-form-item prop="email">
              <el-input v-model="registerForm.email" placeholder="邮箱（选填，用于找回密码）" prefix-icon="el-icon-message" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="submit-btn" :loading="registerLoading" @click="handleRegister">注 册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { setToken, setUser } from '@/utils/auth'

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
.login-container {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(ellipse 60% 50% at 50% 0%, rgba(0,229,160,0.06) 0%, transparent 60%),
    var(--bg-page);
}

.login-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 44px 36px 28px;
  width: 100%;
  max-width: 400px;
  box-shadow: var(--shadow-lg);
}

.login-title {
  text-align: center;
  font-family: var(--font-display);
  font-size: 36px;
  color: var(--accent-primary);
  letter-spacing: 4px;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  color: var(--text-tertiary);
  font-size: 14px;
  margin-bottom: 28px;
}

.login-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 20px rgba(0,229,160,0.20);
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}

.submit-btn:hover {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0,229,160,0.35);
}
</style>