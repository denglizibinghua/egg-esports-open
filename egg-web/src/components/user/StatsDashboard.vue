<template>
  <div class="dashboard-grid">
    <!-- Summary strip -->
    <div class="summary-strip u-panel card-entrance">
      <div class="summary-cell">
        <span class="summary-kicker">WIN RATE</span>
        <span class="summary-value summary-value--mint">{{ accuracy }}<em>%</em></span>
      </div>
      <div class="summary-cell">
        <span class="summary-kicker">CORRECT</span>
        <span class="summary-value">{{ user.correctBets || 0 }}<em>/ {{ user.totalBets || 0 }}</em></span>
      </div>
      <div class="summary-cell">
        <span class="summary-kicker">STREAK</span>
        <span class="summary-value">{{ user.winStreak || 0 }}</span>
      </div>
      <div class="summary-cell">
        <span class="summary-kicker">BEST STREAK</span>
        <span class="summary-value">{{ user.maxWinStreak || 0 }}</span>
      </div>
    </div>

    <!-- Points Trend -->
    <div class="dashboard-card dashboard-card--wide u-panel card-entrance">
      <div class="dashboard-card-header">
        <div class="panel-title-wrap">
          <span class="panel-index">01</span>
          <h4 class="dashboard-card-title">最近投注</h4>
        </div>
        <span class="dashboard-card-subtitle">最近 {{ recentBets.length }} 场</span>
      </div>
      <div class="panel-legend">
        <span class="legend legend--win">WIN</span>
        <span class="legend legend--loss">LOSS</span>
        <span class="legend legend--pending">PEND</span>
      </div>
      <div class="trend-plot" v-if="recentBets.length > 0">
        <div class="points-trend">
          <div
            v-for="(bet, idx) in recentBets"
            :key="'trend-'+idx"
            class="trend-bar"
            :class="{
              'trend-bar--win': bet.status === 1,
              'trend-bar--loss': bet.status === 2,
              'trend-bar--pending': bet.status === 0
            }"
            :style="{ height: trendBarHeight(bet) + 'px', animationDelay: (idx * 60) + 'ms' }"
            :title="(bet.matchTitle || '赛事') + ' | ' + (bet.status === 1 ? '+' + bet.betAmount : bet.status === 2 ? '-' + bet.betAmount : '待结算')"
          >
            <span class="trend-label">
              {{ bet.status === 1 ? '+' + bet.betAmount : bet.status === 2 ? '-' + bet.betAmount : '—' }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="dashboard-empty">
        <span>暂无数据</span>
      </div>
    </div>

    <!-- Win Rate Gauge -->
    <div class="dashboard-card u-panel card-entrance">
      <div class="dashboard-card-header">
        <div class="panel-title-wrap">
          <span class="panel-index">02</span>
          <h4 class="dashboard-card-title">胜率</h4>
        </div>
      </div>
      <div class="win-rate-gauge">
        <div class="gauge-ring">
          <svg viewBox="0 0 120 120" class="gauge-svg">
            <circle cx="60" cy="60" r="52" class="gauge-bg" />
            <circle
              cx="60" cy="60" r="52"
              class="gauge-fill"
              :style="{ strokeDashoffset: gaugeOffset }"
            />
          </svg>
          <div class="gauge-center">
            <span class="gauge-value">{{ accuracy }}</span>
            <span class="gauge-unit">%</span>
          </div>
        </div>
        <div class="gauge-stats">
          <div class="gauge-stat">
            <span class="gauge-stat-val" style="color: var(--accent-primary)">{{ user.correctBets || 0 }}</span>
            <span class="gauge-stat-lbl">猜中</span>
          </div>
          <div class="gauge-stat">
            <span class="gauge-stat-val" style="color: var(--accent-danger)">{{ (user.totalBets || 0) - (user.correctBets || 0) }}</span>
            <span class="gauge-stat-lbl">未中</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Streak Meter -->
    <div class="dashboard-card u-panel card-entrance">
      <div class="dashboard-card-header">
        <div class="panel-title-wrap">
          <span class="panel-index">03</span>
          <h4 class="dashboard-card-title">连胜记录</h4>
        </div>
      </div>
      <div class="streak-meter">
        <div class="streak-current">
          <span class="streak-number">{{ user.winStreak || 0 }}</span>
          <span class="streak-label">当前连胜</span>
        </div>
        <div class="streak-bar-track">
          <div
            class="streak-bar-fill"
            :style="{ width: streakPercent + '%' }"
          ></div>
        </div>
        <div class="streak-max">
          <span class="streak-max-label">最高连胜</span>
          <span class="streak-max-val">{{ user.maxWinStreak || 0 }}</span>
        </div>
      </div>
    </div>

    <!-- Recent Results Chart -->
    <div class="dashboard-card dashboard-card--wide u-panel card-entrance">
      <div class="dashboard-card-header">
        <div class="panel-title-wrap">
          <span class="panel-index">04</span>
          <h4 class="dashboard-card-title">战绩图</h4>
        </div>
        <div class="panel-meta">
          <span class="panel-legend panel-legend--inline">
            <span class="legend legend--win">WIN</span>
            <span class="legend legend--loss">LOSS</span>
            <span class="legend legend--pending">PEND</span>
          </span>
          <span class="dashboard-card-subtitle">最近 10 场</span>
        </div>
      </div>
      <div class="recent-results">
        <div
          v-for="i in 10"
          :key="'result-'+i"
          class="result-dot"
          :class="resultDotClass(i - 1)"
          :title="resultDotTitle(i - 1)"
        >
          <span class="result-icon">{{ resultDotIcon(i - 1) }}</span>
          <span class="result-team" v-if="recentBets[i - 1]">
            {{ recentBets[i - 1].betSide === 0 ? 'A' : 'B' }}
          </span>
        </div>
      </div>
    </div>

    <!-- Profit Curve -->
    <div class="dashboard-card dashboard-card--wide u-panel card-entrance">
      <div class="dashboard-card-header">
        <div class="panel-title-wrap">
          <span class="panel-index">05</span>
          <h4 class="dashboard-card-title">收益曲线</h4>
        </div>
        <span class="dashboard-card-subtitle">NET CUMULATIVE</span>
      </div>
      <div class="chart-container" v-if="displayProfitData && displayProfitData.length > 0" v-loading="profitLoading">
        <svg viewBox="0 0 800 220" class="profit-chart" preserveAspectRatio="xMidYMid meet">
          <defs>
            <linearGradient id="profitGradient" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="var(--accent-primary)" stop-opacity="0.3" />
              <stop offset="100%" stop-color="var(--accent-primary)" stop-opacity="0.02" />
            </linearGradient>
          </defs>
          <!-- Grid lines -->
          <line x1="50" y1="15" x2="50" y2="195" class="chart-axis-line" />
          <line x1="50" y1="195" x2="790" y2="195" class="chart-axis-line" />
          <!-- Zero line -->
          <line
            x1="50"
            :y1="profitZeroY"
            x2="790"
            :y2="profitZeroY"
            class="chart-zero-line"
          />
          <!-- Fill area -->
          <polygon :points="profitChartFill" fill="url(#profitGradient)" class="chart-fill" />
          <!-- Line -->
          <polyline :points="profitChartPoints" fill="none" class="chart-line" />
          <!-- Y axis labels -->
          <text x="45" y="198" class="chart-label chart-label--y" text-anchor="end">{{ profitChartMeta ? profitChartMeta.min : '' }}</text>
          <text x="45" y="18" class="chart-label chart-label--y" text-anchor="end">{{ profitChartMeta ? profitChartMeta.max : '' }}</text>
          <!-- X axis labels -->
          <text x="50" y="213" class="chart-label chart-label--x" text-anchor="start">{{ profitChartMeta ? profitChartMeta.first : '' }}</text>
          <text x="790" y="213" class="chart-label chart-label--x" text-anchor="end">{{ profitChartMeta ? profitChartMeta.last : '' }}</text>
          <!-- Data dots (interactive) -->
          <g v-for="(dot, i) in profitDotCoords" :key="'dot-'+i">
            <circle
              :cx="dot.x"
              :cy="dot.y"
              r="12"
              fill="transparent"
              class="chart-hit-area"
              @mouseenter="chartTooltip = { x: dot.x, y: dot.y, data: displayProfitData[i] }"
              @mouseleave="chartTooltip = null"
            />
            <circle
              :cx="dot.x"
              :cy="dot.y"
              r="3.5"
              class="chart-dot"
            />
          </g>
          <!-- Tooltip -->
          <g v-if="chartTooltip" class="chart-tooltip-group">
            <line
              :x1="chartTooltip.x"
              y1="15"
              :x2="chartTooltip.x"
              y2="195"
              class="chart-tooltip-line"
            />
            <circle
              :cx="chartTooltip.x"
              :cy="chartTooltip.y"
              r="5"
              class="chart-tooltip-dot"
            />
            <rect
              :x="chartTooltip.x > 600 ? chartTooltip.x - 130 : chartTooltip.x + 10"
              :y="chartTooltip.y > 150 ? chartTooltip.y - 48 : chartTooltip.y - 8"
              width="120"
              height="40"
              rx="6"
              class="chart-tooltip-bg"
            />
            <text
              :x="chartTooltip.x > 600 ? chartTooltip.x - 70 : chartTooltip.x + 70"
              :y="chartTooltip.y > 150 ? chartTooltip.y - 30 : chartTooltip.y + 10"
              class="chart-tooltip-text chart-tooltip-text--val"
              text-anchor="middle"
            >{{ chartTooltip.data.points }} 分</text>
            <text
              :x="chartTooltip.x > 600 ? chartTooltip.x - 70 : chartTooltip.x + 70"
              :y="chartTooltip.y > 150 ? chartTooltip.y - 14 : chartTooltip.y + 26"
              class="chart-tooltip-text chart-tooltip-text--time"
              text-anchor="middle"
            >{{ chartTooltip.data.time }}</text>
          </g>
        </svg>
      </div>
      <div class="empty-state chart-empty" v-else>
        <span class="empty-mark">//</span>
        <p>暂无结算数据</p>
        <span>结算后积分变化将在这里展示</span>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'

export default {
  name: 'StatsDashboard',
  props: {
    // 当前用户对象（含 totalBets/correctBets/winStreak/maxWinStreak/points 等统计字段）
    user: { type: Object, required: true },
    // 盈亏走势数据，若父组件已加载则传入，否则子组件自加载
    profitData: { type: Array, default: null }
  },
  data() {
    return {
      recentBets: [],
      // 子组件内部自加载的盈亏数据（当父组件未传入 profitData 时使用）
      innerProfitData: [],
      profitLoading: false,
      chartTooltip: null
    }
  },
  computed: {
    // 胜率百分比
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.min(100, Math.round((this.user.correctBets / this.user.totalBets) * 100))
    },
    // 实际用于渲染的盈亏数据：优先用 prop，否则用内部加载的
    displayProfitData() {
      if (this.profitData && this.profitData.length > 0) return this.profitData
      return this.innerProfitData
    },
    gaugeOffset() {
      var circumference = 2 * Math.PI * 52
      var offset = circumference - (this.accuracy / 100) * circumference
      return offset
    },
    streakPercent() {
      if (!this.user || !this.user.maxWinStreak || this.user.maxWinStreak === 0) return 0
      return Math.min(100, Math.round(((this.user.winStreak || 0) / this.user.maxWinStreak) * 100))
    },
    profitZeroY() {
      var data = this.displayProfitData
      if (!data || data.length === 0) return 105
      var padT = 15, padB = 25
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      return padT + h - ((0 - minP) / range) * h
    },
    profitChartPoints() {
      var data = this.displayProfitData
      if (!data || data.length === 0) return ''
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var pts = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        pts.push(x.toFixed(1) + ',' + y.toFixed(1))
      }
      return pts.join(' ')
    },
    profitChartFill() {
      var data = this.displayProfitData
      if (!data || data.length === 0) return ''
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var pts = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        pts.push(x.toFixed(1) + ',' + y.toFixed(1))
      }
      var bottomY = padT + h
      var firstX = n === 1 ? padL + w / 2 : padL
      var lastX = n === 1 ? padL + w / 2 : padL + (n - 1) * (w / (n - 1))
      pts.push(lastX.toFixed(1) + ',' + bottomY)
      pts.push(firstX.toFixed(1) + ',' + bottomY)
      return pts.join(' ')
    },
    profitChartMeta() {
      var data = this.displayProfitData
      if (!data || data.length === 0) return null
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      return {
        min: minP,
        max: maxP,
        first: data[0].time,
        last: data[data.length - 1].time,
        count: data.length
      }
    },
    profitDotCoords() {
      var data = this.displayProfitData
      if (!data || data.length === 0) return []
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var dots = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        dots.push({ x: x, y: y, idx: i })
      }
      return dots
    }
  },
  created() {
    // 子组件自洽：自动加载最近投注与盈亏曲线
    this.fetchRecentBets()
    // 父组件未传入 profitData 时，子组件自行加载
    if (!this.profitData || this.profitData.length === 0) {
      this.fetchProfitData()
    }
  },
  methods: {
    // 最近投注（趋势条 / 战绩图用）
    fetchRecentBets() {
      request.get('/bet/recent').then(function (res) {
        this.recentBets = res.data || res || []
        if (!Array.isArray(this.recentBets)) {
          this.recentBets = []
        }
      }.bind(this)).catch(function () {
        this.recentBets = []
      }.bind(this))
    },
    // 盈亏累计曲线：拉取所有已结算投注，按时间升序累计净收益
    fetchProfitData() {
      this.profitLoading = true
      request.get('/bet/settled-since').then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        var list = extractList(data)
        if (list.length === 0) {
          this.innerProfitData = []
          return
        }
        // Sort by createTime ASC (oldest→newest)
        list.sort(function (a, b) {
          return new Date(a.createTime) - new Date(b.createTime)
        })
        // Cumulative netChange starting from 0
        var cumulative = 0
        var result = []
        for (var i = 0; i < list.length; i++) {
          var net = list[i].netChange
          if (net === null || net === undefined) {
            if (list[i].status === 1) net = list[i].betAmount
            else if (list[i].status === 2) net = -list[i].betAmount
            else net = 0
          }
          cumulative += net
          var d = new Date(list[i].createTime)
          var m = d.getMonth() + 1
          var day = d.getDate()
          var h = d.getHours().toString().padStart(2, '0')
          var min = d.getMinutes().toString().padStart(2, '0')
          result.push({
            time: m + '/' + day + ' ' + h + ':' + min,
            points: cumulative
          })
        }
        this.innerProfitData = result
      }.bind(this)).catch(function () {
        this.innerProfitData = []
      }.bind(this)).finally(function () {
        this.profitLoading = false
      }.bind(this))
    },
    trendBarHeight(bet) {
      var base = 24
      var max = 80
      if (bet.status === 0) return base
      return Math.min(max, base + bet.betAmount * 2)
    },
    getBetTeamName(bet) {
      // 优先用下注时快照队名，兜底用 A队/B队
      if (bet.teamASnapshot || bet.teamBSnapshot) {
        return bet.betSide === 0
          ? (bet.teamASnapshot || 'A队')
          : (bet.teamBSnapshot || 'B队')
      }
      // 无快照时用当前 match 队名
      if (bet.teamA && bet.teamB) {
        return bet.betSide === 0 ? bet.teamA : bet.teamB
      }
      return bet.betSide === 0 ? 'A队' : 'B队'
    },
    resultDotClass(idx) {
      if (idx >= this.recentBets.length) return 'result-dot--empty'
      var bet = this.recentBets[idx]
      if (bet.status === 1) return 'result-dot--win'
      if (bet.status === 2) return 'result-dot--loss'
      return 'result-dot--pending'
    },
    resultDotIcon(idx) {
      if (idx >= this.recentBets.length) return '·'
      var bet = this.recentBets[idx]
      if (bet.status === 1) return '✓'
      if (bet.status === 2) return '✗'
      return '—'
    },
    resultDotTitle(idx) {
      if (idx >= this.recentBets.length) return '暂无数据'
      var bet = this.recentBets[idx]
      var team = this.getBetTeamName(bet)
      var status = bet.status === 1 ? '猜中' : bet.status === 2 ? '未中' : '待结算'
      return (bet.matchTitle || '赛事') + ' | ' + team + ' | ' + bet.betAmount + '分 | ' + status
    }
  }
}
</script>

<style scoped>
/* ---------- Grid ---------- */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.dashboard-card {
  display: flex;
  flex-direction: column;
  padding: 20px 22px 22px;
}

.dashboard-card--wide {
  grid-column: 1 / -1;
}

/* ---------- Summary strip ---------- */
.summary-strip {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  overflow: hidden;
}

.summary-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 14px 18px;
  border-right: 1px solid var(--border-subtle);
}

.summary-cell:last-child {
  border-right: none;
}

.summary-kicker {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}

.summary-value {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
  color: var(--text-primary);
}

.summary-value em {
  font-style: normal;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.summary-value--mint {
  color: var(--accent-primary);
}

/* ---------- Card header ---------- */
.dashboard-card-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 16px;
}

.panel-title-wrap {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.panel-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--brand);
}

.dashboard-card-title {
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  white-space: nowrap;
}

.dashboard-card-subtitle {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.panel-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

/* ---------- Legends ---------- */
.panel-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: -6px 0 14px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.panel-legend--inline {
  margin: 0;
  gap: 10px;
}

.legend {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.legend::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 1px;
  background: currentColor;
}

.legend--win {
  color: var(--accent-primary);
}

.legend--loss {
  color: var(--accent-danger);
}

.legend--pending {
  color: var(--accent-warning);
}

/* ---------- Trend bars ---------- */
.trend-plot {
  position: relative;
  padding: 6px 2px 0;
  background-image: linear-gradient(var(--border-subtle) 1px, transparent 1px);
  background-size: 100% 36px;
}

.trend-plot::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 1px;
  background: var(--border-default);
}

.points-trend {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 120px;
}

.trend-bar {
  flex: 1 1 0;
  min-width: 6px;
  border-radius: 2px 2px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  overflow: hidden;
  cursor: default;
  animation: trend-grow 0.5s var(--ease-out) both;
  transition: filter var(--duration-fast) ease;
}

@keyframes trend-grow {
  from {
    transform: scaleY(0);
    transform-origin: bottom;
  }
  to {
    transform: scaleY(1);
    transform-origin: bottom;
  }
}

.trend-bar:hover {
  filter: brightness(1.25);
}

.trend-bar--win {
  background: linear-gradient(180deg, var(--accent-primary) 0%, var(--bg-surface-3) 160%);
}

.trend-bar--loss {
  background: linear-gradient(180deg, var(--accent-danger) 0%, var(--bg-surface-3) 160%);
}

.trend-bar--pending {
  background: repeating-linear-gradient(
    -45deg,
    var(--accent-warning) 0 2px,
    var(--bg-surface-2) 2px 7px
  );
  border: 1px solid var(--border-default);
  border-bottom: none;
}

.trend-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--cream);
  text-shadow: 0 1px 2px var(--bg-page);
  white-space: nowrap;
}

.trend-bar--win .trend-label {
  color: var(--bg-page);
  text-shadow: none;
}

/* ---------- Empty ---------- */
.dashboard-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 120px;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);
  font-size: 11px;
  letter-spacing: 0.1em;
  color: var(--text-tertiary);
}

.dashboard-empty::before {
  content: '//';
  color: var(--brand);
  font-weight: 700;
}

/* ---------- Win rate gauge ---------- */
.win-rate-gauge {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
}

.gauge-ring {
  position: relative;
  width: 124px;
  height: 124px;
  flex-shrink: 0;
}

.gauge-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.gauge-bg {
  fill: none;
  stroke: var(--bg-surface-3);
  stroke-width: 7;
}

.gauge-fill {
  fill: none;
  stroke: var(--accent-primary);
  stroke-width: 7;
  stroke-linecap: round;
  stroke-dasharray: 326.73;
  transition: stroke-dashoffset 1s var(--ease-out);
}

.gauge-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.gauge-value {
  font-family: var(--font-mono);
  font-size: 30px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.gauge-unit {
  font-size: 13px;
  color: var(--text-tertiary);
  font-weight: 600;
  margin-top: 4px;
}

.gauge-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gauge-stat {
  display: flex;
  flex-direction: column;
}

.gauge-stat-val {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1.2;
}

.gauge-stat-lbl {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

/* ---------- Streak ---------- */
.streak-meter {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 16px;
}

.streak-current {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.streak-number {
  font-family: var(--font-mono);
  font-size: 38px;
  font-weight: 800;
  line-height: 1;
  color: var(--accent-primary);
  font-variant-numeric: tabular-nums;
}

.streak-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.streak-bar-track {
  position: relative;
  height: 10px;
  background: var(--bg-surface-3);
  border: 1px solid var(--border-subtle);
  overflow: hidden;
}

.streak-bar-track::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    90deg,
    transparent 0 9px,
    var(--bg-surface-1) 9px 10px
  );
  pointer-events: none;
}

.streak-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), var(--brand));
  transition: width 0.8s var(--ease-out);
}

.streak-max {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

.streak-max-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.streak-max-val {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-secondary);
}

/* ---------- Recent results ---------- */
.recent-results {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 8px;
}

.result-dot {
  width: 100%;
  max-width: 56px;
  height: 56px;
  justify-self: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-row);
  cursor: default;
  transition: transform var(--duration-fast) ease, border-color var(--duration-fast) ease;
  animation: dot-pop 0.3s var(--ease-out) both;
}

.result-dot:nth-child(1) { animation-delay: 0ms; }
.result-dot:nth-child(2) { animation-delay: 50ms; }
.result-dot:nth-child(3) { animation-delay: 100ms; }
.result-dot:nth-child(4) { animation-delay: 150ms; }
.result-dot:nth-child(5) { animation-delay: 200ms; }
.result-dot:nth-child(6) { animation-delay: 250ms; }
.result-dot:nth-child(7) { animation-delay: 300ms; }
.result-dot:nth-child(8) { animation-delay: 350ms; }
.result-dot:nth-child(9) { animation-delay: 400ms; }
.result-dot:nth-child(10) { animation-delay: 450ms; }

@keyframes dot-pop {
  from {
    opacity: 0;
    transform: scale(0.7);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.result-dot:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
}

.result-dot--win {
  background: var(--accent-primary-soft);
  border-color: var(--accent-primary);
}

.result-dot--loss {
  background: var(--bg-surface-2);
  border-color: var(--accent-danger);
}

.result-dot--pending {
  background: var(--bg-surface-2);
  border-color: var(--border-default);
}

.result-dot--empty {
  background: var(--bg-inset);
  border: 1px dashed var(--border-subtle);
}

.result-icon {
  font-family: var(--font-mono);
  font-size: 15px;
  font-weight: 700;
  line-height: 1;
}

.result-dot--win .result-icon {
  color: var(--accent-primary);
}

.result-dot--loss .result-icon {
  color: var(--accent-danger);
}

.result-dot--pending .result-icon {
  color: var(--accent-warning);
}

.result-dot--empty .result-icon {
  color: var(--text-faint);
}

.result-team {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

/* ---------- Profit chart ---------- */
.chart-container {
  position: relative;
  width: 100%;
  padding-top: 6px;
  background-image: linear-gradient(var(--border-subtle) 1px, transparent 1px);
  background-size: 100% 44px;
}

.profit-chart {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
}

.chart-axis-line {
  stroke: var(--border-subtle);
  stroke-width: 1;
}

.chart-zero-line {
  stroke: var(--text-tertiary);
  stroke-width: 1;
  stroke-dasharray: 6 4;
  opacity: 0.55;
}

.chart-fill {
  animation: chart-fade-in 0.8s var(--ease-out) both;
}

.chart-line {
  stroke: var(--accent-primary);
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  animation: chart-draw 1s var(--ease-out) both;
}

@keyframes chart-draw {
  from {
    stroke-dasharray: 2000;
    stroke-dashoffset: 2000;
  }
  to {
    stroke-dasharray: 2000;
    stroke-dashoffset: 0;
  }
}

@keyframes chart-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.chart-label {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  fill: var(--text-faint);
}

.chart-label--y {
  font-size: 10px;
  font-weight: 700;
}

.chart-label--x {
  font-size: 9px;
  font-weight: 400;
  letter-spacing: 0.04em;
}

.chart-dot {
  fill: var(--bg-surface-1);
  stroke: var(--accent-primary);
  stroke-width: 2;
  opacity: 0.9;
  transition: opacity 0.15s ease;
}

.chart-hit-area {
  cursor: crosshair;
}

.chart-hit-area:hover ~ .chart-dot {
  opacity: 1;
}

.chart-tooltip-line {
  stroke: var(--brand);
  stroke-width: 1;
  stroke-dasharray: 3 3;
  opacity: 0.6;
}

.chart-tooltip-dot {
  fill: var(--brand);
  stroke: var(--bg-surface-1);
  stroke-width: 2;
}

.chart-tooltip-bg {
  fill: var(--bg-surface-2);
  stroke: var(--border-default);
  stroke-width: 1;
}

.chart-tooltip-text {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.chart-tooltip-text--val {
  font-size: 13px;
  font-weight: 700;
  fill: var(--accent-primary);
}

.chart-tooltip-text--time {
  font-size: 10px;
  font-weight: 400;
  fill: var(--text-tertiary);
}

.chart-empty {
  min-height: 180px;
}

.chart-empty .empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  margin-bottom: 14px;
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.chart-empty p {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}

.chart-empty span:last-child {
  color: var(--text-tertiary);
  font-size: 12px;
}

/* ---------- Empty state (chart-empty 复用) ---------- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;
  max-width: 400px;
  margin: 0 auto;
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-card--wide {
    grid-column: 1;
  }

  .dashboard-card {
    padding: 18px;
  }
}

@media (max-width: 600px) {
  .summary-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-cell:nth-child(2) {
    border-right: none;
  }

  .summary-cell:nth-child(1),
  .summary-cell:nth-child(2) {
    border-bottom: 1px solid var(--border-subtle);
  }

  .win-rate-gauge {
    flex-direction: column;
    align-items: flex-start;
    justify-content: flex-start;
    gap: 16px;
  }

  .gauge-stats {
    flex-direction: row;
    gap: 28px;
  }

  .panel-legend {
    flex-wrap: wrap;
    gap: 10px;
  }

  .dashboard-card-header {
    flex-wrap: wrap;
  }

  .chart-label--x,
  .chart-label--y {
    display: none;
  }

  .points-trend {
    gap: 4px;
    height: 104px;
  }

  .trend-label {
    font-size: 8px;
  }

  .result-dot {
    height: 50px;
    max-width: none;
  }
}

@media (max-width: 420px) {
  .trend-label {
    display: none;
  }

  .result-dot {
    height: 46px;
  }

  .result-icon {
    font-size: 13px;
  }

  .recent-results {
    gap: 6px;
  }
}
</style>
