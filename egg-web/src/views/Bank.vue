<template>
  <div class="bank-page">
    <div class="page-header">
      <h2 class="page-title">银行中心</h2>
      <p class="page-desc">资产耗尽时的安全网——积分归零后可申请贷款周转，每日还有低保兜底。更多玩法即将上线。</p>
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

      <!-- Relief Loan -->
      <div class="bank-section" v-if="status.canClaimLoan">
        <h4 class="section-title">🆘 申请救济贷款</h4>
        <div class="stipend-section">
          <p class="stipend-desc">积分不足时，可申请 <em style="color:var(--accent-primary);font-weight:700">500</em> 救济贷款。贷款会记为债务，赢注后自动还贷。</p>
          <el-button
            type="primary"
            :loading="claimingLoan"
            @click="claimLoan"
            class="repay-btn"
          >申请 500 贷款</el-button>
        </div>
      </div>

      <!-- Rules -->
      <div class="bank-section">
        <h4 class="section-title">📋 银行规则</h4>
        <div class="rules-list">
          <div class="rule-item">
            <span class="rule-icon">🆘</span>
            <div class="rule-body">
              <strong>救济贷款</strong>
              <p>下注时积分不够？自动借 <em>500</em> 分给你。贷款上限 <em>2000</em> 分，到达上限后不可再借。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-icon">💸</span>
            <div class="rule-body">
              <strong>自动还贷</strong>
              <p>押注猜中后，利润的 <em>50%</em> 自动抵扣贷款，无需手动操作。还清为止。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-icon">🎁</span>
            <div class="rule-body">
              <strong>每日低保</strong>
              <p>积分归零 + 贷款已满额时，每天可领 <em>100</em> 分（白送，不是贷款）。每天 00:01 重置。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-icon">💰</span>
            <div class="rule-body">
              <strong>手动还款</strong>
              <p>有闲余积分时可主动还贷，降低负债惩罚。还款实时生效。</p>
            </div>
          </div>
        </div>
        <div class="rule-note">
          ⚠️ 贷款余额为负数即债务。债务会降低你的<b>排名分</b>（排行榜积分，不影响净收益榜）。净收益榜只看押注输赢，贷款和低保均不计入，<b>充分体现你的真实实力</b>。
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
              <template v-else-if="status.loanAmount >= 0 && Math.abs(status.loanAmount) < status.loanLimit">贷款额度未用完，下注时积分不够会自动借给你。</template>
              <template v-else>今日已领取或条件不满足。</template>
            </p>
            <el-button type="info" disabled>明天 00:01 重置</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty/Error State -->
    <div v-else class="empty-state">
      <svg class="empty-svg" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect x="16" y="28" width="48" height="36" rx="4" stroke="#6b6b7c" stroke-width="2" fill="rgba(107,107,124,0.06)"/>
        <path d="M16 28 L40 14 L64 28" stroke="#5b8cff" stroke-width="2" fill="rgba(91,140,255,0.06)" stroke-linejoin="round"/>
        <rect x="28" y="40" width="8" height="16" rx="1" stroke="#6b6b7c" stroke-width="1" fill="rgba(107,107,124,0.08)"/>
        <rect x="44" y="40" width="8" height="16" rx="1" stroke="#6b6b7c" stroke-width="1" fill="rgba(107,107,124,0.08)"/>
        <line x1="12" y1="64" x2="68" y2="64" stroke="#6b6b7c" stroke-width="2" stroke-linecap="round" opacity="0.3"/>
        <circle cx="40" cy="22" r="3" fill="none" stroke="#00e5a0" stroke-width="1" opacity="0.4"/>
        <text x="38" y="24" font-size="6" fill="#00e5a0" opacity="0.5" font-weight="bold">$</text>
      </svg>
      <h3 class="empty-title">银行数据加载失败</h3>
      <p class="empty-desc">请先登录后再查看银行中心</p>
      <el-button type="primary" class="empty-cta" @click="$router.push('/login')">去登录</el-button>
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
      claimingLoan: false,
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
    claimLoan() {
      this.claimingLoan = true
      request.post('/loan/relief').then(function () {
        this.$message.success('借款成功! +500 积分，记得赢了还贷哦')
        this.fetchData()
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '借款失败'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.claimingLoan = false
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

.page-desc {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 8px;
  line-height: 1.6;
  max-width: 520px;
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
  0% { background-position: -200% 0; transform: scaleY(1); }
  50% { transform: scaleY(1.012); }
  100% { background-position: 200% 0; transform: scaleY(1); }
}
@keyframes skeleton-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.skeleton-card {
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
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

/* ── Rules ── */
.rules-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 16px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
}

.rule-icon {
  font-size: 20px;
  flex-shrink: 0;
  line-height: 1.4;
}

.rule-body strong {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.rule-body p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

.rule-body em {
  font-style: normal;
  font-weight: 700;
  color: var(--accent-primary);
  font-family: var(--font-mono);
}

.rule-note {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.7;
  padding: 10px 14px;
  background: rgba(91, 140, 255, 0.06);
  border-radius: var(--radius-sm);
  border-left: 3px solid #5b8cff;
}

.rule-note b {
  color: #5b8cff;
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
    /* 移动端三卡片改为单列堆叠，避免第三个卡片孤立 */
    grid-template-columns: 1fr;
  }
  .bank-quick-stats {
    flex-direction: column;
    gap: 8px;
  }
  /* 卡片内边距与字号收紧 */
  .bank-card {
    padding: 20px 16px;
  }
  .bank-card-value {
    font-size: 26px;
  }
  /* 区块内边距收紧 */
  .bank-section {
    padding: 18px 14px;
  }
  .section-title {
    font-size: 16px;
    margin-bottom: 14px;
    padding-bottom: 10px;
  }
  /* 还款表单：输入框 + 按钮改为纵向堆叠，避免横向溢出 */
  .repay-input-row {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .repay-input {
    width: 100%;
  }
  .repay-unit {
    display: none;
  }
  .repay-btn {
    width: 100%;
    min-height: 44px;
  }
  /* 还款信息允许换行 */
  .repay-info {
    flex-wrap: wrap;
    gap: 6px;
  }
  .repay-sep {
    display: none;
  }
  /* 低保/救济按钮全宽，touch-friendly */
  .stipend-btn,
  .repay-btn {
    width: 100%;
    min-height: 44px;
    padding: 12px 24px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 24px;
  }
  .bank-card-value {
    font-size: 22px;
  }
  .bank-card {
    padding: 16px 12px;
  }
  .rule-item {
    padding: 10px 12px;
    gap: 10px;
  }
  .rule-icon {
    font-size: 18px;
  }
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  max-width: 400px;
  margin: 0 auto;
}

.empty-svg {
  width: 72px;
  height: 72px;
  margin-bottom: 20px;
  opacity: 0.7;
  animation: empty-float 3s ease-in-out infinite;
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-bottom: 24px;
  max-width: 300px;
}

.empty-cta {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 28px;
  box-shadow: 0 0 20px rgba(0, 229, 160, 0.2);
  transition: all 0.2s var(--ease-out);
}

.empty-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0, 229, 160, 0.35);
}
</style>
