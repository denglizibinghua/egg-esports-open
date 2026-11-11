<template>
  <div class="public-profile">
    <!-- ── Top bar / back ───────────────────────────── -->
    <div class="profile-topbar">
      <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
      <span class="topbar-meta">PLAYER // DOSSIER</span>
    </div>

    <!-- ── Loading skeleton ─────────────────────────── -->
    <div v-if="loading" class="profile-skeleton" aria-busy="true" aria-label="加载中">
      <div class="sk sk-hero"></div>
      <div class="sk-grid">
        <div v-for="i in 4" :key="'sk-stat-' + i" class="sk sk-tile"></div>
      </div>
      <div class="sk-panels">
        <div class="sk sk-panel"></div>
        <div class="sk sk-panel"></div>
      </div>
      <span class="sk-caption">LOADING DOSSIER…</span>
    </div>

    <!-- ── Profile ──────────────────────────────────── -->
    <div v-else-if="user" class="profile-content">
      <!-- Identity hero -->
      <section class="identity-hero u-panel card-entrance">
        <span class="hero-grid" aria-hidden="true"></span>

        <div class="hero-main">
          <div class="hero-avatar-ring">
            <el-avatar :size="92" :src="user.avatar" :icon="UserFilled" />
          </div>
          <div class="hero-identity">
            <span class="u-eyebrow hero-eyebrow">// PLAYER DOSSIER</span>
            <h1 class="hero-name" :title="displayName">{{ displayName }}</h1>
            <div class="hero-badges">
              <LevelBadge :count="user.totalBets || 0" />
              <span :class="['rank-chip', rankChipClass]" :title="rankChipTitle">
                <span class="rank-chip-label">{{ rankDimensionLabel }}</span>
                <span class="rank-chip-value">{{ rankChipText }}</span>
              </span>
            </div>
          </div>
        </div>

        <div class="hero-points">
          <span class="hero-points-label">POINTS</span>
          <span class="hero-points-value">{{ user.points }}</span>
          <span class="hero-points-caption">积分</span>
        </div>
      </section>

      <!-- Stats grid -->
      <section class="stats-grid">
        <div class="stat-card card-entrance">
          <span class="stat-label">积分</span>
          <span class="stat-value stat-value--points">{{ user.points }}</span>
          <span class="stat-foot">Points</span>
        </div>
        <div class="stat-card card-entrance">
          <span class="stat-label">净收益</span>
          <span :class="['stat-value', netProfitClass]">{{ netProfitText }}</span>
          <span class="stat-foot">Net Profit</span>
        </div>
        <div class="stat-card card-entrance">
          <span class="stat-label">胜率</span>
          <span class="stat-value">{{ accuracy }}<em>%</em></span>
          <span class="stat-foot">Win Rate</span>
        </div>
        <div class="stat-card card-entrance">
          <span class="stat-label">总押注</span>
          <span class="stat-value">{{ user.totalBets || 0 }}</span>
          <span class="stat-foot">Bets</span>
        </div>
      </section>

      <div class="profile-panels">
        <!-- Ranking -->
        <section v-if="getToken()" class="rank-section u-panel">
          <div class="panel-head">
            <h2 class="panel-title">排行榜排名</h2>
            <span class="panel-meta">RANKING</span>
          </div>
          <div class="rank-items">
            <div class="rank-item" :class="{ active: rankDimension === 'points' }" @click="cycleRankDimension('points')">
              <span class="rank-item-label">积分榜</span>
              <span class="rank-item-value" v-if="ranks.points && ranks.points.rank > 0">#{{ ranks.points.rank }}</span>
              <span class="rank-item-value rank-item-none" v-else>未上榜</span>
            </div>
            <div class="rank-item" :class="{ active: rankDimension === 'accuracy' }" @click="cycleRankDimension('accuracy')">
              <span class="rank-item-label">胜率榜</span>
              <span class="rank-item-value" v-if="ranks.accuracy && ranks.accuracy.rank > 0">#{{ ranks.accuracy.rank }}</span>
              <span class="rank-item-value rank-item-none" v-else>未上榜</span>
            </div>
            <div class="rank-item" :class="{ active: rankDimension === 'worst-profit' }" @click="cycleRankDimension('worst-profit')">
              <span class="rank-item-label">亏损榜</span>
              <span class="rank-item-value" v-if="ranks['worst-profit'] && ranks['worst-profit'].rank > 0">#{{ ranks['worst-profit'].rank }}</span>
              <span class="rank-item-value rank-item-none" v-else>未上榜</span>
            </div>
          </div>
        </section>

        <section v-else class="rank-section u-panel">
          <div class="panel-head">
            <h2 class="panel-title">排行榜排名</h2>
            <span class="panel-meta">RANKING</span>
          </div>
          <div class="rank-login-prompt">
            <span class="prompt-mark" aria-hidden="true">//</span>
            <p>登录后查看排名</p>
            <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
          </div>
        </section>

        <!-- Record -->
        <section class="record-section u-panel">
          <div class="panel-head">
            <h2 class="panel-title">战绩记录</h2>
            <span class="panel-meta">RECORD</span>
          </div>
          <div class="record-body">
            <div class="form-strip">
              <span class="form-strip-label">Recent</span>
              <div class="form-cells">
                <span
                  v-for="i in 5"
                  :key="'form-' + i"
                  :class="['form-cell', { 'is-win': i <= streakCells }]"
                >W</span>
              </div>
              <span class="form-strip-note">{{ user.winStreak || 0 }} 连胜</span>
            </div>

            <div class="record-list">
              <div class="record-row">
                <span class="record-label">猜中场次</span>
                <span class="record-value">{{ user.correctBets || 0 }}<em>/ {{ user.totalBets || 0 }}</em></span>
              </div>
              <div class="record-row">
                <span class="record-label">未中场次</span>
                <span class="record-value">{{ missedBets }}</span>
              </div>
              <div class="record-row">
                <span class="record-label">当前连胜</span>
                <span class="record-value">{{ user.winStreak || 0 }}</span>
              </div>
              <div class="record-row">
                <span class="record-label">最高连胜</span>
                <span class="record-value">{{ user.maxWinStreak || 0 }}</span>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div class="login-hint">
        <p><el-icon><InfoFilled /></el-icon> {{ getToken() ? '投注明细仅本人可见' : '登录后可查看详细投注记录' }}</p>
        <el-button v-if="!getToken()" type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
        <el-button v-else size="small" @click="$router.push('/user')">回我的主页</el-button>
      </div>
    </div>

    <!-- ── Not found ────────────────────────────────── -->
    <div v-else class="empty-state">
      <span class="empty-mark" aria-hidden="true">//</span>
      <h3 class="empty-title">用户不存在</h3>
      <p class="empty-desc">该玩家可能已注销，或链接地址有误。</p>
      <el-button type="primary" class="empty-cta" @click="$router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import LevelBadge from '@/components/user/LevelBadge.vue'
import { ArrowLeft, UserFilled, InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'PublicProfile',
  components: { InfoFilled, LevelBadge },
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      ArrowLeft: markRaw(ArrowLeft),
      UserFilled: markRaw(UserFilled),
      loading: false,
      user: null,
      ranks: {
        points: null,
        accuracy: null,
        'worst-profit': null
      },
      rankDimension: 'points'
    }
  },
  computed: {
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.min(100, Math.round((this.user.correctBets / this.user.totalBets) * 100))
    },
    displayName() {
      if (!this.user) return '匿名玩家'
      return this.user.nickname || this.user.username || '匿名玩家'
    },
    netProfitValue() {
      if (!this.user) return null
      var v = this.user.netProfit
      if (v === undefined || v === null || !Number.isFinite(Number(v))) return null
      return Number(v)
    },
    netProfitText() {
      if (this.netProfitValue === null) return '—'
      return (this.netProfitValue > 0 ? '+' : '') + this.netProfitValue
    },
    netProfitClass() {
      if (this.netProfitValue === null) return 'stat-value--muted'
      return this.netProfitValue < 0 ? 'is-neg' : 'is-pos'
    },
    missedBets() {
      if (!this.user) return 0
      return Math.max(0, (this.user.totalBets || 0) - (this.user.correctBets || 0))
    },
    streakCells() {
      var streak = Number(this.user && this.user.winStreak) || 0
      return Math.min(5, Math.max(0, streak))
    },
    rankData() {
      return this.ranks[this.rankDimension] || null
    },
    rankDimensionLabel() {
      if (this.rankDimension === 'accuracy') return '胜率榜'
      if (this.rankDimension === 'worst-profit') return '亏损榜'
      return '积分榜'
    },
    rankChipText() {
      if (!this.getToken()) return '登录查看'
      var data = this.rankData
      return data && data.rank > 0 ? '#' + data.rank : '未上榜'
    },
    rankChipClass() {
      if (!this.getToken()) return 'is-muted'
      return this.rankData && this.rankData.rank > 0 ? 'is-ranked' : 'is-muted'
    },
    rankChipTitle() {
      var data = this.rankData
      if (!data || !data.rank) return ''
      return data.total ? '共 ' + data.total + ' 位玩家' : ''
    }
  },
  created() {
    this.fetchUser()
    this.fetchRanks()
  },
  methods: {
    fetchUser() {
      this.loading = true
      const userId = this.$route.params.id
      request.get('/user/' + userId).then(res => {
        this.user = res.data || res
      }).catch(() => {
        this.user = null
      }).finally(() => {
        this.loading = false
      })
    },
    fetchRanks() {
      if (!getToken()) return
      const userId = this.$route.params.id
      const dimensions = ['points', 'accuracy', 'worst-profit']
      dimensions.forEach(dim => {
        request.get('/leaderboard/user-rank/' + userId, { params: { dimension: dim } }).then(res => {
          this.ranks[dim] = res.data || res
        }).catch(() => {
          this.ranks[dim] = { rank: 0, total: 0 }
        })
      })
    },
    cycleRankDimension(dim) {
      this.rankDimension = dim
    },
    getToken() {
      return getToken()
    }
  }
}
</script>

<style scoped>
.public-profile {
  min-width: 0;
}

/* ── Top bar ────────────────────────────────────── */
.profile-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-subtle);
}

.back-btn {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.10em;
}

.back-btn:hover {
  color: var(--brand);
}

.topbar-meta {
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  white-space: nowrap;
}

/* ── Identity hero ──────────────────────────────── */
.identity-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 26px;
  margin-bottom: 18px;
}

.hero-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.028) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.028) 1px, transparent 1px);
  background-size: 34px 34px;
  -webkit-mask-image: linear-gradient(90deg, transparent 0%, #000 38%, #000 72%, transparent 100%);
  mask-image: linear-gradient(90deg, transparent 0%, #000 38%, #000 72%, transparent 100%);
  opacity: 0.7;
  pointer-events: none;
}

.hero-main {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  min-width: 0;
}

.hero-avatar-ring {
  flex-shrink: 0;
  padding: 5px;
  border: 1px solid var(--brand-line);
  border-radius: 50%;
  background: radial-gradient(circle at 50% 30%, rgba(255, 70, 85, 0.10), var(--bg-surface-1) 72%);
  box-shadow: 0 0 0 6px rgba(255, 255, 255, 0.018), 0 0 30px rgba(255, 70, 85, 0.10);
}

.hero-avatar-ring :deep(.el-avatar) {
  display: block;
  border-radius: 50%;
  background: var(--bg-surface-3);
}

.hero-identity {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.hero-eyebrow {
  letter-spacing: 0.16em;
}

.hero-name {
  max-width: 100%;
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  overflow-wrap: anywhere;
  word-break: break-word;
}

.hero-badges {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.rank-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 22px;
  padding: 0 8px;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  line-height: 1;
  white-space: nowrap;
}

.rank-chip-label {
  color: var(--text-faint);
}

.rank-chip-value {
  color: var(--text-tertiary);
  font-size: 10px;
  font-weight: 700;
}

.rank-chip.is-ranked {
  background: var(--brand-soft);
  border-color: var(--brand-line);
}

.rank-chip.is-ranked .rank-chip-value {
  color: var(--brand);
}

.rank-chip.is-muted .rank-chip-value {
  color: var(--text-tertiary);
}

.hero-points {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
  padding-left: 24px;
  border-left: 1px solid var(--border-subtle);
}

.hero-points-label {
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.hero-points-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 42px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.02em;
  line-height: 1;
}

.hero-points-caption {
  color: var(--text-tertiary);
  font-size: 11px;
  letter-spacing: 0.10em;
}

/* ── Stats grid ─────────────────────────────────── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 18px;
}

.stat-card {
  position: relative;
  padding: 15px 16px 13px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 32px;
  height: 2px;
  background: var(--border-default);
}

.stat-card:hover {
  border-color: var(--border-default);
}

.stat-label {
  display: block;
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.3;
}

.stat-value {
  display: block;
  margin-top: 6px;
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 26px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.01em;
  line-height: 1.1;
}

.stat-value--points {
  color: var(--accent-primary);
}

.stat-value em {
  margin-left: 1px;
  color: var(--text-tertiary);
  font-style: normal;
  font-size: 15px;
}

.stat-value.is-pos {
  color: var(--accent-primary);
}

.stat-value.is-neg {
  color: var(--accent-danger);
}

.stat-value--muted {
  color: var(--text-tertiary);
}

.stat-foot {
  display: block;
  margin-top: 5px;
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

/* ── Panels ─────────────────────────────────────── */
.profile-panels {
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  gap: 16px;
  align-items: start;
  margin-bottom: 18px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.panel-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.panel-meta {
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  white-space: nowrap;
}

/* Ranking items */
.rank-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px;
}

.rank-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  background: var(--bg-surface-2);
  border: 1px solid transparent;
  border-left: 2px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out);
}

.rank-item:hover {
  border-color: var(--border-default);
}

.rank-item.active {
  border-top-color: var(--border-subtle);
  border-right-color: var(--border-subtle);
  border-bottom-color: var(--border-subtle);
  border-left-color: var(--brand);
  background: linear-gradient(90deg, rgba(255, 70, 85, 0.10), rgba(255, 70, 85, 0.02));
}

.rank-item-label {
  color: var(--text-secondary);
  font-size: 12.5px;
}

.rank-item.active .rank-item-label {
  color: var(--text-primary);
  font-weight: 600;
}

.rank-item-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rank-item.active .rank-item-value {
  color: var(--brand);
}

.rank-item-none {
  color: var(--text-faint);
  font-size: 12px;
  font-weight: 400;
}

.rank-login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 26px 18px 28px;
  text-align: center;
}

.prompt-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  background: rgba(91, 140, 255, 0.07);
  border: 1px solid rgba(91, 140, 255, 0.24);
  border-radius: var(--radius-md);
  color: var(--accent-secondary);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.10em;
  line-height: 1;
}

.rank-login-prompt p {
  color: var(--text-tertiary);
  font-size: 13px;
}

/* Record */
.record-body {
  padding: 16px 18px 18px;
}

.form-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 12px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.form-strip-label {
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.form-cells {
  display: flex;
  gap: 4px;
}

.form-cell {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-subtle);
  border-radius: 2px;
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  line-height: 1;
}

.form-cell.is-win {
  background: rgba(0, 229, 160, 0.10);
  border-color: rgba(0, 229, 160, 0.35);
  color: var(--accent-primary);
}

.form-strip-note {
  margin-left: auto;
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.record-list {
  display: flex;
  flex-direction: column;
}

.record-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.record-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.record-label {
  color: var(--text-tertiary);
  font-size: 12.5px;
}

.record-value {
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.record-value em {
  margin-left: 2px;
  color: var(--text-faint);
  font-style: normal;
  font-weight: 400;
}

/* Login hint strip */
.login-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 13px 18px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--accent-secondary);
  border-radius: var(--radius-sm);
}

.login-hint p {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 12.5px;
}

.login-hint :deep(.el-icon) {
  color: var(--accent-secondary);
}

/* ── Loading skeleton ───────────────────────────── */
@keyframes pp-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.profile-skeleton {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.sk {
  background: linear-gradient(
    90deg,
    var(--bg-surface-2) 0%,
    var(--bg-surface-3) 45%,
    var(--bg-surface-2) 100%
  );
  background-size: 200% 100%;
  animation: pp-shimmer 1.8s ease-in-out infinite;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}

.sk-hero {
  height: 168px;
}

.sk-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}

.sk-tile {
  height: 96px;
  border-radius: var(--radius-md);
}

.sk-panels {
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  gap: 16px;
}

.sk-panel {
  height: 220px;
}

.sk-caption {
  align-self: center;
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.20em;
}

/* ── Empty / not found ──────────────────────────── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  max-width: 460px;
  margin: 24px auto 0;
  padding: 52px 24px;
  text-align: center;
  background: var(--bg-surface-1);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
  color: var(--brand);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0.10em;
  line-height: 1;
}

.empty-title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.empty-desc {
  margin-top: -6px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.empty-cta {
  margin-top: 6px;
}

/* ── Responsive ─────────────────────────────────── */
@media (max-width: 900px) {
  .profile-panels,
  .sk-panels {
    grid-template-columns: 1fr;
  }

  .identity-hero {
    gap: 18px;
  }

  .hero-points-value {
    font-size: 36px;
  }
}

@media (max-width: 767px) {
  .profile-topbar {
    margin-bottom: 18px;
  }

  .identity-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 20px;
  }

  .hero-avatar-ring :deep(.el-avatar) {
    width: 72px !important;
    height: 72px !important;
    line-height: 72px !important;
  }

  .hero-name {
    font-size: 23px;
  }

  .hero-points {
    align-items: flex-start;
    width: 100%;
    padding-left: 0;
    padding-top: 14px;
    border-left: 0;
    border-top: 1px solid var(--border-subtle);
  }

  .hero-points-value {
    font-size: 32px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .sk-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .login-hint {
    flex-wrap: wrap;
  }
}

@media (max-width: 430px) {
  .hero-main {
    gap: 14px;
  }

  .hero-avatar-ring {
    padding: 4px;
  }

  .hero-avatar-ring :deep(.el-avatar) {
    width: 64px !important;
    height: 64px !important;
    line-height: 64px !important;
  }

  .hero-name {
    font-size: 20px;
  }

  .hero-points-value {
    font-size: 28px;
  }

  .stat-card {
    padding: 13px 12px 11px;
  }

  .stat-value {
    font-size: 22px;
  }

  .rank-items,
  .record-body {
    padding: 12px;
  }

  .panel-head {
    padding: 12px 14px;
  }

  .form-cell {
    width: 20px;
    height: 20px;
  }
}
</style>
