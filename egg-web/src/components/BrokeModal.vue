<template>
  <transition name="broke-fade">
    <div v-if="visible" class="broke-overlay">
      <div class="broke-modal">
        <!-- 头部：警示图标 -->
        <div class="broke-header">
          <div class="broke-icon-wrapper">
            <svg class="broke-icon" viewBox="0 0 64 64" fill="none">
              <circle cx="32" cy="32" r="30" stroke="currentColor" stroke-width="2.5" fill="none" />
              <path d="M32 16v20M32 44v4" stroke="currentColor" stroke-width="3" stroke-linecap="round" />
            </svg>
          </div>
          <h2 class="broke-title">积分耗尽</h2>
          <p class="broke-subtitle">余额归零，无法继续下注</p>
        </div>

        <!-- 主体 -->
        <div class="broke-body">
          <div class="broke-info-card">
            <div class="info-row">
              <span class="info-label">当前积分</span>
              <span class="info-value text-danger">0</span>
            </div>
            <div class="info-row" v-if="loanStatus">
              <span class="info-label">贷款余额</span>
              <span class="info-value">{{ Math.abs(loanStatus.loanAmount || 0) }}</span>
            </div>
            <div class="info-row" v-if="loanStatus">
              <span class="info-label">贷款上限</span>
              <span class="info-value">{{ loanStatus.loanLimit || 2000 }}</span>
            </div>
          </div>

          <div class="broke-tip">
            <i class="el-icon-info"></i>
            申请 <strong>500</strong> 救济贷款可立即恢复下注。贷款记为债务，赢注后自动还款。
          </div>
        </div>

        <!-- 底部按钮 -->
        <div class="broke-footer">
          <el-button
            v-if="canGetLoan"
            type="primary"
            size="large"
            :loading="claiming"
            class="broke-btn"
            @click="claimReliefLoan"
          >
            {{ claiming ? '处理中...' : '领取 500 救济贷款' }}
          </el-button>
          <div v-else class="broke-maxed">
            <el-tag type="danger" size="medium">贷款已达上限</el-tag>
            <p class="broke-maxed-tip">
              请等待每日补贴（每天自动发放），或前往
              <router-link to="/bank" class="broke-link" @click.native="$emit('close')">银行中心</router-link>
              查看详情
            </p>
            <el-button size="small" class="broke-btn--secondary" @click="$emit('close')">
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

export default {
  name: 'BrokeModal',
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
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(4px);
}

.broke-modal {
  width: 420px;
  max-width: 90vw;
  background: linear-gradient(145deg, #1a1d2e, #141725);
  border: 1px solid rgba(255, 70, 70, 0.3);
  border-radius: 16px;
  padding: 36px 32px 28px;
  box-shadow:
    0 0 60px rgba(255, 70, 70, 0.15),
    0 8px 40px rgba(0, 0, 0, 0.5);
  animation: brokeSlideIn 0.3s ease-out;
}

@keyframes brokeSlideIn {
  from { transform: translateY(-20px); opacity: 0; }
  to   { transform: translateY(0);    opacity: 1; }
}

/* 头部 */
.broke-header {
  text-align: center;
  margin-bottom: 24px;
}
.broke-icon-wrapper {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 70, 70, 0.1);
  margin-bottom: 12px;
}
.broke-icon {
  width: 36px;
  height: 36px;
  color: #ff4646;
  animation: brokePulse 2s ease-in-out infinite;
}
@keyframes brokePulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.5; }
}
.broke-title {
  font-size: 20px;
  font-weight: 700;
  color: #ff4646;
  margin: 0 0 4px;
}
.broke-subtitle {
  font-size: 13px;
  color: #8892a4;
  margin: 0;
}

/* 信息卡片 */
.broke-info-card {
  background: rgba(255, 255, 255, 0.04);
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
}
.info-row + .info-row {
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}
.info-label {
  font-size: 13px;
  color: #8892a4;
}
.info-value {
  font-size: 14px;
  font-weight: 600;
  color: #e0e0e0;
}
.text-danger { color: #ff4646 !important; }

.broke-tip {
  font-size: 12px;
  color: #6b7394;
  line-height: 1.6;
  padding: 10px 14px;
  background: rgba(0, 191, 165, 0.06);
  border-left: 3px solid var(--accent-primary, #00bfa5);
  border-radius: 0 8px 8px 0;
}
.broke-tip i { margin-right: 4px; }
.broke-tip strong { color: var(--accent-primary, #00bfa5); }

/* 按钮 */
.broke-footer { text-align: center; margin-top: 8px; }
.broke-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
}

/* 贷款上限已满 */
.broke-maxed {
  text-align: center;
}
.broke-maxed-tip {
  font-size: 13px;
  color: #8892a4;
  margin: 12px 0 16px;
  line-height: 1.6;
}
.broke-link {
  color: var(--accent-primary, #00bfa5);
  text-decoration: underline;
}
.broke-btn--secondary {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #aaa;
}

/* 过渡 */
.broke-fade-enter-active { transition: opacity 0.25s ease; }
.broke-fade-leave-active { transition: opacity 0.2s ease; }
.broke-fade-enter,
.broke-fade-leave-to { opacity: 0; }
</style>
