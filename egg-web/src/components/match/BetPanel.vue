<template>
  <!-- 押注面板：WAGER 终端（TBD / 未登录 / 已押注 / 押注表单 / 押注统计） -->
  <div class="bet-panel u-panel card-entrance">
    <header class="bet-head">
      <div class="bet-head-top">
        <span class="u-eyebrow">WAGER</span>
        <span class="bet-head-meta">0% 抽水 · 彩池结算</span>
      </div>
      <h3 class="bet-title">押注</h3>
    </header>

    <!-- TBD Notice -->
    <div v-if="match.teamA === 'TBD' || match.teamB === 'TBD'" class="state-panel state-panel--tbd">
      <span class="state-code">HOLD</span>
      <div class="state-copy">
        <p class="state-title">参赛队伍尚未确定</p>
        <p class="state-desc">暂不支持押注，请等待对阵公布</p>
      </div>
    </div>

    <!-- Not Logged In Prompt — 放开浏览，押注时再转化 -->
    <div v-else-if="!getToken()" class="state-panel state-panel--login">
      <div class="state-head">
        <span class="state-code">AUTH</span>
        <p class="state-title">登录后即可参与竞猜</p>
      </div>
      <p class="state-desc">先浏览赛程与赔率，已有 32 场演示案例可查看</p>
      <div class="prompt-actions">
        <el-button type="primary" class="prompt-btn" @click="$router.push({ name: 'Login', query: { redirect: $route.fullPath } })">去登录</el-button>
        <el-button class="prompt-btn--ghost" @click="$router.push('/showcase')">查看案例</el-button>
      </div>
      <p class="prompt-hint">演示账号：demo_player / demo123 一键体验</p>
    </div>

    <template v-else>
      <!-- 比赛已开始/已结束，不可下注 -->
      <div v-if="match.status !== 0" class="state-panel state-panel--closed">
        <span class="state-code">LOCKED</span>
        <div class="state-copy">
          <p class="state-title">{{ match.status === 1 ? '比赛已开始，不可下注' : '比赛已结束，不可下注' }}</p>
          <p class="state-desc">本场押注通道已关闭</p>
        </div>
      </div>

      <div v-else-if="myBet" class="already-bet">
        <div class="ab-head">
          <span class="ab-eyebrow">POSITION OPEN</span>
          <span class="ab-status"><span class="status-dot"></span>待结算</span>
        </div>
        <div class="ab-body">
          <div class="ab-summary">
            <span class="ab-caption">你已押注</span>
            <span class="ab-team">{{ myBet.betSide === 0 ? (match.teamA === 'TBD' ? '待定' : match.teamA) : (match.teamB === 'TBD' ? '待定' : match.teamB) }}</span>
            <span class="ab-amount">{{ myBet.betAmount }} <em>积分</em></span>
          </div>
          <el-button
            v-if="match.status === 0"
            class="modify-bet-btn"
            @click="openModifyDialog"
          >修改押注</el-button>
        </div>
      </div>

      <div v-else class="bet-form">
        <div class="bet-side-select">
          <div class="field-head">
            <span class="field-label">选择押注队伍</span>
            <span class="field-hint">PICK SIDE</span>
          </div>
          <div class="bet-side-cards">
            <div
              class="team-card"
              :class="{ 'team-card--selected': betForm.betSide === 0 }"
              @click="betForm.betSide = 0"
            >
              <span class="team-card-slot">A</span>
              <div class="team-card-logo">
                <el-image :src="fixLogo(match.teamALogo)" alt="" fit="contain">
                  <template #error><span class="team-card-logo-placeholder">{{ (match.teamA || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <div class="team-card-name">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
              <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">
                <span class="odds-value">{{ match.oddsA.toFixed(2) }}</span>
                <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                  <el-icon class="odds-help"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <div v-if="betForm.betSide === 0" class="team-card-check">&#10003;</div>
            </div>
            <div class="team-card-vs"><span>VS</span></div>
            <div
              class="team-card"
              :class="{ 'team-card--selected': betForm.betSide === 1 }"
              @click="betForm.betSide = 1"
            >
              <span class="team-card-slot">B</span>
              <div class="team-card-logo">
                <el-image :src="fixLogo(match.teamBLogo)" alt="" fit="contain">
                  <template #error><span class="team-card-logo-placeholder">{{ (match.teamB || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <div class="team-card-name">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
              <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">
                <span class="odds-value">{{ match.oddsB.toFixed(2) }}</span>
                <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                  <el-icon class="odds-help"><QuestionFilled /></el-icon>
                </el-tooltip>
              </div>
              <div v-if="betForm.betSide === 1" class="team-card-check">&#10003;</div>
            </div>
          </div>
        </div>

        <div class="bet-amount-section">
          <div class="bet-amount-label">
            <span class="amount-title">押注积分</span>
            <span class="current-points">当前积分<b>{{ userPoints }}</b></span>
            <el-button class="all-in-btn" size="small" @click="betForm.betAmount = Math.max(1, userPoints)">ALL IN</el-button>
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
            <div class="est-main">
              <span class="est-label">预估返还</span>
              <span class="est-amount">{{ estimatedReturn }}</span>
              <span class="est-unit">积分</span>
            </div>
            <span class="est-odds">预估赔率 {{ currentSelectedOdds }}</span>
          </div>
        </div>

        <el-button
          type="primary"
          class="bet-btn"
          :loading="betLoading"
          :disabled="betForm.betSide === null || betForm.betAmount <= 0"
          @click="placeBet"
        >
          <span class="cta-inner">
            <span class="cta-main">确认押注</span>
            <span class="cta-micro">CONFIRM WAGER</span>
          </span>
        </el-button>
      </div>

      <!-- 押注确认弹窗 -->
      <el-dialog title="确认押注" v-model="showBetConfirmDialog" width="400px" custom-class="bet-confirm-dialog" :append-to-body="true">
        <div class="confirm-body">
          <div class="confirm-row">
            <span class="confirm-label">押注队伍</span>
            <span class="confirm-value confirm-value--team">{{ betForm.betSide === 0 ? match.teamA : match.teamB }}</span>
          </div>
          <div class="confirm-row">
            <span class="confirm-label">押注积分</span>
            <span class="confirm-value u-num">{{ betForm.betAmount }}</span>
          </div>
          <div class="confirm-row">
            <span class="confirm-label">当前赔率</span>
            <span class="confirm-value confirm-value--odds">{{ currentSelectedOdds }}</span>
          </div>
          <p class="confirm-note">最终按彩池结算（Parimutuel）</p>
          <div class="confirm-outcomes">
            <div class="outcome outcome--win">
              <span class="outcome-label">猜中预估回报</span>
              <span class="outcome-value"><strong>{{ estimatedReturn }}</strong> 积分</span>
            </div>
            <div class="outcome outcome--lose">
              <span class="outcome-label">猜错将失去</span>
              <span class="outcome-value"><strong>{{ betForm.betAmount }}</strong> 积分</span>
            </div>
          </div>
        </div>
        <template #footer><span>
          <el-button @click="showBetConfirmDialog = false">取消</el-button>
          <el-button type="primary" @click="confirmPlaceBet">确认押注</el-button>
        </span></template>
      </el-dialog>
    </template>

    <div class="bet-stats">
      <div class="bet-stat">
        <span class="stat-side">A</span>
        <div class="stat-copy">
          <span class="stat-label" :class="{ 'stat-label-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
          <span class="stat-value"><b>{{ betCount.teamA }}</b> 人押注</span>
        </div>
      </div>
      <div class="bet-stat">
        <span class="stat-side">B</span>
        <div class="stat-copy">
          <span class="stat-label" :class="{ 'stat-label-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
          <span class="stat-value"><b>{{ betCount.teamB }}</b> 人押注</span>
        </div>
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
        <div class="field-head">
          <span class="field-label">选择押注队伍</span>
          <span class="field-hint">PICK SIDE</span>
        </div>
        <div class="bet-side-cards">
          <div
            class="team-card"
            :class="{ 'team-card--selected': modifyForm.betSide === 0 }"
            @click="modifyForm.betSide = 0"
          >
            <span class="team-card-slot">A</span>
            <div class="team-card-logo">
              <el-image :src="fixLogo(match && match.teamALogo)" alt="" fit="contain">
                <template #error><span class="team-card-logo-placeholder">{{ (match ? match.teamA : '').charAt(0) }}</span></template>
              </el-image>
            </div>
            <div class="team-card-name">{{ match && match.teamA === 'TBD' ? '待定' : (match ? match.teamA : '') }}</div>
            <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">
              <span class="odds-value">{{ match.oddsA.toFixed(2) }}</span>
              <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                <el-icon class="odds-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="modifyForm.betSide === 0" class="team-card-check">&#10003;</div>
          </div>
          <div class="team-card-vs"><span>VS</span></div>
          <div
            class="team-card"
            :class="{ 'team-card--selected': modifyForm.betSide === 1 }"
            @click="modifyForm.betSide = 1"
          >
            <span class="team-card-slot">B</span>
            <div class="team-card-logo">
              <el-image :src="fixLogo(match && match.teamBLogo)" alt="" fit="contain">
                <template #error><span class="team-card-logo-placeholder">{{ (match ? match.teamB : '').charAt(0) }}</span></template>
              </el-image>
            </div>
            <div class="team-card-name">{{ match && match.teamB === 'TBD' ? '待定' : (match ? match.teamB : '') }}</div>
            <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">
              <span class="odds-value">{{ match.oddsB.toFixed(2) }}</span>
              <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                <el-icon class="odds-help"><QuestionFilled /></el-icon>
              </el-tooltip>
            </div>
            <div v-if="modifyForm.betSide === 1" class="team-card-check">&#10003;</div>
          </div>
        </div>
      </div>

      <div class="bet-amount-section">
        <div class="bet-amount-label">
          <span class="amount-title">押注积分</span>
          <span class="current-points">可用积分<b>{{ modifyMaxAmount }}</b></span>
          <el-button class="all-in-btn" size="small" @click="modifyForm.betAmount = Math.max(1, modifyMaxAmount)">ALL IN</el-button>
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
          <div class="est-main">
            <span class="est-label">预估返还</span>
            <span class="est-amount">{{ modifyEstimatedReturn }}</span>
            <span class="est-unit">积分</span>
          </div>
          <span class="est-odds">预估赔率 {{ modifyCurrentOdds }}</span>
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
import { fixLogoUrl } from '@/utils/format'
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
    fixLogo(url) { return fixLogoUrl(url) },
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
/* ===== Panel shell ===== */
.bet-panel {
  --panel-pad: 22px;
  position: relative;
  max-width: 880px;
  margin: 24px auto 0;
  padding: var(--panel-pad);
}

.bet-head {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.bet-head::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
}

.bet-head-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.bet-head-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
}

.bet-title {
  font-family: var(--font-body);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1.1;
  color: var(--text-primary);
}

/* ===== Shared state panels (TBD / login / closed) ===== */
.state-panel {
  margin-top: 18px;
  padding: 16px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  border-left: 2px solid var(--border-strong);
}

.state-panel--tbd {
  border-left-color: var(--text-faint);
  background: var(--bg-inset);
  border-style: dashed;
  border-left-style: solid;
}

.state-panel--closed {
  border-left-color: var(--accent-warning);
}

.state-panel--login {
  border-left-color: var(--brand);
  padding: 18px;
}

.state-code {
  display: inline-flex;
  align-items: center;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-tertiary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sharp);
  padding: 2px 6px;
  flex-shrink: 0;
  text-transform: uppercase;
  line-height: 1.4;
}

.state-panel--closed .state-code {
  color: var(--accent-warning);
  border-color: var(--accent-warning);
}

.state-panel--login .state-code {
  color: var(--brand);
  border-color: var(--brand);
}

.state-panel--tbd,
.state-panel--closed {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.state-copy {
  min-width: 0;
}

.state-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
}

.state-desc {
  margin-top: 3px;
  font-size: 12.5px;
  color: var(--text-tertiary);
  line-height: 1.55;
}

.state-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.state-head .state-title {
  font-size: 15px;
}

.state-panel--login .state-desc {
  font-size: 13px;
  margin-top: 0;
}

.prompt-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.prompt-btn {
  min-width: 108px;
  height: 36px;
  font-weight: 700;
}

.prompt-btn--ghost {
  height: 36px;
}

.prompt-hint {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-subtle);
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.03em;
  color: var(--text-faint);
}

/* ===== Already bet — amber summary ===== */
.already-bet {
  margin-top: 18px;
  padding: 14px 16px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--accent-warning);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ab-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.ab-eyebrow {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--accent-warning);
}

.ab-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  text-transform: uppercase;
}

.ab-body {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.ab-summary {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.ab-caption {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.ab-team {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.ab-amount {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-warning);
}

.ab-amount em {
  font-style: normal;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 500;
  color: var(--text-tertiary);
  margin-left: 1px;
}

.modify-bet-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--accent-warning);
  --el-button-text-color: var(--accent-warning);
  --el-button-hover-bg-color: var(--bg-surface-3);
  --el-button-hover-border-color: var(--accent-warning);
  --el-button-hover-text-color: var(--accent-warning);
  --el-button-active-bg-color: var(--bg-surface-3);
  --el-button-active-border-color: var(--accent-warning);
  --el-button-active-text-color: var(--accent-warning);
  height: 34px;
  font-size: 12.5px;
  font-weight: 700;
  flex-shrink: 0;
}

/* ===== Form ===== */
.bet-form {
  padding-top: 18px;
}

.field-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
  letter-spacing: 0.01em;
}

.field-hint {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  text-transform: uppercase;
}

.bet-side-cards {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 30px minmax(0, 1fr);
  align-items: stretch;
  gap: 10px;
}

.team-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-height: 152px;
  padding: 18px 12px 14px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  user-select: none;
  transition:
    border-color var(--duration-fast) ease,
    background var(--duration-fast) ease,
    transform var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) ease;
}

.team-card:hover {
  border-color: var(--border-strong);
  transform: translateY(-2px);
}

.team-card--selected,
.team-card--selected:hover {
  border-color: var(--brand);
  background:
    linear-gradient(180deg, var(--brand-soft), transparent 62%),
    var(--bg-surface-1);
  box-shadow: 0 0 0 1px var(--brand-line), var(--shadow-brand);
  transform: translateY(0);
}

.team-card-slot {
  position: absolute;
  top: 9px;
  left: 11px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-faint);
  line-height: 1;
}

.team-card--selected .team-card-slot {
  color: var(--brand);
}

.team-card-logo {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  border: 1px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
  margin-top: 2px;
}

.team-card--selected .team-card-logo {
  border-color: var(--brand-line);
  background: var(--bg-surface-3);
}

.team-card-logo-placeholder {
  font-family: var(--font-display);
  font-size: 24px;
  line-height: 1;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}

.team-card-name {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.35;
  overflow-wrap: anywhere;
  max-width: 100%;
  min-height: 2.7em;
  display: flex;
  align-items: center;
  justify-content: center;
}

.team-card-odds {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: auto;
  min-height: 28px;
}

.odds-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1;
  color: var(--accent-warning);
}

.team-card--selected .odds-value {
  color: var(--text-primary);
}

.odds-help {
  font-size: 13px;
  color: var(--text-faint);
  cursor: help;
  transition: color var(--duration-fast) ease;
}

.odds-help:hover {
  color: var(--text-secondary);
}

.team-card-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 21px;
  height: 21px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--brand);
  color: var(--cream);
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  border-radius: var(--radius-sm);
  clip-path: polygon(0 0, 100% 0, 100% calc(100% - 6px), calc(100% - 6px) 100%, 0 100%);
}

.team-card-vs {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.team-card-vs::before {
  content: '';
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 50%;
  width: 1px;
  background: var(--border-subtle);
}

.team-card-vs span {
  position: relative;
  padding: 3px 0;
  background: var(--bg-surface-1);
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-faint);
}

/* ===== Amount ===== */
.bet-amount-section {
  margin-top: 20px;
}

.bet-amount-label {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.amount-title {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--text-secondary);
}

.current-points {
  margin-left: auto;
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  font-size: 11.5px;
  font-weight: 500;
  color: var(--text-tertiary);
}

.current-points b {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 15px;
  font-weight: 700;
  color: var(--accent-primary);
  letter-spacing: -0.01em;
}

.all-in-btn {
  --el-button-bg-color: transparent;
  --el-button-border-color: var(--border-default);
  --el-button-text-color: var(--accent-primary);
  --el-button-hover-bg-color: var(--accent-primary-soft);
  --el-button-hover-border-color: var(--accent-primary);
  --el-button-hover-text-color: var(--accent-primary);
  --el-button-active-bg-color: var(--accent-primary-soft);
  --el-button-active-border-color: var(--accent-primary);
  --el-button-active-text-color: var(--accent-primary);
  height: 26px;
  padding: 0 10px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  flex-shrink: 0;
}

.all-in-btn:hover {
  box-shadow: var(--shadow-mint);
}

.slider-desktop {
  padding: 0 2px;
}

.slider-desktop :deep(.el-slider) {
  padding-left: 2px;
}

.slider-desktop :deep(.el-slider__input) {
  width: 82px;
}

.slider-desktop :deep(.el-input__inner) {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 13px;
  text-align: center;
}

.amount-stepper {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) 44px;
  align-items: stretch;
  gap: 8px;
  margin-top: 10px;
}

.stepper-btn {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 600;
  line-height: 1;
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition:
    color var(--duration-fast) ease,
    border-color var(--duration-fast) ease,
    background var(--duration-fast) ease,
    transform var(--duration-fast) ease;
}

.stepper-btn:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--bg-surface-3);
}

.stepper-btn:active {
  transform: scale(0.97);
}

.stepper-value {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 10px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 0.01em;
  color: var(--text-primary);
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.estimated-return {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px 12px;
  flex-wrap: wrap;
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--accent-primary-soft);
  border-left: 2px solid var(--accent-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.est-main {
  display: inline-flex;
  align-items: baseline;
  gap: 7px;
  min-width: 0;
}

.est-label {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.est-amount {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 21px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--accent-primary);
}

.est-unit {
  font-size: 11px;
  color: var(--text-tertiary);
}

.est-odds {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

/* ===== Primary CTA ===== */
.bet-btn {
  width: 100%;
  height: 48px;
  margin-top: 18px;
  border-radius: var(--radius-sm);
  font-weight: 700;
}

.bet-btn:not(.is-disabled):hover {
  box-shadow: var(--shadow-brand);
}

.cta-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  line-height: 1.1;
}

.cta-main {
  font-size: 15px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.cta-micro {
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.24em;
  color: var(--cream);
  opacity: 0.72;
}

/* ===== Footer stats ===== */
.bet-stats {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0;
  margin: 20px calc(var(--panel-pad) * -1) calc(var(--panel-pad) * -1);
  padding: 14px var(--panel-pad) 16px;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-inset);
  border-radius: 0 0 calc(var(--radius-lg) - 1px) calc(var(--radius-lg) - 1px);
}

.bet-stat {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 0 12px;
  min-width: 0;
}

.bet-stat:first-child {
  padding-left: 0;
  border-right: 1px solid var(--border-subtle);
}

.bet-stat:last-child {
  padding-right: 0;
  padding-left: 14px;
}

.stat-side {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  line-height: 1.5;
  color: var(--text-faint);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sharp);
  padding: 0 4px;
  flex-shrink: 0;
  margin-top: 2px;
}

.stat-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.stat-label {
  font-size: 12.5px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.stat-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10.5px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.stat-value b {
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-primary);
  margin-right: 1px;
}

.stat-label-tbd {
  color: var(--text-tertiary);
  font-style: normal;
}

/* ===== Confirm dialog ===== */
.confirm-body {
  display: flex;
  flex-direction: column;
}

.confirm-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.confirm-row:first-child {
  padding-top: 0;
}

.confirm-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.confirm-value {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  text-align: right;
  overflow-wrap: anywhere;
  min-width: 0;
}

.confirm-value--team {
  font-size: 15px;
}

.confirm-value--odds {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 16px;
  color: var(--accent-warning);
}

.confirm-note {
  margin-top: 8px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  text-transform: uppercase;
}

.confirm-outcomes {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  margin-top: 12px;
}

.outcome {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  min-width: 0;
}

.outcome--win {
  border-left: 2px solid var(--accent-primary);
}

.outcome--lose {
  border-left: 2px solid var(--accent-danger);
}

.outcome-label {
  font-size: 10.5px;
  color: var(--text-tertiary);
  line-height: 1.4;
}

.outcome-value {
  font-size: 10.5px;
  color: var(--text-tertiary);
  line-height: 1.3;
  white-space: nowrap;
}

.outcome-value strong {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin-right: 2px;
}

.outcome--win .outcome-value strong {
  color: var(--accent-primary);
}

.outcome--lose .outcome-value strong {
  color: var(--accent-danger);
}

/* ===== Modify dialog ===== */
.modify-bet-form .bet-side-select {
  margin-bottom: 18px;
}

.modify-bet-form .bet-amount-section {
  margin-top: 0;
  margin-bottom: 0;
}

.modify-bet-form .team-card {
  min-height: 130px;
  padding: 14px 10px 12px;
  gap: 7px;
}

.modify-bet-form .team-card-logo {
  width: 44px;
  height: 44px;
}

.modify-bet-form .team-card-name {
  font-size: 13px;
}

.modify-bet-form .odds-value {
  font-size: 20px;
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .bet-panel {
    --panel-pad: 16px;
    margin-top: 16px;
  }

  .bet-title {
    font-size: 22px;
  }

  .bet-head-meta {
    font-size: 9px;
  }

  .bet-side-cards {
    grid-template-columns: minmax(0, 1fr) 22px minmax(0, 1fr);
    gap: 8px;
  }

  .team-card {
    min-height: 138px;
    padding: 16px 10px 12px;
    gap: 6px;
  }

  .team-card-logo {
    width: 46px;
    height: 46px;
  }

  .team-card-name {
    font-size: 13px;
    min-height: 2.7em;
  }

  .odds-value {
    font-size: 21px;
  }

  .team-card-vs span {
    font-size: 8.5px;
    letter-spacing: 0;
  }

  .bet-amount-label {
    gap: 6px;
  }

  .current-points {
    font-size: 11px;
  }

  .current-points b {
    font-size: 14px;
  }

  .est-amount {
    font-size: 19px;
  }

  .bet-stats {
    margin-top: 16px;
    padding-top: 12px;
    padding-bottom: 14px;
  }

  .stat-label {
    font-size: 12px;
  }

  .stat-value {
    font-size: 10px;
  }

  .ab-body {
    align-items: stretch;
  }

  .modify-bet-btn {
    width: 100%;
  }

  .prompt-actions .el-button {
    flex: 1;
    min-width: 0;
  }

  .prompt-btn,
  .prompt-btn--ghost {
    height: 38px;
  }
}

@media (max-width: 480px) {
  .bet-panel {
    --panel-pad: 12px;
  }

  .bet-head-meta {
    display: none;
  }

  .bet-side-cards {
    grid-template-columns: minmax(0, 1fr) 18px minmax(0, 1fr);
    gap: 6px;
  }

  .team-card {
    min-height: 122px;
    padding: 14px 8px 10px;
    gap: 5px;
  }

  .team-card-logo {
    width: 40px;
    height: 40px;
  }

  .team-card-name {
    font-size: 12px;
    line-height: 1.3;
    min-height: 2.6em;
  }

  .odds-value {
    font-size: 18px;
  }

  .team-card-check {
    width: 18px;
    height: 18px;
    font-size: 10px;
  }

  .team-card-slot {
    top: 7px;
    left: 8px;
    font-size: 8px;
  }

  .current-points {
    font-size: 11px;
  }

  .all-in-btn {
    margin-left: auto;
  }

  .amount-stepper {
    grid-template-columns: 42px minmax(0, 1fr) 42px;
    gap: 6px;
  }

  .stepper-btn {
    height: 42px;
    font-size: 18px;
  }

  .stepper-value {
    min-height: 42px;
    font-size: 20px;
  }

  .estimated-return {
    padding: 9px 11px;
    gap: 4px 10px;
  }

  .est-amount {
    font-size: 17px;
  }

  .est-odds {
    font-size: 9.5px;
  }

  .bet-btn {
    height: 48px;
    margin-top: 14px;
  }

  .cta-main {
    font-size: 14px;
  }

  .bet-stats {
    padding-top: 12px;
  }

  .bet-stat {
    padding: 0 8px;
  }

  .bet-stat:last-child {
    padding-left: 10px;
  }

  .stat-label {
    font-size: 11.5px;
  }

  .stat-value {
    font-size: 9.5px;
  }

  .confirm-outcomes {
    grid-template-columns: minmax(0, 1fr);
  }

  .outcome-value {
    white-space: normal;
  }

  .modify-bet-form .team-card {
    min-height: 116px;
    padding: 12px 8px 10px;
  }

  .modify-bet-form .team-card-logo {
    width: 38px;
    height: 38px;
  }

  .modify-bet-form .odds-value {
    font-size: 17px;
  }
}
</style>
