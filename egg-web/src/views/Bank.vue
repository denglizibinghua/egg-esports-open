<template>
  <div class="bank-page">
    <div class="page-header">
      <h2 class="page-title">银行中心</h2>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="bank-loading">
      <div v-for="i in 3" :key="'skel-'+i" class="skeleton-card">
        <div class="skeleton-line skeleton-line--val"></div>
        <div class="skeleton-line skeleton-line--lbl"></div>
      </div>
    </div>

    <div v-else-if="status" class="bank-content">
      <!-- Overview Cards -->
      <div class="bank-overview">
        <div class="bank-card">
          <div class="bank-card-value" :class="{ 'text-danger': status.loanAmount < 0 }">
            {{ status.loanAmount || 0 }}
          </div>
          <div class="bank-card-label">贷款余额</div>
        </div>
        <div class="bank-card">
          <div class="bank-card-value">{{ status.loanLimit }}</div>
          <div class="bank-card-label">贷款上限</div>
        </div>
        <div class="bank-card">
          <div class="bank-card-value">{{ status.activeBets || 0 }}</div>
          <div class="bank-card-label">押注中</div>
        </div>
      </div>

      <!-- Quick Stats -->
      <div class="bank-quick-stats">
        <div class="quick-stat">
          <span class="quick-stat-label">排行榜积分</span>
          <span class="quick-stat-value">{{ status.rankingScore }}</span>
        </div>
        <div class="quick-stat">
          <span class="quick-stat-label">当前现金</span>
          <span class="quick-stat-value">{{ userPoints }}</span>
        </div>
      </div>

      <!-- Manual Repay -->
      <div class="bank-section" v-if="status.loanAmount < 0">
        <h4 class="section-title">主动还款</h4>
        <div class="repay-form">
          <div class="repay-input-row">
            <el-input-number
              v-model="repayAmount"
              :min="1"
              :max="maxRepay"
              :step="1"
              size="medium"
              class="repay-input"
            />
            <span class="repay-unit">积分</span>
            <el-button
              type="primary"
              :loading="repaying"
              :disabled="repayAmount <= 0"
              @click="doRepay"
              class="repay-btn"
            >确认还款</el-button>
          </div>
          <div class="repay-info">
            <span>当前债务: {{ Math.abs(status.loanAmount) }}</span>
            <span class="repay-sep">|</span>
            <span>还清需: {{ Math.abs(status.loanAmount) }}</span>
          </div>
        </div>
      </div>

      <!-- Daily Stipend -->
      <div class="bank-section">
        <h4 class="section-title">每日低保</h4>
        <div class="stipend-section">
          <div v-if="status.canClaimStipend" class="stipend-available">
            <p class="stipend-desc">你的积分已耗尽且贷款已达上限，可领取每日低保。</p>
            <el-button
              type="warning"
              :loading="claimingStipend"
              @click="claimStipend"
              class="stipend-btn"
            >领取 {{ stipendAmount }} 积分</el-button>
          </div>
          <div v-else class="stipend-unavailable">
            <p class="stipend-desc">
              <template v-if="userPoints > 0">你还有可用积分，无需领取低保。</template>
              <template v-else-if="status.loanAmount >= 0 && Math.abs(status.loanAmount) < status.loanLimit">贷款额度未用完，还可以申请救济贷款。</template>
              <template v-else>今日已领取或条件不满足。</template>
            </p>
            <el-button type="info" disabled>明天 00:01 重置</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'BankPage',
  data() {
    return {
      loading: true,
      status: null,
      userPoints: 0,
      repayAmount: 100,
      repaying: false,
      claimingStipend: false,
      stipendAmount: 100
    }
  },
  computed: {
    maxRepay() {
      return Math.min(this.userPoints, Math.abs((this.status && this.status.loanAmount) || 0))
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      Promise.all([
        request.get('/loan/status'),
        request.get('/user/me')
      ]).then(function (results) {
        var loanData = results[0].data || results[0]
        var userData = results[1].data || results[1]
        this.status = loanData
        this.userPoints = userData.points || 0
      }.bind(this)).catch(function () {
        this.$message.error('加载失败')
      }.bind(this)).finally(function () {
        this.loading = false
      }.bind(this))
    },
    doRepay() {
      if (this.repayAmount <= 0) return
      this.repaying = true
      request.post('/loan/repay', { amount: this.repayAmount }).then(function (res) {
        var data = res.data || res
        this.$message.success('还款成功! -' + data.repaidAmount + ' 积分')
        this.repayAmount = Math.min(100, this.maxRepay)
        this.fetchData()
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '还款失败'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.repaying = false
      }.bind(this))
    },
    claimStipend() {
      this.claimingStipend = true
      request.post('/loan/stipend').then(function (res) {
        var data = res.data || res
        this.$message.success('领取成功! +' + data.stipendAmount + ' 积分')
        this.fetchData()
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '领取失败'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.claimingStipend = false
      }.bind(this))
    }
  }
}
</script>

<style scoped>
.bank-page {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

/* Loading */
.bank-loading {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.skeleton-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
  margin: 0 auto 8px;
}

.skeleton-line--val { width: 60px; height: 28px; }
.skeleton-line--lbl { width: 40px; height: 14px; }

/* Overview Cards */
.bank-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.bank-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 28px 20px;
  text-align: center;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out);
}

.bank-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
}

.bank-card-value {
  font-family: var(--font-mono);
  font-size: 32px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.bank-card-value.text-danger {
  color: #f56c6c;
}

.bank-card-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Quick Stats */
.bank-quick-stats {
  display: flex;
  gap: 24px;
  padding: 16px 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 24px;
}

.quick-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.quick-stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.quick-stat-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-primary);
}

/* Sections */
.bank-section {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
}

.section-title {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--text-primary);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}

/* Repay Form */
.repay-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.repay-input {
  width: 200px;
}

.repay-unit {
  font-size: 14px;
  color: var(--text-secondary);
}

.repay-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
}

.repay-info {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.repay-sep {
  color: var(--border-default);
}

/* Stipend */
.stipend-section {
  text-align: center;
  padding: 16px 0;
}

.stipend-desc {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 16px;
  line-height: 1.6;
}

.stipend-btn {
  background: #e6a23c !important;
  border-color: #e6a23c !important;
  color: #fff !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  font-size: 15px;
  padding: 12px 32px;
}

@media (max-width: 640px) {
  .bank-overview {
    grid-template-columns: repeat(2, 1fr);
  }
  .bank-quick-stats {
    flex-direction: column;
    gap: 8px;
  }
}
</style>
