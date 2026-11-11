<template>
  <div class="bank-page">
    <header class="page-header">
      <div class="header-main">
        <span class="page-eyebrow">// BANK · 资本中心</span>
        <h1 class="page-title">银行中心</h1>
        <p class="page-desc">资产耗尽时的安全网——积分归零后可申请贷款周转，每日还有低保兜底。更多玩法即将上线。</p>
      </div>
      <div class="header-tag">
        <span class="tag-dot"></span>
        <span>CREDIT DESK</span>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="bank-loading">
      <div class="skeleton-hero">
        <div class="skeleton-line skeleton-line--eyebrow"></div>
        <div class="skeleton-line skeleton-line--value"></div>
        <div class="skeleton-line skeleton-line--meta"></div>
      </div>
      <div class="skeleton-grid">
        <div v-for="i in 3" :key="'skel-' + i" class="skeleton-card">
          <div class="skeleton-line skeleton-line--val"></div>
          <div class="skeleton-line skeleton-line--lbl"></div>
        </div>
      </div>
    </div>

    <div v-else-if="status" class="bank-content">
      <!-- Balance hero + debt status -->
      <section class="balance-hero u-panel card-entrance">
        <div class="balance-main">
          <span class="balance-label">积分余额 <em>BALANCE</em></span>
          <div class="balance-value">{{ userPoints }}</div>
          <div class="balance-foot">
            <span class="foot-item">排行榜积分 <b>{{ status.rankingScore }}</b></span>
            <span class="foot-item">押注中 <b>{{ status.activeBets || 0 }}</b></span>
          </div>
        </div>

        <div class="debt-panel" :class="{ 'debt-panel--active': status.loanAmount < 0 }">
          <div class="debt-head">
            <span class="debt-label">贷款余额 <em>LOAN</em></span>
            <span
              class="status-badge"
              :class="status.loanAmount < 0 ? 'status-badge--lost' : 'status-badge--won'"
            >{{ status.loanAmount < 0 ? '负债中' : '无负债' }}</span>
          </div>
          <div class="debt-value" :class="{ 'is-debt': status.loanAmount < 0 }">
            {{ status.loanAmount || 0 }}
          </div>
          <div class="usage">
            <div class="usage-meta">
              <span>额度占用</span>
              <span class="usage-pct">{{ debtUsage }}%</span>
            </div>
            <div class="usage-track">
              <div class="usage-fill" :style="{ width: debtUsage + '%' }"></div>
            </div>
            <div class="usage-legs">
              <span>已用 {{ Math.abs(status.loanAmount || 0) }}</span>
              <span>上限 {{ status.loanLimit }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- Action cards: relief loan / daily stipend -->
      <div class="action-grid">
        <section v-if="status.canClaimLoan" class="action-card action-card--loan u-panel card-entrance">
          <div class="action-head">
            <span class="action-index">01</span>
            <h2 class="action-title">申请救济贷款</h2>
            <span class="action-tag">RELIEF</span>
          </div>
          <p class="action-desc">积分不足时，可申请 <em>500</em> 救济贷款。贷款会记为债务，赢注后自动还贷。</p>
          <div class="action-foot">
            <span class="action-meta">LOAN · +500</span>
            <el-button
              type="primary"
              :loading="claimingLoan"
              @click="claimLoan"
              class="action-btn"
            >申请 500 贷款</el-button>
          </div>
        </section>

        <section class="action-card action-card--stipend u-panel card-entrance">
          <div class="action-head">
            <span class="action-index">02</span>
            <h2 class="action-title">每日低保</h2>
            <span class="action-tag">STIPEND</span>
          </div>

          <div v-if="status.canClaimStipend" class="stipend-available">
            <p class="action-desc">你的积分已耗尽且贷款已达上限，可领取每日低保。</p>
            <div class="action-foot">
              <span class="action-meta">DAILY · RESET 00:01</span>
              <el-button
                type="warning"
                :loading="claimingStipend"
                @click="claimStipend"
                class="action-btn"
              >领取 {{ stipendAmount }} 积分</el-button>
            </div>
          </div>

          <div v-else class="stipend-unavailable">
            <p class="action-desc">
              <template v-if="userPoints > 0">你还有可用积分，无需领取低保。</template>
              <template v-else-if="status.loanAmount >= 0 && Math.abs(status.loanAmount) < status.loanLimit">贷款额度未用完，下注时积分不够会自动借给你。</template>
              <template v-else>今日已领取或条件不满足。</template>
            </p>
            <div class="action-foot">
              <span class="action-meta">DAILY · RESET 00:01</span>
              <el-button type="info" disabled>明天 00:01 重置</el-button>
            </div>
          </div>
        </section>
      </div>

      <!-- Manual repay -->
      <section v-if="status.loanAmount < 0" class="bank-panel repay-panel u-panel card-entrance">
        <div class="panel-head">
          <span class="action-index">03</span>
          <h2 class="action-title">主动还款</h2>
          <span class="panel-tag">REPAY</span>
        </div>
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
            <span>当前债务: <b>{{ Math.abs(status.loanAmount) }}</b></span>
            <span class="repay-sep">|</span>
            <span>还清需: <b>{{ Math.abs(status.loanAmount) }}</b></span>
          </div>
        </div>
      </section>

      <!-- Rules -->
      <section class="bank-panel rules-panel u-panel card-entrance">
        <div class="panel-head">
          <span class="action-index">04</span>
          <h2 class="action-title">银行规则</h2>
          <span class="panel-tag">PROTOCOL</span>
        </div>

        <div class="rules-list">
          <div class="rule-item">
            <span class="rule-index">R-01</span>
            <div class="rule-body">
              <strong>救济贷款</strong>
              <p>下注时积分不够？自动借 <em>500</em> 分给你。贷款上限 <em>2000</em> 分，到达上限后不可再借。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-index">R-02</span>
            <div class="rule-body">
              <strong>自动还贷</strong>
              <p>押注猜中后，利润的 <em>50%</em> 自动抵扣贷款，无需手动操作。还清为止。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-index">R-03</span>
            <div class="rule-body">
              <strong>每日低保</strong>
              <p>积分归零 + 贷款已满额时，每天可领 <em>100</em> 分（白送，不是贷款）。每天 00:01 重置。</p>
            </div>
          </div>
          <div class="rule-item">
            <span class="rule-index">R-04</span>
            <div class="rule-body">
              <strong>手动还款</strong>
              <p>有闲余积分时可主动还贷，降低负债惩罚。还款实时生效。</p>
            </div>
          </div>
        </div>

        <div class="rule-note">
          <span class="note-flag">NOTE</span>
          <p>贷款余额为负数即债务。债务会降低你的<b>排名分</b>（排行榜积分，不影响净收益榜）。净收益榜只看押注输赢，贷款和低保均不计入，<b>充分体现你的真实实力</b>。</p>
        </div>
      </section>
    </div>

    <!-- Empty/Error State -->
    <div v-else class="empty-state">
      <div class="empty-mark">//</div>
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
    },
    debtUsage() {
      const limit = Number((this.status && this.status.loanLimit) || 0)
      if (!limit) return 0
      const used = Math.abs((this.status && this.status.loanAmount) || 0)
      return Math.min(100, Math.round((used / limit) * 100))
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
      }.bind(this)).catch((error) => {
        // 区分 401 未登录与其他错误，提供差异化提示
        if (error.response && error.response.status === 401) {
          this.$message.error('请先登录')
          this.$router.push('/login')
        } else {
          this.$message.error('加载失败，请刷新重试')
        }
      }).finally(function () {
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
  max-width: 940px;
  margin: 0 auto;
}

/* ---------- Header ---------- */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 18px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-subtle);
  position: relative;
}

.page-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
}

.page-eyebrow {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--text-primary);
}

.page-desc {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 8px;
  line-height: 1.7;
  max-width: 560px;
}

.header-tag {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  flex-shrink: 0;
  padding: 4px 9px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.tag-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent-primary);
  box-shadow: 0 0 8px var(--accent-primary);
}

/* ---------- Loading ---------- */
.bank-loading {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-hero,
.skeleton-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.skeleton-hero {
  min-height: 186px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 14px;
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.skeleton-card {
  min-height: 108px;
  text-align: center;
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton-line {
  background: linear-gradient(
    90deg,
    var(--bg-surface-2) 0%,
    var(--bg-surface-3) 40%,
    var(--bg-surface-2) 80%
  );
  background-size: 200% 100%;
  animation: shimmer 1.6s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-card .skeleton-line {
  margin: 0 auto 8px;
}

.skeleton-line--eyebrow { width: 96px; height: 10px; }
.skeleton-line--value { width: 190px; height: 46px; }
.skeleton-line--meta { width: 240px; height: 12px; max-width: 100%; }
.skeleton-line--val { width: 64px; height: 26px; }
.skeleton-line--lbl { width: 48px; height: 12px; }

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes skeleton-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* ---------- Balance hero ---------- */
.balance-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(0, 0.88fr);
  gap: 26px;
  padding: 26px 26px 24px;
  margin-bottom: 16px;
}

.balance-main {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}

.balance-label,
.debt-label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.balance-label em,
.debt-label em {
  font-style: normal;
  color: var(--brand);
  margin-left: 8px;
}

.balance-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: clamp(46px, 7vw, 64px);
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.03em;
  color: var(--accent-primary);
  margin: 14px 0 16px;
  overflow-wrap: anywhere;
}

.balance-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.foot-item b {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: var(--text-secondary);
  margin-left: 4px;
}

/* Debt panel */
.debt-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px 15px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  background: var(--bg-inset);
  min-width: 0;
  transition: border-color var(--duration-normal) var(--ease-out);
}

.debt-panel--active {
  border-color: color-mix(in srgb, var(--accent-danger) 38%, transparent);
  background: var(--bg-inset);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent-danger) 7%, transparent), transparent 72%),
    var(--bg-inset);
}

.debt-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.debt-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 30px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-primary);
}

.debt-value.is-debt {
  color: var(--accent-danger);
}

.usage {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.usage-meta,
.usage-legs {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.05em;
  color: var(--text-faint);
  font-variant-numeric: tabular-nums;
}

.usage-pct {
  color: var(--accent-warning);
  font-weight: 700;
}

.usage-track {
  height: 4px;
  border-radius: 99px;
  background: var(--bg-surface-3);
  overflow: hidden;
}

.usage-fill {
  height: 100%;
  border-radius: 99px;
  background: linear-gradient(90deg, var(--accent-warning), var(--accent-danger));
  transition: width var(--duration-normal) var(--ease-out);
}

/* ---------- Action cards ---------- */
.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.action-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 22px 24px;
  min-width: 0;
}

.action-card--stipend {
  background: var(--bg-surface-1);
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent-warning) 5%, transparent) 0%, transparent 52%),
    var(--bg-surface-1);
}

.action-head {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.action-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
  flex-shrink: 0;
}

.action-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  line-height: 1.3;
}

.action-tag {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-faint);
  white-space: nowrap;
}

.action-desc {
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.action-desc em {
  font-style: normal;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-weight: 700;
  color: var(--accent-primary);
}

.action-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 4px;
}

.action-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--text-faint);
  white-space: nowrap;
}

.action-btn {
  min-width: 136px;
}

/* ---------- Panels ---------- */
.bank-panel {
  padding: 22px 24px;
  margin-bottom: 16px;
}

.panel-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  margin-bottom: 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.panel-tag {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--text-faint);
  white-space: nowrap;
}

/* Repay */
.repay-input-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.repay-input {
  width: 200px;
  flex-shrink: 0;
}

.repay-input :deep(.el-input__wrapper) {
  min-height: 40px;
}

.repay-input :deep(.el-input__inner) {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.repay-unit {
  font-size: 13px;
  color: var(--text-tertiary);
}

.repay-info {
  display: flex;
  gap: 10px;
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
  flex-wrap: wrap;
}

.repay-info b {
  color: var(--text-primary);
  font-weight: 700;
}

.repay-sep {
  color: var(--text-faint);
}

.repay-btn {
  min-width: 128px;
}

/* Rules */
.rules-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.rule-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px 15px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  min-width: 0;
}

.rule-index {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
  padding-top: 3px;
  flex-shrink: 0;
}

.rule-body {
  min-width: 0;
}

.rule-body strong {
  display: block;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.rule-body p {
  font-size: 12.5px;
  color: var(--text-tertiary);
  line-height: 1.7;
  margin: 0;
}

.rule-body em {
  font-style: normal;
  font-weight: 700;
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.rule-note {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.75;
  padding: 12px 14px;
  background: color-mix(in srgb, var(--accent-warning) 5%, transparent);
  border-radius: var(--radius-sm);
  border-left: 2px solid var(--accent-warning);
}

.note-flag {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  color: var(--accent-warning);
  padding-top: 2px;
  flex-shrink: 0;
}

.rule-note b {
  color: var(--accent-warning);
  font-weight: 700;
}

/* ---------- Empty state ---------- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 24px;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
  background: var(--bg-surface-1);
}

.empty-mark {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: 20px;
  max-width: 320px;
  line-height: 1.7;
}

.empty-cta {
  min-width: 132px;
}

/* ---------- Responsive ---------- */
@media (max-width: 720px) {
  .balance-hero {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .debt-panel {
    padding: 14px 15px;
  }

  .skeleton-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .page-title {
    font-size: 26px;
  }

  .header-tag {
    align-self: flex-start;
  }

  .balance-hero {
    padding: 20px 18px;
  }

  .balance-value {
    font-size: 44px;
  }

  .action-card {
    padding: 18px 16px;
  }

  .action-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }

  .bank-panel {
    padding: 18px 16px;
  }

  .panel-head {
    flex-wrap: wrap;
  }

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

  .repay-sep {
    display: none;
  }

  .rules-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 400px) {
  .balance-value {
    font-size: 40px;
  }

  .debt-value {
    font-size: 26px;
  }

  .page-desc {
    font-size: 12.5px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .card-entrance {
    animation: none;
  }
}
</style>
