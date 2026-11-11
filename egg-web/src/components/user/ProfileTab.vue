<template>
  <div class="profile-tab">
    <!-- Identity card -->
    <section class="identity-card u-panel card-entrance">
      <div class="identity-main">
        <button type="button" class="avatar-frame" title="更换头像" @click="showCropper = true">
          <el-avatar :size="96" :src="user.avatar" :icon="UserFilled" />
          <span class="avatar-overlay">
            <el-icon><Edit /></el-icon>
            <span>更换头像</span>
          </span>
        </button>

        <div class="identity-meta">
          <div class="identity-eyebrow">// OPERATOR</div>
          <div class="user-nickname-row">
            <h2 class="user-nickname" :title="user.nickname || user.username">{{ user.nickname || user.username }}</h2>
            <!-- Rank Badge -->
            <button
              v-if="myRank && myRank.rank > 0"
              type="button"
              :class="['rank-badge', rankBadgeClass]"
              :title="'点击切换排行维度: ' + rankDimensionLabel"
              @click="$emit('cycle-rank')"
            >
              <span class="rank-num">#{{ myRank.rank }}</span>
              <span class="rank-dim-label">{{ rankDimensionLabel }}</span>
              <span class="rank-cycle" aria-hidden="true">↻</span>
            </button>
            <button
              v-else
              type="button"
              class="rank-badge rank-badge--none"
              title="点击切换排行维度"
              @click="$emit('cycle-rank')"
            >
              <span class="rank-dim-label">{{ rankDimensionLabel }}</span>
              <span class="rank-none-text">暂无排名</span>
              <span class="rank-cycle" aria-hidden="true">↻</span>
            </button>
          </div>

          <div class="identity-sub">
            <span class="identity-date">加入于 {{ formatDate(user.createTime) }}</span>
            <span class="identity-sep">/</span>
            <span class="identity-uid">BETTOR PROFILE</span>
          </div>

          <div class="identity-level-row">
            <span class="level-caption">LEVEL</span>
            <LevelBadge :count="user.totalBets || 0" />
          </div>
        </div>
      </div>

      <div class="identity-side">
        <div class="rank-score" v-if="loanStatus && loanStatus.rankingScore !== undefined">
          <span class="rank-score-label">RANK SCORE</span>
          <span class="rank-score-value">{{ loanStatus.rankingScore }}</span>
        </div>
        <div class="identity-actions">
          <el-button class="ghost-action" size="small" @click="openEditDialog">
            <el-icon><Edit /></el-icon> 编辑资料
          </el-button>
          <el-button class="ghost-action" size="small" @click="passwordDialogVisible = true">
            <el-icon><Lock /></el-icon> 修改密码
          </el-button>
        </div>
      </div>
    </section>

    <!-- Stat grid -->
    <div class="stats-grid">
      <article class="stat-cell stat-cell--points card-entrance">
        <span class="stat-kicker">POINTS</span>
        <span class="stat-value stat-value--mint">{{ user.points || 0 }}</span>
        <span class="stat-label">总积分</span>
      </article>
      <article class="stat-cell card-entrance">
        <span class="stat-kicker">NET PROFIT</span>
        <span class="stat-value" :class="netProfitClass">{{ netProfitDisplay }}</span>
        <span class="stat-label">净收益</span>
      </article>
      <article class="stat-cell card-entrance">
        <span class="stat-kicker">WIN RATE</span>
        <span class="stat-value">{{ accuracy }}<em>%</em></span>
        <span class="stat-label">胜率</span>
        <span class="stat-track">
          <span class="stat-fill" :style="{ width: accuracy + '%' }"></span>
        </span>
      </article>
      <article class="stat-cell card-entrance">
        <span class="stat-kicker">BETS</span>
        <span class="stat-value">{{ user.totalBets || 0 }}</span>
        <span class="stat-label">总押注</span>
      </article>
    </div>

    <!-- Secondary micro stats -->
    <div class="micro-strip card-entrance">
      <div class="micro-item">
        <span class="micro-label">猜中</span>
        <span class="micro-value">{{ user.correctBets || 0 }}</span>
      </div>
      <div class="micro-item">
        <span class="micro-label">当前连胜</span>
        <span class="micro-value">{{ user.winStreak || 0 }}</span>
      </div>
      <div class="micro-item">
        <span class="micro-label">最高连胜</span>
        <span class="micro-value">{{ user.maxWinStreak || 0 }}</span>
      </div>
      <span class="micro-note">DATA SYNCED AFTER SETTLEMENT</span>
    </div>

    <!-- Public Profile Link -->
    <div class="public-profile-hint" v-if="user.id">
      <span class="hint-mark">
        <el-icon><LinkIcon /></el-icon>
      </span>
      <span class="hint-label">公开主页</span>
      <code class="profile-url" :title="profileUrl">{{ profileUrl }}</code>
      <el-button link class="copy-url-btn" @click="copyProfileUrl">复制</el-button>
    </div>

    <!-- Bank Quick Link -->
    <router-link v-if="loanStatus" to="/bank" class="bank-entry card-entrance">
      <span class="bank-mark">
        <el-icon><Wallet /></el-icon>
      </span>
      <span class="bank-text">
        <strong>银行中心</strong>
        <small>BANK · 存取积分 / 贷款管理</small>
      </span>
      <span class="bank-meta">
        <span class="bank-debt" v-if="loanStatus.loanAmount < 0">
          贷款 {{ formatLoanAmount(loanStatus.loanAmount) }}
        </span>
        <span class="bank-clean" v-else>无贷款</span>
        <el-icon class="bank-arrow"><ArrowRight /></el-icon>
      </span>
    </router-link>

    <!-- Edit Profile Dialog -->
    <el-dialog
      title="编辑个人资料"
      v-model="editDialogVisible"
      width="420px"
      :close-on-click-modal="false"
      class="uc-dialog"
    >
      <el-form :model="editForm" :rules="editRules" ref="editForm" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-avatar :size="64" :src="avatarPreview" :icon="UserFilled" />
            <div class="avatar-upload-meta">
              <span class="avatar-upload-hint">1:1 方形 · 圆形展示</span>
              <el-button size="small" link @click="showCropper = true">更换头像</el-button>
            </div>
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
    <el-dialog
      title="修改密码"
      v-model="passwordDialogVisible"
      width="420px"
      :close-on-click-modal="false"
      class="uc-dialog"
    >
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
      <template #footer><span class="dialog-footer">
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
import LevelBadge from '@/components/user/LevelBadge.vue'
// Element Plus SVG 图标（迁移自 Element UI 字体图标）
import { UserFilled, Edit, Lock, Link as LinkIcon, Wallet, ArrowRight } from '@element-plus/icons-vue'

export default {
  name: 'ProfileTab',
  // UserFilled 仅作为 :icon 的 data 变量使用，无需注册为组件；Link 用别名避开 HTML 保留名
  components: { AvatarCropper, LevelBadge, Edit, Lock, LinkIcon, Wallet, ArrowRight },
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
    netProfitDisplay() {
      var v = (this.user && this.user.netProfit) || 0
      return v > 0 ? '+' + v : '' + v
    },
    netProfitClass() {
      var v = (this.user && this.user.netProfit) || 0
      if (v > 0) return 'stat-value--mint'
      if (v < 0) return 'stat-value--danger'
      return ''
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
.profile-tab {
  display: flex;
  flex-direction: column;
}

/* ---------- Identity card ---------- */
.identity-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 28px;
  margin-bottom: 16px;
}

.identity-main {
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

/* Avatar with ring + edit overlay */
.avatar-frame {
  position: relative;
  display: block;
  flex-shrink: 0;
  width: 104px;
  height: 104px;
  padding: 3px;
  border: none;
  border-radius: 50%;
  background: conic-gradient(from 200deg, var(--brand), var(--accent-primary) 58%, var(--brand));
  cursor: pointer;
  transition: box-shadow var(--duration-normal) var(--ease-out), transform var(--duration-normal) var(--ease-out);
}

.avatar-frame:hover,
.avatar-frame:focus-visible {
  transform: translateY(-2px);
  box-shadow: var(--shadow-brand);
}

.avatar-frame :deep(.el-avatar) {
  width: 100% !important;
  height: 100% !important;
  display: block;
  border: 1px solid var(--border-strong);
  background: var(--bg-surface-3);
  border-radius: 50%;
  overflow: hidden;
}

.avatar-frame :deep(.el-avatar img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 3px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  padding-bottom: 12px;
  background: linear-gradient(180deg, transparent 30%, var(--bg-page) 100%);
  color: var(--cream);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  opacity: 0;
  transition: opacity var(--duration-fast) ease;
  pointer-events: none;
}

.avatar-frame:hover .avatar-overlay,
.avatar-frame:focus-visible .avatar-overlay {
  opacity: 1;
}

.avatar-overlay .el-icon {
  font-size: 14px;
  color: var(--brand);
}

@media (hover: none) {
  .avatar-overlay {
    inset: auto 2px 2px auto;
    width: 26px;
    height: 26px;
    padding: 0;
    justify-content: center;
    align-items: center;
    background: var(--bg-page);
    border: 1px solid var(--brand-line);
    border-radius: 50%;
    opacity: 1;
    font-size: 0;
  }
  .avatar-overlay span {
    display: none;
  }
}

.identity-meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.identity-eyebrow {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
}

.user-nickname-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.user-nickname {
  font-family: var(--font-body);
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--text-primary);
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
}

/* Rank chip */
.rank-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 24px;
  padding: 0 9px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--text-secondary);
  cursor: pointer;
  white-space: nowrap;
  transition: border-color var(--duration-fast) ease, color var(--duration-fast) ease;
}

.rank-badge:hover {
  border-color: var(--brand-line);
  color: var(--text-primary);
}

.rank-badge--gold {
  color: var(--gold);
  border-color: var(--gold);
  background: var(--bg-surface-2);
}

.rank-badge--silver {
  color: var(--silver);
  border-color: var(--silver);
  background: var(--bg-surface-2);
}

.rank-badge--bronze {
  color: var(--bronze);
  border-color: var(--bronze);
  background: var(--bg-surface-2);
}

.rank-badge--normal {
  color: var(--text-secondary);
}

.rank-badge--none {
  color: var(--text-tertiary);
  font-weight: 600;
}

.rank-num {
  font-size: 11px;
  letter-spacing: 0.02em;
}

.rank-dim-label {
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.08em;
  opacity: 0.7;
}

.rank-cycle {
  font-size: 11px;
  opacity: 0.45;
}

.rank-none-text {
  font-size: 10px;
  letter-spacing: 0.04em;
}

.identity-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.identity-sep {
  color: var(--text-faint);
}

.identity-uid {
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--text-faint);
}

.identity-level-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.level-caption {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}

.identity-side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 14px;
  padding-left: 24px;
  border-left: 1px solid var(--border-subtle);
  flex-shrink: 0;
}

.rank-score {
  text-align: right;
}

.rank-score-label {
  display: block;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}

.rank-score-value {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  color: var(--cream);
}

.identity-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.identity-actions .el-button + .el-button {
  margin-left: 0;
}

.ghost-action {
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  font-weight: 600;
}

.ghost-action:hover {
  border-color: var(--brand-line);
  color: var(--brand);
  background: var(--bg-surface-3);
}

/* ---------- Stat grid ---------- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-cell {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 16px 16px 14px;
  background:
    linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 52%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: transform var(--duration-normal) var(--ease-out), border-color var(--duration-normal) var(--ease-out);
}

.stat-cell::before {
  content: '';
  position: absolute;
  top: 0;
  left: 16px;
  width: 22px;
  height: 2px;
  background: var(--border-strong);
}

.stat-cell:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
}

.stat-cell--points {
  background:
    linear-gradient(180deg, var(--brand-soft) 0%, transparent 60%),
    var(--bg-surface-1);
  border-color: var(--brand-line);
}

.stat-cell--points::before {
  width: 34px;
  background: var(--brand);
}

.stat-kicker {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.stat-value {
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
  line-height: 1.15;
  color: var(--text-primary);
}

.stat-value em {
  font-style: normal;
  font-size: 13px;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.stat-value--mint {
  color: var(--accent-primary);
}

.stat-value--danger {
  color: var(--accent-danger);
}

.stat-cell--points .stat-value {
  font-size: 32px;
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.stat-track {
  height: 3px;
  margin-top: 9px;
  background: var(--bg-surface-3);
  border-radius: 1px;
  overflow: hidden;
}

.stat-fill {
  display: block;
  height: 100%;
  background: var(--accent-primary);
  transition: width 0.6s var(--ease-out);
}

/* ---------- Micro strip ---------- */
.micro-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0 16px;
  margin-top: 12px;
  padding: 11px 16px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.micro-item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-right: 16px;
  border-right: 1px solid var(--border-subtle);
}

.micro-item:last-of-type {
  padding-right: 0;
  border-right: none;
}

.micro-label {
  font-size: 11px;
  color: var(--text-tertiary);
}

.micro-value {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

.micro-note {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-faint);
  text-transform: uppercase;
}

/* ---------- Public profile ---------- */
.public-profile-hint {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  min-width: 0;
}

.hint-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  flex-shrink: 0;
  color: var(--accent-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-size: 13px;
}

.hint-label {
  flex-shrink: 0;
  font-size: 12px;
  color: var(--text-tertiary);
}

.profile-url {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--accent-secondary);
  background: transparent;
  padding: 0;
}

.copy-url-btn {
  flex-shrink: 0;
  color: var(--brand);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

/* ---------- Bank entry ---------- */
.bank-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 16px;
  padding: 16px 18px;
  background:
    linear-gradient(90deg, var(--brand-soft) 0%, transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.bank-entry:hover {
  transform: translateY(-2px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-md);
}

.bank-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
  font-size: 18px;
}

.bank-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.bank-text strong {
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-primary);
  white-space: nowrap;
}

.bank-text small {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.bank-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
  flex-shrink: 0;
}

.bank-debt {
  padding: 2px 8px;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  color: var(--accent-warning);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.bank-clean {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--accent-primary);
}

.bank-arrow {
  color: var(--text-tertiary);
  transition: transform var(--duration-fast) ease, color var(--duration-fast) ease;
}

.bank-entry:hover .bank-arrow {
  color: var(--brand);
  transform: translateX(3px);
}

/* ---------- Dialogs ---------- */
.avatar-upload {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-upload :deep(.el-avatar) {
  border: 1px solid var(--brand-line);
  background: var(--bg-surface-3);
}

.avatar-upload-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.avatar-upload-hint {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--text-faint);
  text-transform: uppercase;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .identity-card {
    flex-direction: column;
    align-items: stretch;
    gap: 18px;
    padding: 20px;
  }

  .identity-side {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
    padding-left: 0;
    padding-top: 16px;
    border-left: none;
    border-top: 1px solid var(--border-subtle);
  }

  .rank-score {
    text-align: left;
  }

  .identity-actions {
    justify-content: flex-end;
  }

  .micro-note {
    display: none;
  }
}

@media (max-width: 480px) {
  .identity-main {
    gap: 14px;
  }

  .avatar-frame {
    width: 84px;
    height: 84px;
  }

  .user-nickname {
    font-size: 21px;
  }

  .identity-card {
    padding: 16px;
  }

  .identity-side {
    flex-wrap: wrap;
    gap: 12px;
  }

  .identity-actions {
    width: 100%;
  }

  .identity-actions .el-button {
    flex: 1;
    margin-left: 0;
  }

  .stat-cell {
    padding: 14px 13px 12px;
  }

  .stat-cell--points .stat-value {
    font-size: 26px;
  }

  .stat-value {
    font-size: 22px;
  }

  .stat-label {
    font-size: 11px;
  }

  .micro-strip {
    gap: 0 14px;
    padding: 10px 13px;
  }

  .micro-item {
    padding-right: 14px;
  }

  .micro-item:last-of-type {
    padding-right: 0;
  }

  .public-profile-hint {
    flex-wrap: wrap;
  }

  .profile-url {
    flex-basis: 100%;
    order: 4;
  }

  .bank-entry {
    padding: 14px;
    gap: 12px;
  }

  .bank-text small {
    letter-spacing: 0.08em;
  }

  .bank-meta {
    gap: 6px;
  }
}
</style>
