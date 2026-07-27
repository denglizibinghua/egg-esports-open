<template>
  <!-- 押注面板：TBD 提示 / 未登录提示 / 已押注 alert / 押注表单 / 押注统计 -->
  <div class="bet-panel">
    <h3 class="panel-title">押注</h3>

    <!-- TBD Notice -->
    <div v-if="match.teamA === 'TBD' || match.teamB === 'TBD'" class="bet-tbd-notice">
      <div class="tbd-icon">⏳</div>
      <p class="tbd-text">参赛队伍尚未确定，暂不支持押注</p>
    </div>

    <!-- Not Logged In Prompt -->
    <div v-else-if="!getToken()" class="bet-login-prompt">
      <div class="prompt-icon">🔒</div>
      <p class="prompt-text">登录后即可参与竞猜</p>
      <el-button type="primary" class="prompt-btn" @click="$router.push('/login')">去登录</el-button>
    </div>

    <template v-else>
      <div v-if="myBet" class="already-bet">
        <el-alert
          :title="'你已押注 ' + (myBet.betSide === 0 ? match.teamA : match.teamB) + ' ' + myBet.betAmount + ' 积分'"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-button
          v-if="match.status === 0"
          type="warning"
          size="small"
          class="modify-bet-btn"
          @click="openModifyDialog"
        >修改押注</el-button>
      </div>

      <div v-else class="bet-form">
        <div class="bet-side-select">
          <div class="bet-side-label">选择押注队伍</div>
          <div class="bet-side-cards">
            <div
              class="team-card"
              :class="{ 'team-card--selected': betForm.betSide === 0 }"
              @click="betForm.betSide = 0"
            >
              <div class="team-card-logo">
                <el-image :src="match.teamALogo" alt="" fit="contain">
                  <template #error><span class="team-card-logo-placeholder">{{ (match.teamA || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <div class="team-card-name">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
              <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">
                {{ match.oddsA.toFixed(2) }}
                <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                  <el-icon style="font-size:14px;margin-left:4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <div v-if="betForm.betSide === 0" class="team-card-check">&#10003;</div>
            </div>
            <div class="team-card-vs">VS</div>
            <div
              class="team-card"
              :class="{ 'team-card--selected': betForm.betSide === 1 }"
              @click="betForm.betSide = 1"
            >
              <div class="team-card-logo">
                <el-image :src="match.teamBLogo" alt="" fit="contain">
                  <template #error><span class="team-card-logo-placeholder">{{ (match.teamB || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <div class="team-card-name">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
              <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">
                {{ match.oddsB.toFixed(2) }}
                <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                  <el-icon style="font-size:14px;margin-left:4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <div v-if="betForm.betSide === 1" class="team-card-check">&#10003;</div>
            </div>
          </div>
        </div>

        <div class="bet-amount-section">
          <div class="bet-amount-label">
            押注积分
            <span class="current-points">当前积分: {{ userPoints }}</span>
            <el-button class="all-in-btn" size="small" @click="betForm.betAmount = Math.max(1, userPoints)">全押</el-button>
          </div>
          <div class="slider-desktop">
            <el-slider
              v-model="betForm.betAmount"
              :min="1"
              :max="Math.max(1, userPoints)"
              :step="1"
              show-input
              :show-input-controls="false"
            />
          </div>
          <div class="amount-stepper">
            <button class="stepper-btn" @click="betForm.betAmount = Math.max(1, betForm.betAmount - 10)">−</button>
            <span class="stepper-value">{{ betForm.betAmount }}</span>
            <button class="stepper-btn" @click="betForm.betAmount = Math.min(userPoints, betForm.betAmount + 10)">+</button>
          </div>
          <div class="estimated-return" v-if="betForm.betSide !== null && betForm.betAmount > 0 && match">
            <span class="est-label">预估返还</span>
            <span class="est-amount">{{ estimatedReturn }} 积分</span>
            <span class="est-odds">(预估赔率 {{ currentSelectedOdds }})</span>
          </div>
        </div>

        <el-button
          type="primary"
          class="bet-btn"
          :loading="betLoading"
          :disabled="betForm.betSide === null || betForm.betAmount <= 0"
          @click="placeBet"
        >
          确认押注
        </el-button>
      </div>

      <!-- 押注确认弹窗 -->
      <el-dialog title="确认押注" v-model="showBetConfirmDialog" width="400px" custom-class="bet-confirm-dialog" :append-to-body="true">
        <div class="confirm-body">
          <p class="confirm-team">押注队伍：<strong>{{ betForm.betSide === 0 ? match.teamA : match.teamB }}</strong></p>
          <p class="confirm-amount">押注积分：<strong>{{ betForm.betAmount }}</strong></p>
          <p class="confirm-odds">当前赔率：<strong>{{ currentSelectedOdds }}</strong>（最终按彩池结算）</p>
          <p class="confirm-est">猜中预估回报：<strong style="color:var(--accent-primary)">{{ estimatedReturn }} 积分</strong></p>
          <p class="confirm-lose">猜错将失去：<strong style="color:var(--accent-danger)">{{ betForm.betAmount }} 积分</strong></p>
        </div>
        <template #footer><span>
          <el-button @click="showBetConfirmDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmPlaceBet">确认押注</el-button>
        </span></template>
      </el-dialog>
    </template>

    <div class="bet-stats">
      <div class="bet-stat">
        <span class="stat-label" :class="{ 'stat-label-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
        <span class="stat-value">{{ betCount.teamA }} 人押注</span>
      </div>
      <div class="bet-stat">
        <span class="stat-label" :class="{ 'stat-label-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
        <span class="stat-value">{{ betCount.teamB }} 人押注</span>
      </div>
    </div>
  </div>

  <!-- 修改押注弹窗 -->
  <el-dialog
    title="修改押注"
    v-model="showModifyBetDialog"
    width="400px"
    :close-on-click-modal="false"
    custom-class="dark-dialog"
  >
    <div class="modify-bet-form">
      <div class="bet-side-select">
        <div class="bet-side-label">选择押注队伍</div>
        <div class="bet-side-cards">
          <div
            class="team-card"
            :class="{ 'team-card--selected': modifyForm.betSide === 0 }"
            @click="modifyForm.betSide = 0"
          >
            <div class="team-card-logo">
              <el-image :src="match && match.teamALogo" alt="" fit="contain">
                <template #error><span class="team-card-logo-placeholder">{{ (match ? match.teamA : '').charAt(0) }}</span></template>
              </el-image>
            </div>
            <div class="team-card-name">{{ match && match.teamA === 'TBD' ? '待定' : (match ? match.teamA : '') }}</div>
            <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">
              {{ match.oddsA.toFixed(2) }}
              <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                <el-icon style="font-size:14px;margin-left:4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="modifyForm.betSide === 0" class="team-card-check">&#10003;</div>
          </div>
          <div class="team-card-vs">VS</div>
          <div
            class="team-card"
            :class="{ 'team-card--selected': modifyForm.betSide === 1 }"
            @click="modifyForm.betSide = 1"
          >
            <div class="team-card-logo">
              <el-image :src="match && match.teamBLogo" alt="" fit="contain">
                <template #error><span class="team-card-logo-placeholder">{{ (match ? match.teamB : '').charAt(0) }}</span></template>
              </el-image>
            </div>
            <div class="team-card-name">{{ match && match.teamB === 'TBD' ? '待定' : (match ? match.teamB : '') }}</div>
            <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">
              {{ match.oddsB.toFixed(2) }}
              <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                <el-icon style="font-size:14px;margin-left:4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="modifyForm.betSide === 1" class="team-card-check">&#10003;</div>
          </div>
        </div>
      </div>

      <div class="bet-amount-section">
        <div class="bet-amount-label">
          押注积分
          <span class="current-points">可用积分: {{ modifyMaxAmount }}</span>
          <el-button class="all-in-btn" size="small" @click="modifyForm.betAmount = Math.max(1, modifyMaxAmount)">全押</el-button>
        </div>
        <div class="slider-desktop">
          <el-slider
            v-model="modifyForm.betAmount"
            :min="1"
            :max="Math.max(1, modifyMaxAmount)"
            :step="1"
            show-input
            :show-input-controls="false"
          />
        </div>
        <div class="amount-stepper">
          <button class="stepper-btn" @click="modifyForm.betAmount = Math.max(1, modifyForm.betAmount - 10)">−</button>
          <span class="stepper-value">{{ modifyForm.betAmount }}</span>
          <button class="stepper-btn" @click="modifyForm.betAmount = Math.min(modifyMaxAmount, modifyForm.betAmount + 10)">+</button>
        </div>
        <div class="estimated-return" v-if="modifyForm.betSide !== null && modifyForm.betAmount > 0 && match">
          <span class="est-label">预估返还</span>
          <span class="est-amount">{{ modifyEstimatedReturn }} 积分</span>
          <span class="est-odds">(预估赔率 {{ modifyCurrentOdds }})</span>
        </div>
      </div>
    </div>

    <template #footer><span>
      <el-button @click="showModifyBetDialog = false">取消</el-button>
      <el-button type="primary" :loading="modifying" @click="submitModifyBet">确认修改</el-button>
    </span></template>
  </el-dialog>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
// Element Plus SVG 图标（迁移自 Element UI 字体图标）
import { QuestionFilled } from '@element-plus/icons-vue'

export default {
  name: 'BetPanel',
  // 声明向父组件触发的事件
  emits: ['place-bet', 'modify-bet', 'broke-trigger'],
  props: {
    // 比赛对象：含 teamA/teamB/oddsA/oddsB/status 等
    match: { type: Object, required: true },
    // 当前用户积分
    userPoints: { type: Number, default: 0 },
    // 当前用户在该比赛上的押注记录（无则为 null）
    myBet: { type: Object, default: null },
    // 双方押注人数统计 { teamA, teamB }
    betCount: { type: Object, default: () => ({ teamA: 0, teamB: 0 }) }
  },
  data() {
    return {
      // 暴露图标组件供模板绑定使用（markRaw 避免被响应式化）
      QuestionFilled: markRaw(QuestionFilled),
      // 押注表单
      betForm: {
        betSide: null,
        betAmount: 50
      },
      betLoading: false,
      // 修改押注弹窗
      showModifyBetDialog: false,
      modifying: false,
      modifyForm: {
        betSide: 0,
        betAmount: 0
      },
      // 押注确认弹窗
      showBetConfirmDialog: false
    }
  },
  computed: {
    // 修改押注时可用的最大积分（当前积分 + 已押注积分）
    modifyMaxAmount() {
      return this.userPoints + (this.myBet ? this.myBet.betAmount : 0)
    },
    // 当前选中队伍的赔率（字符串，保留两位小数）
    currentSelectedOdds() {
      if (this.betForm.betSide === 0) return this.match ? (this.match.oddsA || 0).toFixed(2) : '0.00'
      if (this.betForm.betSide === 1) return this.match ? (this.match.oddsB || 0).toFixed(2) : '0.00'
      return '0.00'
    },
    // 预估返还 = 押注积分 × 赔率
    estimatedReturn() {
      if (this.betForm.betSide === null || !this.match) return 0
      var odds = this.betForm.betSide === 0 ? (this.match.oddsA || 0) : (this.match.oddsB || 0)
      return Math.floor(this.betForm.betAmount * Number(odds))
    },
    // 修改弹窗中选中队伍的赔率
    modifyCurrentOdds() {
      if (this.modifyForm.betSide === 0) return this.match ? (this.match.oddsA || 0).toFixed(2) : '0.00'
      if (this.modifyForm.betSide === 1) return this.match ? (this.match.oddsB || 0).toFixed(2) : '0.00'
      return '0.00'
    },
    // 修改弹窗中的预估返还
    modifyEstimatedReturn() {
      if (this.modifyForm.betSide === null || !this.match) return 0
      var odds = this.modifyForm.betSide === 0 ? (this.match.oddsA || 0) : (this.match.oddsB || 0)
      return Math.floor(this.modifyForm.betAmount * Number(odds))
    }
  },
  watch: {
    // 打开修改弹窗时，用当前 myBet 初始化表单
    showModifyBetDialog(val) {
      if (val && this.myBet) {
        this.modifyForm.betSide = this.myBet.betSide
        this.modifyForm.betAmount = this.myBet.betAmount
      }
    }
  },
  methods: {
    // 暴露给模板判断登录态
    getToken() {
      return getToken()
    },
    // 点击"确认押注"按钮：校验后打开确认弹窗
    placeBet() {
      if (this.betForm.betSide === null) {
        this.$message.warning('请选择押注队伍')
        return
      }
      if (this.betForm.betAmount <= 0) {
        this.$message.warning('请输入押注积分')
        return
      }
      // 打开确认弹窗，不直接提交
      this.showBetConfirmDialog = true
    },
    // 确认弹窗中点击"确认押注"：真正调用押注接口
    confirmPlaceBet() {
      this.showBetConfirmDialog = false
      this.betLoading = true
      request.post('/bet/place', {
        matchId: this.match.id,
        betSide: this.betForm.betSide,
        betAmount: this.betForm.betAmount
      }).then(function () {
        this.$message.success('押注成功! 赔率以结算时最终奖池为准')
        // 通知父组件刷新比赛、我的押注、押注计数、用户积分
        this.$emit('place-bet')
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || ''
        // 积分不足相关错误 → 通知父组件弹出救济贷款弹窗
        if (msg.indexOf('积分不足') !== -1 || msg.indexOf('贷款') !== -1) {
          this.$emit('broke-trigger')
        } else if (msg) {
          this.$message.error(msg)
        }
      }.bind(this)).finally(function () {
        this.betLoading = false
      }.bind(this))
    },
    // 打开修改押注弹窗
    openModifyDialog() {
      if (this.myBet) {
        this.modifyForm.betSide = this.myBet.betSide
        this.modifyForm.betAmount = this.myBet.betAmount
      }
      this.showModifyBetDialog = true
    },
    // 提交修改押注
    submitModifyBet() {
      if (this.modifyForm.betSide === null || this.modifyForm.betSide === undefined) {
        this.$message.warning('请选择押注队伍')
        return
      }
      if (this.modifyForm.betAmount <= 0) {
        this.$message.warning('请输入押注积分')
        return
      }
      if (this.modifyForm.betSide === this.myBet.betSide && this.modifyForm.betAmount === this.myBet.betAmount) {
        this.$message.info('未修改任何内容')
        return
      }
      this.modifying = true
      request.put('/bet/' + this.myBet.id, {
        betSide: this.modifyForm.betSide,
        betAmount: this.modifyForm.betAmount
      }).then(function () {
        this.$message.success('修改成功')
        this.showModifyBetDialog = false
        // 通知父组件刷新我的押注、押注计数、用户积分
        this.$emit('modify-bet')
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '修改失败，请重试'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.modifying = false
      }.bind(this))
    }
  }
}
</script>

<style scoped>
/* Betting Panel - Glass Effect */
.bet-panel {
  background: rgba(18,18,26,0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  padding: 28px;
  margin-top: 24px;
  position: relative;
  overflow: hidden;
}

.panel-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 1px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 12px;
}

/* 已押注提示：横向布局 */
.already-bet {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.already-bet .el-alert {
  flex: 1;
}

.bet-form {
  margin-bottom: 20px;
}

.bet-side-select {
  margin-bottom: 24px;
}

.bet-side-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
}

.bet-side-cards {
  display: flex;
  align-items: stretch;
  gap: 12px;
}

.team-card {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-height: 120px;
  padding: 20px 16px;
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) ease;
  user-select: none;
}

.team-card:hover {
  background: #1f1f2c;
  border-color: #3a3a4a;
}

.team-card--selected {
  background: linear-gradient(135deg, rgba(0,229,160,0.08), transparent);
  border-color: #3d5a40;
  box-shadow: 0 0 24px rgba(0,229,160,0.15);
}

.team-card--selected::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  border: 2px solid rgba(0,229,160,0.4);
  animation: selected-border-pulse 2s ease-in-out infinite;
  pointer-events: none;
}

@keyframes selected-border-pulse {
  0%, 100% { border-color: rgba(0,229,160,0.4); }
  50% { border-color: rgba(0,229,160,0.8); }
}

.team-card--selected:hover {
  background: linear-gradient(135deg, rgba(0,229,160,0.08), transparent);
  border-color: #3d5a40;
}

.team-card-logo {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: #252533;
  border: 1px solid #2a2a3a;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.team-card-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-card-logo-placeholder {
  font-size: 20px;
  font-weight: 700;
  color: #5b8c5a;
}

.team-card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.3;
}

.team-card-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: var(--accent-primary);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  box-shadow: 0 0 8px rgba(0,229,160,0.4);
}

.team-card-vs {
  width: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  color: #4a4a5a;
  font-weight: 600;
  letter-spacing: 1px;
}

.bet-amount-section {
  margin-bottom: 24px;
}

.bet-amount-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
}

.current-points {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-weight: 600;
  margin-left: 8px;
}

.bet-btn {
  width: 100%;
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 20px rgba(0,229,160,0.20);
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.bet-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.2), transparent);
  transition: left 0.5s ease;
}

.bet-btn:hover {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0,229,160,0.35);
}

.bet-btn:hover::after {
  left: 100%;
}

.bet-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}

.bet-stat {
  text-align: center;
}

.stat-label {
  display: block;
  color: var(--text-primary);
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}

.stat-value {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
}

.stat-label-tbd {
  color: var(--text-tertiary) !important;
  font-style: italic;
}

/* Bet Login Prompt */
.bet-login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
}

.prompt-icon {
  font-size: 36px;
  margin-bottom: 12px;
  opacity: 0.7;
}

.prompt-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.prompt-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 32px;
}

/* TBD Notice */
.bet-tbd-notice {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.tbd-icon {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.tbd-text {
  font-size: 15px;
  color: var(--text-tertiary);
  margin: 0;
}

/* Modify Bet Button */
.modify-bet-btn {
  flex-shrink: 0;
  background: transparent !important;
  border: 1px solid #e6a23c !important;
  color: #e6a23c !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  transition: all var(--duration-fast) ease;
}

.modify-bet-btn:hover {
  background: rgba(230, 162, 60, 0.12) !important;
  border-color: #e6a23c !important;
  color: #e6a23c !important;
}

/* Dark Dialog */
:deep(.dark-dialog) {
  background: #12121a;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6);
}

:deep(.dark-dialog .el-dialog__header) {
  border-bottom: 1px solid var(--border-subtle);
  padding: 16px 20px;
}

:deep(.dark-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 16px;
}

:deep(.dark-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-tertiary);
}

:deep(.dark-dialog .el-dialog__body) {
  padding: 20px;
}

:deep(.dark-dialog .el-dialog__footer) {
  border-top: 1px solid var(--border-subtle);
  padding: 12px 20px;
}

.modify-bet-form .bet-side-select {
  margin-bottom: 20px;
}

.modify-bet-form .bet-amount-section {
  margin-bottom: 0;
}

/* Team Card Odds */
.team-card-odds {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-primary);
  margin-top: 4px;
  letter-spacing: 0.5px;
  text-shadow: 0 0 12px rgba(0,229,160,0.3);
}

/* Estimated Return */
.estimated-return {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(0, 229, 160, 0.06);
  border: 1px solid rgba(0, 229, 160, 0.15);
  border-radius: var(--radius-md);
  margin-top: 12px;
}

.est-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.est-amount {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-primary);
}

.est-odds {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* 押注确认弹窗 */
.confirm-body p {
  margin: 10px 0;
  font-size: 15px;
  color: var(--text-primary);
}

.confirm-body strong {
  font-weight: 700;
}

.confirm-body .confirm-est strong {
  color: var(--accent-primary);
}

.confirm-body .confirm-lose strong {
  color: var(--accent-danger);
}

/* Gradient Slider */
:deep(.el-slider__bar) {
  background: linear-gradient(90deg, var(--bg-surface-3), var(--accent-primary)) !important;
  height: 6px !important;
}
:deep(.el-slider__runway) {
  background: var(--bg-surface-3) !important;
  height: 6px !important;
}
:deep(.el-slider__button) {
  border-color: var(--accent-primary) !important;
  width: 18px;
  height: 18px;
  box-shadow: 0 0 8px rgba(0,229,160,0.3);
  transition: transform 0.15s ease;
}
:deep(.el-slider__button:hover) {
  transform: scale(1.15);
}

/* All-in Button */
.all-in-btn {
  background: transparent !important;
  border: 1px solid var(--accent-primary) !important;
  color: var(--accent-primary) !important;
  font-size: 12px !important;
  padding: 2px 10px !important;
  border-radius: var(--radius-sm) !important;
  transition: all 0.15s ease;
  margin-left: 8px;
  line-height: 1.4;
}
.all-in-btn:hover {
  background: rgba(0,229,160,0.1) !important;
  transform: scale(1.05);
}

/* Confirm Dialog Animation */
:deep(.bet-confirm-dialog) {
  animation: dialog-pop 0.3s ease-out;
}
@keyframes dialog-pop {
  from { opacity: 0; transform: scale(0.92) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.confirm-body {
  background: rgba(0,0,0,0.15);
  padding: 16px;
  border-radius: var(--radius-md);
}

.confirm-body .el-button--primary {
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
}
.confirm-body .el-button--primary:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0,229,160,0.3);
}

/* ===== 移动端适配 ===== */

/* Slider → stepper responsive */
.slider-desktop {
  display: block;
}
.amount-stepper {
  display: none;
}

@media (max-width: 768px) {
  /* 押注面板：缩小内边距 */
  .bet-panel {
    padding: 18px 14px;
    margin-top: 16px;
  }
  .panel-title {
    font-size: 18px;
    margin-bottom: 14px;
    padding-bottom: 10px;
  }

  /* 押注队伍卡片：缩小内边距，VS 间距收紧 */
  .bet-side-cards {
    gap: 8px;
  }
  .team-card {
    min-height: 96px;
    padding: 14px 10px;
    gap: 6px;
  }
  .team-card-logo {
    width: 40px;
    height: 40px;
  }
  .team-card-logo-placeholder {
    font-size: 16px;
  }
  .team-card-name {
    font-size: 13px;
  }
  .team-card-odds {
    font-size: 15px;
  }
  .team-card-vs {
    width: 22px;
    font-size: 10px;
  }

  /* 押注积分区 */
  .bet-amount-label {
    font-size: 12px;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }
  .current-points {
    font-size: 12px;
    margin-left: 0;
    width: 100%;
  }
  /* 预估返还：允许换行 */
  .estimated-return {
    flex-wrap: wrap;
    gap: 6px;
    padding: 10px 12px;
  }
  .est-amount {
    font-size: 18px;
  }

  /* 确认押注按钮：touch-friendly */
  .bet-btn {
    min-height: 44px;
    font-size: 15px;
  }

  /* 押注统计：紧凑 */
  .bet-stats {
    padding-top: 14px;
    gap: 12px;
  }
  .stat-label {
    font-size: 13px;
  }
  .stat-value {
    font-size: 12px;
  }

  /* 已押注提示：改为纵向堆叠 */
  .already-bet {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .modify-bet-btn {
    width: 100%;
  }

  /* 未登录提示按钮：全宽 */
  .bet-login-prompt {
    padding: 24px 16px;
  }
  .prompt-btn {
    width: 100%;
    min-height: 44px;
  }
}

/* 小屏手机：slider 替换为 stepper 按钮 */
@media (max-width: 480px) {
  .slider-desktop {
    display: none;
  }
  .amount-stepper {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 8px 0;
  }
  .stepper-btn {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    border: 1px solid var(--accent-primary);
    background: transparent;
    color: var(--accent-primary);
    font-size: 22px;
    font-weight: 700;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.15s ease;
  }
  .stepper-btn:hover {
    background: rgba(0,229,160,0.1);
  }
  .stepper-value {
    font-family: var(--font-mono);
    font-size: 28px;
    font-weight: 700;
    color: var(--text-primary);
    min-width: 60px;
    text-align: center;
  }

  /* 队伍卡片名称占满，避免溢出 */
  .team-card-name {
    font-size: 12px;
    word-break: break-word;
    white-space: normal;
  }
}
</style>
