<template>
  <transition name="broke-fade">
    <div v-if="visible" class="broke-overlay">
      <div class="broke-modal u-panel" role="dialog" aria-modal="true" aria-label="积分耗尽">
        <!-- 头部：余额告警 -->
        <header class="broke-head">
          <div class="broke-head-top">
            <span class="u-eyebrow">BALANCE ALERT</span>
            <span class="broke-head-code">BALANCE 0</span>
          </div>
          <div class="broke-title-row">
            <div class="broke-icon-plate">
              <svg class="broke-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                <path d="M12 3 2.5 20h19L12 3Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round" />
                <path d="M12 9v5" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                <circle cx="12" cy="17" r="1" fill="currentColor" />
              </svg>
            </div>
            <div class="broke-title-copy">
              <h2 class="broke-title">积分耗尽</h2>
              <p class="broke-subtitle">余额归零，无法继续下注</p>
            </div>
          </div>
        </header>

        <!-- 主体：余额 / 债务数字 -->
        <div class="broke-body">
          <div class="broke-metrics">
            <div class="metric metric--hero">
              <span class="metric-label">当前积分</span>
              <span class="metric-value metric-value--lg">0</span>
            </div>
            <div class="metric-row" v-if="loanStatus">
              <div class="metric">
                <span class="metric-label">贷款余额</span>
                <span class="metric-value">{{ Math.abs(loanStatus.loanAmount || 0) }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">贷款上限</span>
                <span class="metric-value">{{ loanStatus.loanLimit || 2000 }}</span>
              </div>
            </div>
          </div>

          <div class="broke-tip">
            <el-icon class="tip-icon"><InfoFilled /></el-icon>
            <span>申请 <strong>500</strong> 救济贷款可立即恢复下注。贷款记为债务，赢注后自动还款。</span>
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="broke-footer">
          <template v-if="canGetLoan">
            <el-button
              type="primary"
              size="large"
              :loading="claiming"
              class="broke-btn"
              @click="claimReliefLoan"
            >
              <span class="broke-btn-inner">
                <span class="broke-btn-main">{{ claiming ? '处理中...' : '领取救济贷款' }}</span>
                <span class="broke-btn-micro">RELIEF LOAN · +500 PTS</span>
              </span>
            </el-button>
            <el-button class="broke-btn--secondary" @click="$emit('close')">
              稍后再说
            </el-button>
          </template>
          <div v-else class="broke-maxed">
            <div class="broke-maxed-head">
              <el-tag type="danger" size="medium" class="broke-limit-tag">贷款已达上限</el-tag>
            </div>
            <p class="broke-maxed-tip">
              请等待每日补贴（每天自动发放），或前往
              <router-link to="/bank" class="broke-link" @click="$emit('close')">银行中心</router-link>
              查看详情
            </p>
            <el-button class="broke-btn--secondary" @click="$emit('close')">
              我知道了
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script>
import request from '@/utils/request'
import { InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'BrokeModal',
  components: { InfoFilled },
  props: {
    visible: { type: Boolean, default: false }
  },
  data() {
    return {
      claiming: false,
      loanStatus: null,
      canGetLoan: true
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.fetchLoanStatus()
      }
    }
  },
  methods: {
    async fetchLoanStatus() {
      try {
        const res = await request.get('/loan/status')
        this.loanStatus = res.data || res
        const loanAmount = Math.abs(this.loanStatus.loanAmount || 0)
        const loanLimit = this.loanStatus.loanLimit || 2000
        this.canGetLoan = loanAmount < loanLimit
      } catch (e) {
        this.canGetLoan = true // 兜底：获取失败时允许尝试
      }
    },
    async claimReliefLoan() {
      this.claiming = true
      try {
        const res = await request.post('/loan/relief')
        const data = res.data || res
        this.$message.success('已发放 500 救济贷款，当前积分：' + (data.points || '—'))
        this.$emit('success', data)
      } catch (e) {
        const msg = (e.response && e.response.data && e.response.data.message) || e.message || '请求失败'
        this.$message.error(msg)
        // 可能是贷款上限已满，重新检查状态
        this.fetchLoanStatus()
      } finally {
        this.claiming = false
      }
    }
  }
}
</script>

<style scoped>
.broke-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: var(--el-mask-color);
  backdrop-filter: blur(5px);
  -webkit-backdrop-filter: blur(5px);
}

.broke-modal {
  width: 400px;
  max-width: 100%;
  padding: 20px;
  border-top: 2px solid var(--brand);
  animation: broke-in 0.24s var(--ease-out);
}

@keyframes broke-in {
  from { opacity: 0; transform: translateY(-10px) scale(0.985); }
  to   { opacity: 1; transform: translateY(0) scale(1); }
}

/* ===== 头部 ===== */
.broke-head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.broke-head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.broke-head-code {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  text-transform: uppercase;
}

.broke-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.broke-icon-plate {
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: var(--bg-surface-2);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
}

.broke-icon {
  width: 20px;
  height: 20px;
  color: var(--accent-danger);
}

.broke-title-copy {
  min-width: 0;
}

.broke-title {
  font-family: var(--font-body);
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--text-primary);
}

.broke-subtitle {
  margin-top: 2px;
  font-size: 12.5px;
  color: var(--text-tertiary);
  line-height: 1.45;
}

/* ===== 数字区 ===== */
.broke-body {
  padding-top: 14px;
}

.broke-metrics {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 9px 12px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  min-width: 0;
}

.metric--hero {
  border-left: 2px solid var(--accent-danger);
  padding: 10px 12px;
}

.metric-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 6px;
}

.metric-label {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  line-height: 1.4;
}

.metric-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.2;
  color: var(--text-primary);
}

.metric-value--lg {
  font-size: 30px;
  color: var(--accent-danger);
  letter-spacing: -0.03em;
}

.broke-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 10px;
  padding: 9px 12px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-secondary);
  background: var(--accent-primary-soft);
  border-left: 2px solid var(--accent-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.tip-icon {
  flex-shrink: 0;
  margin-top: 2px;
  font-size: 13px;
  color: var(--accent-primary);
}

.broke-tip strong {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  color: var(--accent-primary);
  font-weight: 700;
}

/* ===== 按钮 ===== */
.broke-footer {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.broke-btn {
  width: 100%;
  height: 48px;
  font-weight: 700;
}

.broke-btn-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  line-height: 1.1;
}

.broke-btn-main {
  font-size: 14px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.broke-btn-micro {
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.2em;
  color: var(--cream);
  opacity: 0.72;
}

.broke-btn--secondary {
  --el-button-bg-color: transparent;
  --el-button-border-color: transparent;
  --el-button-text-color: var(--text-tertiary);
  --el-button-hover-bg-color: var(--bg-surface-2);
  --el-button-hover-border-color: var(--border-subtle);
  --el-button-hover-text-color: var(--text-secondary);
  --el-button-active-bg-color: var(--bg-surface-2);
  --el-button-active-border-color: var(--border-subtle);
  --el-button-active-text-color: var(--text-secondary);
  width: 100%;
  height: 34px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

/* ===== 贷款上限 ===== */
.broke-maxed {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: center;
}

.broke-maxed-head {
  display: flex;
  justify-content: center;
}

.broke-limit-tag {
  color: var(--accent-danger) !important;
  border-color: var(--brand-line) !important;
  background: var(--brand-soft) !important;
}

.broke-maxed-tip {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.65;
}

.broke-link {
  color: var(--accent-primary);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.broke-link:hover {
  color: var(--brand);
}

/* ===== 过渡 ===== */
.broke-fade-enter-active {
  transition: opacity var(--duration-normal) ease;
}

.broke-fade-leave-active {
  transition: opacity var(--duration-fast) ease;
}

.broke-fade-enter,
.broke-fade-leave-to {
  opacity: 0;
}

@media (max-width: 480px) {
  .broke-overlay {
    padding: 12px;
  }

  .broke-modal {
    width: 100%;
    padding: 16px;
  }

  .broke-title {
    font-size: 18px;
  }

  .metric-value--lg {
    font-size: 26px;
  }

  .broke-btn {
    height: 46px;
  }
}
</style>
