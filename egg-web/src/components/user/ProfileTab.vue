<template>
  <div class="profile-tab">
    <!-- User Info Card -->
    <div class="user-info-card">
      <div class="user-avatar-section">
        <el-avatar :size="80" :src="user.avatar" :icon="UserFilled" />
        <div class="user-meta">
          <div class="user-nickname-row">
            <span class="user-nickname">{{ user.nickname || user.username }}</span>
            <!-- Rank Badge -->
            <span v-if="myRank && myRank.rank > 0"
                  :class="['rank-badge', rankBadgeClass]"
                  @click="$emit('cycle-rank')"
                  style="cursor: pointer"
                  :title="'点击切换排行维度: ' + rankDimensionLabel">
              <span v-if="myRank.rank === 1 && rankDimension === 'points'" class="rank-crown">👑</span>
              <span v-else-if="myRank.rank === 1">🥇</span>
              <span v-else-if="myRank.rank === 2">🥈</span>
              <span v-else-if="myRank.rank === 3">🥉</span>
              #{{ myRank.rank }}
              <span class="rank-dim-label">{{ rankDimensionLabel }}</span>
            </span>
            <span v-else class="rank-badge rank-badge--none"
                  @click="$emit('cycle-rank')"
                  style="cursor: pointer"
                  :title="'点击切换排行维度'">
              {{ rankDimensionLabel }}暂无
            </span>
          </div>
          <div class="user-join-date">加入于 {{ formatDate(user.createTime) }}</div>
        </div>
      </div>
      <div class="user-info-right">
        <div class="user-points">
          <div class="points-value">{{ user.points }}</div>
          <div class="points-label">积分</div>
          <div class="points-sub" v-if="loanStatus && loanStatus.rankingScore !== undefined">
            排名分 {{ loanStatus.rankingScore }}
          </div>
        </div>
        <el-button class="edit-profile-btn" size="small" @click="openEditDialog">
          <el-icon><Edit /></el-icon> 编辑资料
        </el-button>
        <el-button class="edit-profile-btn" size="small" @click="passwordDialogVisible = true" style="margin-left: 8px;">
          <el-icon><Lock /></el-icon> 修改密码
        </el-button>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ user.totalBets || 0 }}</div>
        <div class="stat-label">总押注</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ user.correctBets || 0 }}</div>
        <div class="stat-label">猜中</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ accuracy }}%</div>
        <div class="stat-label">胜率</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ user.winStreak || 0 }}</div>
        <div class="stat-label">连胜</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ user.maxWinStreak || 0 }}</div>
        <div class="stat-label">最高连胜</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ user.points || 0 }}</div>
        <div class="stat-label">总积分</div>
      </div>
    </div>

    <!-- Public Profile Link -->
    <div class="public-profile-hint" v-if="user.id">
      <el-icon><LinkIcon /></el-icon>
      <span>公开主页链接：</span>
      <code class="profile-url">{{ profileUrl }}</code>
      <el-button link class="copy-url-btn" @click="copyProfileUrl">复制</el-button>
    </div>

    <!-- Bank Quick Link -->
    <div class="bank-link-card" v-if="loanStatus">
      <router-link to="/bank" class="bank-link">
        <div class="bank-link-left">
          <el-icon><Wallet /></el-icon>
          <span>银行中心</span>
        </div>
        <div class="bank-link-right">
          <span class="bank-link-debt" v-if="loanStatus.loanAmount < 0">
            贷款 {{ formatLoanAmount(loanStatus.loanAmount) }}
          </span>
          <span class="bank-link-clean" v-else>无贷款</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </router-link>
    </div>

    <!-- Edit Profile Dialog -->
    <el-dialog
      title="编辑个人资料"
      v-model="editDialogVisible"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="editRules" ref="editForm" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-avatar :size="64" :src="avatarPreview" :icon="UserFilled" />
            <el-button size="small" link @click="showCropper = true">更换头像</el-button>
          </div>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入新昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer><span class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveProfile">保存</el-button>
      </span></template>
    </el-dialog>

    <!-- Password Change Dialog -->
    <el-dialog title="修改密码" v-model="passwordDialogVisible" width="420px" :close-on-click-modal="false">
      <el-form ref="passwordForm" :model="passwordForm" :rules="passwordRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="6-30个字符" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer><span>
        <el-button @click="passwordDialogVisible = false" :disabled="passwordSaving">取消</el-button>
        <el-button type="primary" @click="submitPassword" :loading="passwordSaving">确认修改</el-button>
      </span></template>
    </el-dialog>

    <!-- Avatar Cropper -->
    <avatar-cropper v-model="showCropper" @cropped="onAvatarCropped" />
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import AvatarCropper from '@/components/AvatarCropper.vue'
// Element Plus SVG 图标（迁移自 Element UI 字体图标）
import { UserFilled, Edit, Lock, Link as LinkIcon, Wallet, ArrowRight } from '@element-plus/icons-vue'

export default {
  name: 'ProfileTab',
  // UserFilled 仅作为 :icon 的 data 变量使用，无需注册为组件；Link 用别名避开 HTML 保留名
  components: { AvatarCropper, Edit, Lock, LinkIcon, Wallet, ArrowRight },
  props: {
    // 当前登录用户信息
    user: { type: Object, required: true },
    // 我的排名信息
    myRank: { type: Object, default: null },
    // 排行维度（points / accuracy / worst-profit）
    rankDimension: { type: String, default: 'points' },
    // 贷款 / 银行状态
    loanStatus: { type: Object, default: null }
  },
  // 与父组件通信的事件
  emits: ['update-profile', 'avatar-updated', 'refresh-user', 'cycle-rank'],
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      UserFilled: markRaw(UserFilled),
      editDialogVisible: false,
      showCropper: false,
      editSaving: false,
      editForm: {
        nickname: ''
      },
      avatarPreview: '',
      editRules: {
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
        ]
      },
      passwordDialogVisible: false,
      passwordSaving: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 30, message: '密码长度需在6-30个字符之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入的密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      }
    }
  },
  computed: {
    // 胜率百分比
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.min(100, Math.round((this.user.correctBets / this.user.totalBets) * 100))
    },
    rankDimensionLabel() {
      var labels = { 'points': '积分', 'accuracy': '胜率', 'worst-profit': '亏损' }
      return labels[this.rankDimension] || '积分'
    },
    rankBadgeClass() {
      if (!this.myRank || this.myRank.rank <= 0) return 'rank-badge--none'
      if (this.myRank.rank === 1) return 'rank-badge--gold'
      if (this.myRank.rank === 2) return 'rank-badge--silver'
      if (this.myRank.rank === 3) return 'rank-badge--bronze'
      return 'rank-badge--normal'
    },
    profileUrl() {
      if (!this.user || !this.user.id) return ''
      return window.location.origin + '/profile/' + this.user.id
    }
  },
  methods: {
    openEditDialog() {
      this.editForm.nickname = this.user.nickname || this.user.username || ''
      this.avatarPreview = this.user.avatar || ''
      this.editDialogVisible = true
      this.$nextTick(function () {
        if (this.$refs.editForm) {
          this.$refs.editForm.clearValidate()
        }
      }.bind(this))
    },
    // 头像裁剪完成：本地更新预览，并通知父组件更新 user（避免直接修改 props）
    onAvatarCropped(url) {
      this.avatarPreview = url
      this.$emit('avatar-updated', url)
    },
    // 保存昵称：调用接口成功后通知父组件更新 user
    saveProfile() {
      this.$refs.editForm.validate(function (valid) {
        if (!valid) return
        this.editSaving = true
        request.put('/user/profile', { nickname: this.editForm.nickname }).then(function () {
          this.$message.success('资料更新成功')
          this.$emit('update-profile', this.editForm.nickname)
          this.editDialogVisible = false
        }.bind(this)).catch(function () {
          this.$message.error('更新失败，请重试')
        }.bind(this)).finally(function () {
          this.editSaving = false
        }.bind(this))
      }.bind(this))
    },
    submitPassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) return
        this.passwordSaving = true
        request.put('/user/password', {
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword
        }).then(() => {
          this.$message.success('密码修改成功')
          this.passwordDialogVisible = false
          this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
          this.$refs.passwordForm.resetFields()
        }).catch(() => {})
        .finally(() => { this.passwordSaving = false })
      })
    },
    copyProfileUrl() {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(this.profileUrl).then(function () {
          this.$message.success('链接已复制')
        }.bind(this))
      } else {
        var input = document.createElement('input')
        input.value = this.profileUrl
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('链接已复制')
      }
    },
    formatLoanAmount(amount) {
      if (amount === null || amount === undefined) return '0'
      if (amount >= 0) return '0'
      return amount.toString()
    },
    formatDate(timeStr) {
      if (!timeStr) return '-'
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + m + '-' + day + ' ' + h + ':' + min
    }
  }
}
</script>

<style scoped>
/* User Info Card */
.user-info-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
}

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-nickname-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.user-nickname {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 600;
}

.user-join-date {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 4px;
}

.user-info-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-points {
  text-align: center;
}

.points-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 32px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.points-label {
  color: var(--text-tertiary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.points-sub {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
}

/* Rank Badge */
.rank-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  border-radius: var(--radius-full);
  font-family: var(--font-mono);
  letter-spacing: 0.04em;
}

.rank-badge--gold {
  color: #1a1200;
  background: linear-gradient(135deg, #ffd700, #ffb300);
  box-shadow: 0 0 12px rgba(255, 215, 0, 0.4);
}

.rank-badge--silver {
  color: #1a1a1a;
  background: linear-gradient(135deg, #e0e0e0, #b0b0b0);
  box-shadow: 0 0 8px rgba(192, 192, 192, 0.3);
}

.rank-badge--bronze {
  color: #1a0e00;
  background: linear-gradient(135deg, #cd7f32, #a0522d);
  box-shadow: 0 0 8px rgba(205, 127, 50, 0.3);
}

.rank-badge--normal {
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
}

.rank-badge--none {
  color: var(--text-tertiary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  font-weight: 500;
  font-family: var(--font-body);
}

.rank-crown {
  font-size: 14px;
}

.rank-total {
  color: var(--text-tertiary);
}

.rank-dim-label {
  font-size: 10px;
  opacity: 0.7;
  margin-left: 4px;
  font-weight: 400;
}

/* Edit Profile Button */
.edit-profile-btn {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-md) !important;
  font-weight: 500;
  transition: all var(--duration-fast) ease;
}

.edit-profile-btn:hover {
  border-color: var(--accent-primary) !important;
  color: var(--accent-primary) !important;
  background: rgba(0, 229, 160, 0.06) !important;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  text-align: center;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
}

.stat-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Public Profile Hint */
.public-profile-hint {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.public-profile-hint i {
  color: var(--accent-secondary);
  font-size: 16px;
}

.profile-url {
  color: var(--accent-secondary);
  background: var(--bg-surface-2);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 12px;
  word-break: break-all;
}

.copy-url-btn {
  color: var(--accent-primary) !important;
  font-size: 12px !important;
  padding: 2px 8px !important;
  flex-shrink: 0;
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-uploader {
  display: inline-block;
}

/* Dialog Footer */
.dialog-footer .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
}

/* Bank Quick Link */
.bank-link-card {
  margin-top: 20px;
}

.bank-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  text-decoration: none;
  transition: border-color var(--duration-fast) ease,
              background var(--duration-fast) ease;
}

.bank-link:hover {
  border-color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.04);
}

.bank-link-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.bank-link-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.bank-link-debt {
  font-family: var(--font-mono);
  color: #f56c6c;
}

.bank-link-clean {
  color: var(--accent-primary);
}

/* Responsive */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .user-info-card {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  .user-avatar-section {
    flex-direction: column;
  }
  .user-info-right {
    flex-direction: column;
    gap: 12px;
  }
}
</style>
