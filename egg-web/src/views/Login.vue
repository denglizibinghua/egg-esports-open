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
  name: 'Login',
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
        confirmPassword: ''
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
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.login-card {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 40px 36px 24px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
}

.login-title {
  text-align: center;
  font-size: 28px;
  font-weight: 800;
  color: #FF4655;
  letter-spacing: 3px;
  margin-bottom: 4px;
}

.login-subtitle {
  text-align: center;
  color: #a0a0b0;
  font-size: 14px;
  margin-bottom: 28px;
}

.login-form {
  margin-top: 20px;
}

.submit-btn {
  width: 100%;
  background-color: #FF4655;
  border-color: #FF4655;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
}

.submit-btn:hover {
  background-color: #e63e4d;
  border-color: #e63e4d;
}
</style>