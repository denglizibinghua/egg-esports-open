<template>
  <div class="home">
    <!-- 新手引导卡片 -->
    <OnboardingCard />

    <!-- ===== HERO：左文案 / 右焦点赛 ===== -->
    <section class="hero" :class="heroThemeClass">
      <div class="hero-grid" aria-hidden="true"></div>

      <div class="hero-inner">
        <div class="hero-copy">
          <span class="hero-eyebrow">
            <span class="hero-eyebrow-mark"></span>
            VALORANT // PREDICTION ARENA
          </span>
          <h1 class="hero-title">
            <span class="hero-title-line">瓦洛兰特</span>
            <span class="hero-title-line hero-title-line--brand">预测社区</span>
          </h1>
          <p class="hero-sub">0% 抽水纯彩池 · 赔率实时同步 · 猜中按比例分池</p>

          <div class="hero-actions">
            <el-button
              v-if="heroMatch"
              type="primary"
              class="hero-primary btn-hover-lift"
              @click="goDetail(heroMatch.id)"
            >{{ heroMatch.status === 0 ? '立即竞猜' : heroMatch.status === 1 ? '查看实况' : '查看战报' }}</el-button>
            <router-link to="/matches" class="hero-ghost">查看赛程</router-link>
          </div>

          <div class="hero-stats">
            <div class="hero-stat">
              <span class="hero-stat-value">0%</span>
              <span class="hero-stat-label">抽水</span>
            </div>
            <div class="hero-stat">
              <span class="hero-stat-value">32</span>
              <span class="hero-stat-label">场赛事</span>
            </div>
            <div class="hero-stat">
              <span class="hero-stat-value">24H</span>
              <span class="hero-stat-label">实时赔率</span>
            </div>
          </div>
        </div>

        <!-- 焦点赛 -->
        <div v-if="heroMatch" class="hero-feature">
          <div class="hero-feature-head">
            <div class="hero-feature-left">
              <span class="tournament-chip">{{ heroMatch.tournament || '常规赛事' }}</span>
              <span class="hero-feature-tag">FEATURED</span>
            </div>
            <span v-if="heroMatch.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
            <span v-else-if="heroMatch.status === 0" class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>即将开始
            </span>
            <span v-else class="status-badge status-badge--finished">已结束</span>
          </div>

          <div class="hero-matchup">
            <div class="hero-side">
              <div class="hero-logo">
                <el-image :src="fixLogoUrl(heroMatch.teamALogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (heroMatch.teamA || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="hero-side-name">{{ heroMatch.teamA }}</span>
            </div>

            <div class="hero-mid">
              <div v-if="hasScore(heroMatch)" class="hero-score" :class="{ 'hero-score--live': heroMatch.status === 1 }">
                <span>{{ heroMatch.scoreA }}</span>
                <span class="hero-score-sep">:</span>
                <span>{{ heroMatch.scoreB }}</span>
              </div>
              <div v-else class="hero-vs">VS</div>
              <span class="hero-time">{{ formatTime(heroMatch.matchTime) }}</span>
            </div>

            <div class="hero-side">
              <div class="hero-logo">
                <el-image :src="fixLogoUrl(heroMatch.teamBLogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (heroMatch.teamB || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="hero-side-name">{{ heroMatch.teamB }}</span>
            </div>
          </div>

          <div v-if="heroMatch.oddsA !== null && heroMatch.oddsA !== undefined" class="hero-odds">
            <div class="hero-odds-label">
              <span class="hero-odds-eyebrow">ODDS · IMPLIED</span>
              <span class="hero-odds-pool">POOL 0%</span>
            </div>
            <div class="hero-odds-cells">
              <div class="hero-odd">
                <div class="hero-odd-head">
                  <span class="hero-odd-name">{{ heroMatch.teamA }}</span>
                  <span class="hero-odd-val">{{ heroMatch.oddsA.toFixed(2) }}</span>
                </div>
                <div class="hero-odd-track">
                  <span class="hero-odd-fill hero-odd-fill--a" :style="{ width: impliedPct(heroMatch.oddsA, heroMatch.oddsB) + '%' }"></span>
                </div>
              </div>
              <div class="hero-odd">
                <div class="hero-odd-head">
                  <span class="hero-odd-name">{{ heroMatch.teamB }}</span>
                  <span class="hero-odd-val">{{ heroMatch.oddsB.toFixed(2) }}</span>
                </div>
                <div class="hero-odd-track">
                  <span class="hero-odd-fill hero-odd-fill--b" :style="{ width: (100 - impliedPct(heroMatch.oddsA, heroMatch.oddsB)) + '%' }"></span>
                </div>
              </div>
            </div>
          </div>

          <div class="hero-feature-foot">
            <div class="hero-countdown" v-if="heroMatch && heroMatch.status === 0 && getCountdownSeconds(heroMatch) > 0">
              <div class="hero-countdown-box">
                <span class="hero-countdown-num">{{ Math.floor(getCountdownSeconds(heroMatch) / 86400) }}</span>
                <span class="hero-countdown-label">天</span>
              </div>
              <div class="hero-countdown-box">
                <span class="hero-countdown-num">{{ Math.floor((getCountdownSeconds(heroMatch) % 86400) / 3600) }}</span>
                <span class="hero-countdown-label">时</span>
              </div>
              <div class="hero-countdown-box">
                <span class="hero-countdown-num">{{ Math.floor((getCountdownSeconds(heroMatch) % 3600) / 60) }}</span>
                <span class="hero-countdown-label">分</span>
              </div>
            </div>
            <el-button class="hero-feature-cta" @click="goDetail(heroMatch.id)">
              进入比赛<span class="hero-feature-cta-arrow">→</span>
            </el-button>
          </div>
        </div>

        <div v-else class="hero-empty">
          <span class="hero-empty-mark">//</span>
          <span class="hero-empty-label">NO ACTIVE MATCH</span>
          <p class="hero-empty-text">暂无赛事，管理员正在筹备中</p>
        </div>
      </div>
    </section>

    <!-- 瓦聚焦：多游戏入口已隐藏，保留Adapter，可?gameId=2调试 -->
    <div class="game-tabs" v-if="false && games.length > 0">
      <button class="game-tab" :class="{ active: selectedGameId === null }" @click="switchGame(null)">
        全部游戏
      </button>
      <button
        v-for="game in games"
        :key="game.id"
        class="game-tab"
        :class="{ active: selectedGameId === game.id }"
        @click="switchGame(game.id)"
      >
        <el-image v-if="game.icon" :src="game.icon" class="game-icon" fit="contain">
          <template #error><span class="img-fallback">{{ (game.name || '').charAt(0) }}</span></template>
        </el-image>
        {{ game.name }}
      </button>
    </div>

    <!-- 公告栏 D1: 32px细条，首日隐藏由 prop 控制 -->
    <AnnouncementBar :game-id="selectedGameId" :compact="true" :visible="announcementVisible" />

    <!-- ===== 01 今日焦点 ===== -->
    <section v-if="focusMatches.length > 0" ref="focusSection" class="home-section">
      <div class="section-head">
        <div class="section-head-main">
          <span class="section-index">01</span>
          <h2 class="section-title">焦点赛事</h2>
          <span class="section-meta">
            共 {{ focusMatches.length }} 场<span v-if="focusLiveCount > 0"> · {{ focusLiveCount }} 进行中</span>
          </span>
        </div>
        <span class="section-count">TOTAL {{ ('0' + focusMatches.length).slice(-2) }}</span>
      </div>

      <div class="card-list">
        <article
          v-for="match in focusMatches"
          :key="'focus-'+match.id"
          class="match-card card-entrance"
          :class="{ 'match-card--live': match.status === 1 }"
          @click="goDetail(match.id)"
        >
          <div class="mc-head">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span v-if="match.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
            <span v-else-if="match.status === 0" class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
            <span v-else class="status-badge status-badge--finished">已结束</span>
          </div>

          <div class="mc-teams">
            <div class="mc-team">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamALogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (match.teamA || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="mc-team-name">{{ match.teamA }}</span>
            </div>

            <div v-if="hasScore(match)" class="mc-score" :class="{ 'mc-score--live': match.status === 1 }">
              <span>{{ match.scoreA }}</span>
              <span class="mc-score-sep">:</span>
              <span>{{ match.scoreB }}</span>
            </div>
            <div v-else class="mc-vs">VS</div>

            <div class="mc-team">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamBLogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (match.teamB || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="mc-team-name">{{ match.teamB }}</span>
            </div>
          </div>

          <div v-if="match.status === 2 && getMapScores(match).length" class="map-chips map-chips--sm mc-maps">
            <span
              v-for="(m, idx) in getMapScores(match)"
              :key="idx"
              class="map-chip"
              :class="{ 'map-chip--win': m.winner !== null && m.winner !== undefined }"
            >{{ m.map }} {{ m.scoreA }}-{{ m.scoreB }}</span>
          </div>

          <div class="mc-foot">
            <div class="mc-foot-left">
              <span class="mc-time">{{ formatTime(match.matchTime) }}</span>
              <span v-if="match.status === 0 && getCountdownSeconds(match) > 0" class="countdown-badge">{{ formatCountdown(match) }}</span>
            </div>
            <div class="mc-foot-right">
              <span v-if="match.betCount" class="mc-bets"><span class="mc-bets-num">{{ match.betCount }}</span> 注</span>
              <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-odds-capsule">{{ match.oddsA.toFixed(2) }} / {{ match.oddsB.toFixed(2) }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- ===== 02 全部赛程 ===== -->
    <section v-if="upcomingMatches.length > 0" ref="upcomingSection" class="home-section">
      <div class="section-head">
        <div class="section-head-main">
          <span class="section-index">02</span>
          <h2 class="section-title">全部赛程</h2>
          <span class="section-meta" v-if="upcomingMatches.length > 6">
            已展示 {{ displayUpcoming.length }}/{{ upcomingMatches.length }}
          </span>
        </div>
        <span class="section-count">TOTAL {{ ('0' + upcomingMatches.length).slice(-2) }}</span>
      </div>

      <div class="card-list">
        <article
          v-for="match in displayUpcoming"
          :key="'up-'+match.id"
          class="match-card card-entrance"
          @click="goDetail(match.id)"
        >
          <div class="mc-head">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
          </div>

          <div class="mc-teams">
            <div class="mc-team">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamALogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (match.teamA || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="mc-team-name">{{ match.teamA }}</span>
            </div>

            <div class="mc-vs">VS</div>

            <div class="mc-team">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamBLogo)" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ (match.teamB || '').charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="mc-team-name">{{ match.teamB }}</span>
            </div>
          </div>

          <div v-if="match.status === 2 && getMapScores(match).length" class="map-chips map-chips--sm mc-maps">
            <span
              v-for="(m, idx) in getMapScores(match)"
              :key="idx"
              class="map-chip"
              :class="{ 'map-chip--win': m.winner !== null && m.winner !== undefined }"
            >{{ m.map }} {{ m.scoreA }}-{{ m.scoreB }}</span>
          </div>

          <div class="mc-foot">
            <div class="mc-foot-left">
              <span class="mc-time">{{ formatTime(match.matchTime) }}</span>
              <span v-if="getCountdownSeconds(match) > 0" class="countdown-badge">{{ formatCountdown(match) }}</span>
            </div>
            <div class="mc-foot-right">
              <span v-if="match.betCount" class="mc-bets"><span class="mc-bets-num">{{ match.betCount }}</span> 注</span>
              <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-odds-capsule">{{ match.oddsA.toFixed(2) }} / {{ match.oddsB.toFixed(2) }}</span>
            </div>
          </div>
        </article>
      </div>

      <div v-if="hasUpcomingMore" class="section-expand">
        <button class="section-expand-btn" @click="toggleUpcoming">
          <span v-if="!upcomingShowAll">展开{{ upcomingRemainCount }}场 ▾</span>
          <span v-else>收起 ▴</span>
        </button>
        <div class="section-dots">
          <span class="section-dot" :class="{ active: !upcomingShowAll }"></span>
          <span class="section-dot" :class="{ active: upcomingShowAll }"></span>
        </div>
      </div>
    </section>

    <!-- ===== 03 预言家排行 ===== -->
    <section ref="rankSection" class="home-section home-section--rank">
      <div class="section-head">
        <div class="section-head-main">
          <span class="section-index">03</span>
          <h2 class="section-title">预言家排行</h2>
        </div>
        <div class="section-head-right">
          <span class="section-count" v-if="leaderboard.length > 0">Top {{ leaderboard.length }}</span>
          <router-link v-if="top3.length > 0" to="/leaderboard" class="section-link">查看完整榜单 →</router-link>
        </div>
      </div>

      <div v-if="rankLoading" class="podium podium--loading">
        <div class="podium-slot podium-slot--silver">
          <div class="podium-card skeleton">
            <span class="skeleton-line sk-avatar"></span>
            <span class="skeleton-line sk-rank-name"></span>
            <span class="skeleton-line sk-rank-pts"></span>
          </div>
          <div class="podium-plinth podium-plinth--silver"></div>
        </div>
        <div class="podium-slot podium-slot--gold">
          <div class="podium-card skeleton">
            <span class="skeleton-line sk-avatar sk-avatar--lg"></span>
            <span class="skeleton-line sk-rank-name"></span>
            <span class="skeleton-line sk-rank-pts"></span>
          </div>
          <div class="podium-plinth podium-plinth--gold"></div>
        </div>
        <div class="podium-slot podium-slot--bronze">
          <div class="podium-card skeleton">
            <span class="skeleton-line sk-avatar"></span>
            <span class="skeleton-line sk-rank-name"></span>
            <span class="skeleton-line sk-rank-pts"></span>
          </div>
          <div class="podium-plinth podium-plinth--bronze"></div>
        </div>
      </div>

      <div v-else-if="top3.length === 0 && leaderboard.length === 0" class="empty-state">
        <div class="empty-mark">//</div>
        <h3 class="empty-title">排行暂无数据</h3>
        <p class="empty-desc">还没有人参与竞猜，成为第一个上榜的人吧</p>
        <router-link to="/matches" class="empty-cta">去竞猜 <span>→</span></router-link>
      </div>

      <template v-else>
        <div v-if="top3.length > 0" class="podium">
          <div v-if="top3[1]" class="podium-slot podium-slot--silver">
            <div class="podium-card">
              <span class="podium-plate">02</span>
              <div class="podium-avatar-ring">
                <el-avatar :size="56" :src="top3[1].avatar" :icon="UserFilled" />
              </div>
              <div class="podium-name">{{ top3[1].nickname || top3[1].username }}</div>
              <div class="podium-points">
                <span class="podium-points-num">{{ top3[1].points }}</span>
                <span class="podium-points-unit">PTS</span>
              </div>
            </div>
            <div class="podium-plinth podium-plinth--silver">
              <span class="podium-plinth-rank">2</span>
            </div>
          </div>

          <div v-if="top3[0]" class="podium-slot podium-slot--gold">
            <svg class="podium-crown" viewBox="0 0 28 18" aria-hidden="true" focusable="false">
              <path d="M2 4l5 4 7-6 7 6 5-4-2 11H4L2 4z" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round" />
              <path d="M5 17h18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" />
            </svg>
            <div class="podium-card">
              <span class="podium-plate">01</span>
              <div class="podium-avatar-ring podium-avatar-ring--gold">
                <el-avatar :size="72" :src="top3[0].avatar" :icon="UserFilled" />
              </div>
              <div class="podium-name">{{ top3[0].nickname || top3[0].username }}</div>
              <div class="podium-points">
                <span class="podium-points-num">{{ top3[0].points }}</span>
                <span class="podium-points-unit">PTS</span>
              </div>
            </div>
            <div class="podium-plinth podium-plinth--gold">
              <span class="podium-plinth-rank">1</span>
            </div>
          </div>

          <div v-if="top3[2]" class="podium-slot podium-slot--bronze">
            <div class="podium-card">
              <span class="podium-plate">03</span>
              <div class="podium-avatar-ring">
                <el-avatar :size="56" :src="top3[2].avatar" :icon="UserFilled" />
              </div>
              <div class="podium-name">{{ top3[2].nickname || top3[2].username }}</div>
              <div class="podium-points">
                <span class="podium-points-num">{{ top3[2].points }}</span>
                <span class="podium-points-unit">PTS</span>
              </div>
            </div>
            <div class="podium-plinth podium-plinth--bronze">
              <span class="podium-plinth-rank">3</span>
            </div>
          </div>
        </div>
      </template>
    </section>

    <!-- 加载骨架（全空时） -->
    <div v-if="!loaded && focusMatches.length === 0 && upcomingMatches.length === 0" class="loading-full">
      <div v-for="i in 6" :key="'sk-'+i" class="match-card match-card--skeleton skeleton card-entrance">
        <div class="sk-head">
          <span class="skeleton-line sk-chip"></span>
          <span class="skeleton-line sk-badge"></span>
        </div>
        <div class="sk-teams">
          <div class="sk-team">
            <span class="skeleton-line sk-logo"></span>
            <span class="skeleton-line sk-name"></span>
          </div>
          <span class="skeleton-line sk-vs"></span>
          <div class="sk-team">
            <span class="skeleton-line sk-logo"></span>
            <span class="skeleton-line sk-name"></span>
          </div>
        </div>
        <div class="sk-foot">
          <span class="skeleton-line sk-time"></span>
          <span class="skeleton-line sk-odds"></span>
        </div>
      </div>
    </div>

    <!-- 右侧浮动导航（保留 DOM 以备回滚，默认隐藏） -->
    <div class="float-nav">
      <button v-if="focusMatches.length > 0" class="float-nav-btn" title="今日焦点" @click="scrollToSection('focus')">01</button>
      <button v-if="upcomingMatches.length > 0" class="float-nav-btn" title="全部赛程" @click="scrollToSection('upcoming')">02</button>
      <button v-if="top3.length > 0 || leaderboard.length > 0" class="float-nav-btn" title="排行榜" @click="scrollToSection('rank')">03</button>
      <button class="float-nav-btn" title="回顶部" @click="scrollToTop">↑</button>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import AnnouncementBar from '@/components/AnnouncementBar.vue'
import OnboardingCard from '@/components/OnboardingCard.vue'
import { markRaw } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
// 时间格式化工具（MM-DD HH:mm），统一提取自 @/utils/format
import { formatTime, fixLogoUrl } from '@/utils/format'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { getUser } from '@/utils/auth'

export default {
  name: 'HomePage',
  components: { AnnouncementBar, OnboardingCard },
  data() {
    return {
      // 暴露给模板 :icon 绑定的图标组件（用 markRaw 避免响应化）
      UserFilled: markRaw(UserFilled),
      games: [],
      selectedGameId: 1,
      // hero
      heroMatch: null,
      // sections
      hotMatches: [],
      liveMatches: [],
      upcomingMatches: [],
      top3: [],
      leaderboard: [],
      // state
      loaded: false,
      rankLoading: false,
      pollTimer: null,
      announcementVisible: true,
      // D2: 全部赛程折叠状态
      upcomingShowAll: false
    }
  },
  computed: {
    heroThemeClass() {
      if (this.selectedGameId === 1) return 'hero--valorant'
      if (this.selectedGameId === 2) return 'hero--lol'
      if (this.selectedGameId === 3) return 'hero--dota'
      if (this.selectedGameId === 4) return 'hero--cs2'
      return 'hero--default'
    },
    // D2: 合并 hot+live 去重取6，保证 3列×2行 无孤卡
    focusMatches() {
      var seen = {}
      var merged = []
      var live = this.liveMatches || []
      var hot = this.hotMatches || []
      for (var i = 0; i < live.length; i++) {
        var m = live[i]
        if (!seen[m.id]) { seen[m.id] = true; merged.push(m) }
      }
      for (var j = 0; j < hot.length; j++) {
        var h = hot[j]
        if (!seen[h.id]) { seen[h.id] = true; merged.push(h) }
      }
      return merged.slice(0, 6)
    },
    focusLiveCount() {
      var c = 0
      for (var i = 0; i < this.focusMatches.length; i++) if (this.focusMatches[i].status === 1) c++
      return c
    },
    // D2: 全部赛程 默认6张 + 展开
    displayUpcoming() {
      if (this.upcomingShowAll) return this.upcomingMatches
      return (this.upcomingMatches || []).slice(0, 6)
    },
    hasUpcomingMore() {
      return (this.upcomingMatches || []).length > 6
    },
    upcomingRemainCount() {
      return Math.max(0, (this.upcomingMatches || []).length - 6)
    },
    // 兼容旧命名：供模板过渡使用
    displayHot6() {
      return this.focusMatches
    }
  },
  created() {
    // 瓦聚焦：默认瓦洛兰特，支持 ?gameId=2 调试保留Adapter
    var q = this.$route && this.$route.query && this.$route.query.gameId
    if (q !== undefined && q !== null && q !== '') {
      var parsed = Number(q)
      if (!isNaN(parsed)) this.selectedGameId = parsed
    }
    // D1: 首日不显示公告栏（给 AnnouncementBar 传 visible prop 控制）
    try {
      var KEY = 'egg_home_first_seen'
      var first = localStorage.getItem(KEY)
      if (!first) {
        localStorage.setItem(KEY, String(Date.now()))
        this.announcementVisible = false
      } else {
        var elapsed = Date.now() - Number(first)
        this.announcementVisible = elapsed > 86400000
      }
    } catch (e) { this.announcementVisible = true }
    this.fetchGames()
    this.loadAll()
    this.startPolling()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    // ===== 工具 =====
    // formatTime / extractList 已外移到 @/utils，此处通过 ES6 简写暴露 formatTime 给模板
    formatTime,
    fixLogoUrl,
    filterTBD(list) {
      // 过滤所有 TBD 变体（TBD、TBD-1、TBD-2 等）
      return list.filter(function (m) {
        return !/^TBD/.test(m.teamA) && !/^TBD/.test(m.teamB)
      })
    },
    hasScore(match) {
      return (match.status === 1 || match.status === 2) &&
        match.scoreA !== null && match.scoreA !== undefined &&
        match.scoreB !== null && match.scoreB !== undefined
    },
    // 展示用：两项赔率归一化后的隐含胜率（仅用于 hero 赔率概率条宽度）
    impliedPct(oddsA, oddsB) {
      var a = Number(oddsA)
      var b = Number(oddsB)
      if (!a || !b || a <= 0 || b <= 0) return 50
      var ia = 1 / a
      var ib = 1 / b
      var total = ia + ib
      if (!total) return 50
      return Math.round((ia / total) * 100)
    },
    getMapScores(match) {
      if (match.mapScores && Array.isArray(match.mapScores)) return match.mapScores
      if (match.mapScoresJson) {
        try {
          var parsed = typeof match.mapScoresJson === 'string' ? JSON.parse(match.mapScoresJson) : match.mapScoresJson
          if (Array.isArray(parsed)) return parsed
        } catch (e) { /* ignore */ }
      }
      return []
    },
    getAccuracy(row) {
      if (!row.totalBets || row.totalBets === 0) return 0
      return Math.round((row.correctBets / row.totalBets) * 100)
    },
    getRankClass(rank) {
      if (rank === 1) return 'rank-gold'
      if (rank === 2) return 'rank-silver'
      if (rank === 3) return 'rank-bronze'
      return ''
    },
    getTableRowClass({ row }) {
      var user = getUser()
      if (user && row && (row.userId === user.id || row.username === user.username)) {
        return 'current-user-row'
      }
      return ''
    },
    goDetail(id) {
      this.$router.push('/match/' + id)
    },

    // ===== 倒计时工具 =====
    getCountdownSeconds(match) {
      if (!match.matchTime) return 0
      var diff = new Date(match.matchTime).getTime() - Date.now()
      return Math.max(0, Math.floor(diff / 1000))
    },
    formatCountdown(match) {
      var seconds = this.getCountdownSeconds(match)
      if (seconds <= 0) return ''
      var days = Math.floor(seconds / 86400)
      var hours = Math.floor((seconds % 86400) / 3600)
      var mins = Math.floor((seconds % 3600) / 60)
      if (days > 0) return days + '天' + hours + '时'
      if (hours > 0) return hours + '时' + mins + '分'
      return mins + '分'
    },

    // ===== 游戏筛选 =====
    fetchGames() {
      request.get('/game/list').then(function (res) {
        this.games = extractList(res.data || res)
      }.bind(this)).catch(function () {
        this.games = []
      }.bind(this))
    },
    switchGame(gameId) {
      this.selectedGameId = gameId
      this.upcomingShowAll = false
      this.loadAll()
    },
    toggleUpcoming() {
      this.upcomingShowAll = !this.upcomingShowAll
    },

    // ===== 全量加载 =====
    loadAll() {
      this.loaded = false
      this.fetchHot()
      this.fetchLive()
      this.fetchUpcoming()
      this.fetchRank()
      // 兜底：5s 后解除 loading
      var self = this
      setTimeout(function () { self.loaded = true }, 5000)
    },

    // ===== 热门 =====
    fetchHot() {
      var params = { limit: 6 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/hot', { params: params }).then(function (res) {
        this.hotMatches = this.filterTBD(extractList(res.data || res))
        this.updateHero()
      }.bind(this)).catch(function () {
        this.hotMatches = []
      }.bind(this))
    },
    updateHero() {
      // 优先 live，其次 upcoming，取 hot 和 live 的交集第一条
      var candidates = this.liveMatches.length > 0 ? this.liveMatches :
        (this.upcomingMatches.length > 0 ? this.upcomingMatches :
         (this.hotMatches.length > 0 ? this.hotMatches : []))
      this.heroMatch = candidates.length > 0 ? candidates[0] : null
    },

    // ===== 进行中 =====
    fetchLive() {
      var params = { page: 1, size: 6, status: 1 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/list', { params: params }).then(function (res) {
        this.liveMatches = this.filterTBD(extractList(res.data || res))
        if (!this.heroMatch) this.updateHero()
      }.bind(this)).catch(function () {
        this.liveMatches = []
      }.bind(this))
    },

    // ===== 即将开始 =====
    fetchUpcoming() {
      var params = { page: 1, size: 8, status: 0 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/list', { params: params }).then(function (res) {
        this.upcomingMatches = this.filterTBD(extractList(res.data || res))
        if (!this.heroMatch) this.updateHero()
        this.loaded = true
      }.bind(this)).catch(function () {
        this.upcomingMatches = []
        this.loaded = true
      }.bind(this))
    },

    // ===== 排行榜 ===== D5: 首页仅 podium3，完整表格迁至 /leaderboard
    fetchRank() {
      this.rankLoading = true
      var self = this
      var params = {}
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/leaderboard/top/3', { params: params }).then(function (res) {
        var data = res.data || res
        self.top3 = Array.isArray(data) ? data : []
      }.bind(self)).catch(function () { self.top3 = [] }.bind(self)).finally(function () {
        self.rankLoading = false
      }.bind(self))
      // 保留 leaderboard 空数组以兼容模板，完整数据由 /leaderboard 页加载
      self.leaderboard = []
    },

    // ===== 轮询 =====
    startPolling() {
      var self = this
      this.pollTimer = setInterval(function () {
        if (!document.hidden) {
          self.fetchHot()
          self.fetchLive()
          self.fetchUpcoming()
        }
      }, 30000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },

    // ===== 浮动导航 =====
    scrollToSection(key) {
      var refMap = { focus: 'focusSection', hot: 'focusSection', live: 'focusSection', upcoming: 'upcomingSection', rank: 'rankSection' }
      var ref = refMap[key]
      if (ref && this.$refs[ref]) {
        this.$refs[ref].scrollIntoView({ behavior: 'smooth', block: 'start' })
      }
    },
    scrollToTop() {
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
}
</script>

<style scoped>
.home {
  min-width: 0;
}

/* ================= HERO ================= */
.hero {
  --hero-glow-a: rgba(255, 70, 85, 0.16);
  --hero-glow-b: rgba(255, 181, 71, 0.05);
  position: relative;
  overflow: hidden;
  margin-bottom: 20px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(780px 430px at 4% -12%, var(--hero-glow-a), transparent 62%),
    radial-gradient(620px 360px at 98% 6%, var(--hero-glow-b), transparent 62%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.022), transparent 46%),
    var(--bg-surface-1);
}

.hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  z-index: 3;
  width: 148px;
  height: 2px;
  background: linear-gradient(90deg, var(--brand), transparent);
}

.hero::after {
  content: '';
  position: absolute;
  left: -1px;
  bottom: -1px;
  width: 16px;
  height: 16px;
  border-left: 1px solid var(--brand-line);
  border-bottom: 1px solid var(--brand-line);
  border-bottom-left-radius: var(--radius-xl);
  pointer-events: none;
}

.hero--lol {
  --hero-glow-a: rgba(91, 140, 255, 0.15);
  --hero-glow-b: rgba(0, 229, 160, 0.04);
}

.hero--dota {
  --hero-glow-a: rgba(255, 77, 109, 0.13);
  --hero-glow-b: rgba(91, 140, 255, 0.06);
}

.hero--cs2 {
  --hero-glow-a: rgba(222, 155, 40, 0.15);
  --hero-glow-b: rgba(255, 181, 71, 0.05);
}

.hero-grid {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.026) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.026) 1px, transparent 1px);
  background-size: 52px 52px;
  -webkit-mask-image: radial-gradient(ellipse 80% 120% at 32% 0%, rgba(0, 0, 0, 1) 0%, transparent 72%);
  mask-image: radial-gradient(ellipse 80% 120% at 32% 0%, rgba(0, 0, 0, 1) 0%, transparent 72%);
}

.hero-inner {
  position: relative;
  z-index: 2;
  display: grid;
  grid-template-columns: minmax(0, 44fr) minmax(0, 56fr);
  align-items: center;
  gap: 40px;
  padding: 40px;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
}

.hero-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--brand);
  white-space: nowrap;
}

.hero-eyebrow-mark {
  width: 6px;
  height: 6px;
  background: var(--brand);
  box-shadow: 0 0 10px rgba(255, 70, 85, 0.7);
  flex-shrink: 0;
}

.hero-title {
  display: flex;
  flex-direction: column;
  margin-top: 16px;
  font-family: var(--font-body);
  font-size: clamp(30px, 3.5vw, 44px);
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.hero-title-line--brand {
  color: var(--brand);
}

.hero-sub {
  max-width: 400px;
  margin-top: 14px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.hero-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}

.hero-primary {
  height: 42px;
  padding: 0 22px;
  font-size: 13px;
  letter-spacing: 0.06em;
}

.hero-ghost {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  transition: border-color var(--duration-fast) ease, background var(--duration-fast) ease, color var(--duration-fast) ease;
}

.hero-ghost:hover {
  color: var(--text-primary);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.hero-stats {
  display: flex;
  align-items: stretch;
  margin-top: 28px;
}

.hero-stat {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 0 18px;
  border-left: 1px solid var(--border-subtle);
}

.hero-stat:first-child {
  padding-left: 0;
  border-left: 0;
}

.hero-stat-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--text-primary);
}

.hero-stat-label {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  white-space: nowrap;
}

/* ---- featured match ---- */
.hero-feature {
  position: relative;
  min-width: 0;
  padding: 20px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.028), transparent 44%),
    var(--bg-inset);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  box-shadow: 0 18px 48px rgba(0, 0, 0, 0.42);
}

.hero-feature::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 18px;
  width: 64px;
  height: 2px;
  background: var(--brand);
}

.hero-feature::after {
  content: '';
  position: absolute;
  right: -1px;
  bottom: -1px;
  width: 14px;
  height: 14px;
  border-right: 1px solid var(--brand-line);
  border-bottom: 1px solid var(--brand-line);
  border-bottom-right-radius: var(--radius-lg);
  pointer-events: none;
}

.hero-feature-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
  margin-bottom: 18px;
}

.hero-feature-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.hero-feature-left .tournament-chip {
  display: inline-block;
  min-width: 0;
  max-width: 160px;
  flex: 0 1 auto;
}

.hero-feature-tag {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  white-space: nowrap;
  flex-shrink: 0;
}

.hero-matchup {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 10px;
}

.hero-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.hero-logo {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  box-shadow: inset 0 0 18px rgba(255, 255, 255, 0.03);
  flex-shrink: 0;
}

.hero-logo :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.hero-logo :deep(img) {
  padding: 5px;
}

.hero-side-name {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.3;
}

.hero-mid {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
  min-width: 74px;
  padding-top: 4px;
}

.hero-score {
  display: flex;
  align-items: baseline;
  gap: 6px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 46px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.03em;
  color: var(--text-primary);
}

.hero-score--live {
  color: var(--brand);
  text-shadow: 0 0 28px rgba(255, 70, 85, 0.45);
}

.hero-score-sep {
  font-size: 26px;
  font-weight: 400;
  color: var(--text-faint);
}

.hero-vs {
  font-family: var(--font-display);
  font-size: 42px;
  line-height: 1;
  letter-spacing: 0.08em;
  color: var(--text-secondary);
}

.hero-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.hero-odds {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid var(--border-subtle);
}

.hero-odds-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.hero-odds-eyebrow {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-tertiary);
}

.hero-odds-pool {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.14em;
  color: var(--accent-primary);
  white-space: nowrap;
}

.hero-odds-cells {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.hero-odd {
  min-width: 0;
  padding: 8px 10px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.hero-odd-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.hero-odd-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  color: var(--text-secondary);
}

.hero-odd-val {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 13px;
  font-weight: 700;
  color: var(--accent-warning);
  line-height: 1.2;
}

.hero-odd-track {
  height: 3px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 2px;
  background: rgba(255, 255, 255, 0.06);
}

.hero-odd-fill {
  display: block;
  height: 100%;
  border-radius: 2px;
  transition: width var(--duration-normal) var(--ease-out);
}

.hero-odd-fill--a {
  background: var(--team-a);
}

.hero-odd-fill--b {
  background: var(--team-b);
}

.hero-feature-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.hero-countdown {
  display: flex;
  gap: 6px;
}

.hero-countdown-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1px;
  min-width: 46px;
  padding: 6px 8px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.hero-countdown-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 18px;
  font-weight: 700;
  line-height: 1.1;
  color: var(--accent-warning);
}

.hero-countdown-label {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.1em;
  color: var(--text-tertiary);
}

.hero-feature-cta {
  --el-button-bg-color: rgba(255, 70, 85, 0.08);
  --el-button-border-color: rgba(255, 70, 85, 0.28);
  --el-button-text-color: var(--text-primary);
  --el-button-hover-bg-color: rgba(255, 70, 85, 0.16);
  --el-button-hover-border-color: var(--brand);
  --el-button-hover-text-color: var(--text-primary);
  --el-button-active-bg-color: rgba(255, 70, 85, 0.2);
  --el-button-active-border-color: var(--brand);
  --el-button-active-text-color: var(--text-primary);
  height: 38px;
  margin-left: auto;
  padding: 0 14px;
  font-size: 12px;
  font-weight: 600;
}

.hero-feature-cta-arrow {
  margin-left: 7px;
  font-family: var(--font-mono);
  color: var(--brand);
}

.hero-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  padding: 34px 20px;
  text-align: center;
  background: rgba(255, 255, 255, 0.012);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.hero-empty-mark {
  font-family: var(--font-mono);
  font-size: 32px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand);
  opacity: 0.7;
}

.hero-empty-label {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.18em;
  color: var(--text-faint);
}

.hero-empty-text {
  font-size: 13px;
  color: var(--text-secondary);
}

/* ===== 游戏筛选（保留隐藏 DOM） ===== */
.game-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px;
  margin-bottom: 16px;
  overflow-x: auto;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  scrollbar-width: none;
}
.game-tabs::-webkit-scrollbar { display: none; }
.game-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: color var(--duration-fast) ease, background var(--duration-fast) ease, border-color var(--duration-fast) ease;
}
.game-tab:hover { color: var(--text-primary); background: var(--bg-surface-2); }
.game-tab.active {
  color: var(--brand);
  background: var(--brand-soft);
  border-color: var(--brand-line);
  font-weight: 600;
}
.game-icon {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
}

/* ================= SECTION HEAD ================= */
.home-section {
  margin-bottom: 48px;
}

.home-section:last-of-type {
  margin-bottom: 0;
}

.section-head {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
}

.section-head::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
}

.section-head-main {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 10px;
  min-width: 0;
}

.section-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--brand);
  flex-shrink: 0;
}

.section-title {
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.section-meta {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.section-head-right {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.section-count {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
}

.section-link {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--text-secondary);
  white-space: nowrap;
  transition: color var(--duration-fast) ease;
}

.section-link:hover {
  color: var(--brand);
}

/* ================= MATCH CARDS ================= */
.card-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.match-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 178px;
  padding: 16px;
  cursor: pointer;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 45%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  transition: transform 0.24s var(--ease-out), border-color 0.24s var(--ease-out), box-shadow 0.24s var(--ease-out);
}

.match-card:hover {
  transform: translateY(-3px);
  border-color: rgba(255, 70, 85, 0.28);
  box-shadow: 0 14px 34px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(255, 70, 85, 0.08);
}

/* 入场动画结束后释放 transform，避免压住 hover 上浮 */
.match-card.card-entrance {
  animation-fill-mode: backwards;
}

.match-card--live {
  border-left: 3px solid var(--brand);
  padding-left: 14px;
  background:
    linear-gradient(135deg, rgba(255, 70, 85, 0.09), transparent 44%),
    var(--bg-surface-1);
}

.mc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
  margin-bottom: 13px;
}

.mc-head .tournament-chip {
  display: inline-block;
  min-width: 0;
  max-width: 150px;
  flex: 0 1 auto;
}

.mc-teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 8px;
  margin: 2px 0 14px;
}

.mc-team {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.team-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
}

.team-logo :deep(img) {
  padding: 4px;
}

.team-logo-placeholder {
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
}

.mc-team-name {
  width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.35;
  color: var(--text-primary);
}

.mc-score {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding-top: 9px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.mc-score--live {
  color: var(--brand);
  text-shadow: 0 0 18px rgba(255, 70, 85, 0.35);
}

.mc-score-sep {
  font-size: 15px;
  font-weight: 400;
  color: var(--text-faint);
}

.mc-vs {
  padding-top: 11px;
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 0.1em;
  color: var(--text-faint);
}

.mc-maps {
  margin: 0 0 12px;
}

.mc-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 11px;
  border-top: 1px solid var(--border-subtle);
  min-width: 0;
}

.mc-foot-left {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
}

.mc-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  letter-spacing: 0.03em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.mc-foot-right {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.mc-bets {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 9px;
  letter-spacing: 0.03em;
  color: var(--text-faint);
  white-space: nowrap;
}

.mc-bets-num {
  color: var(--text-tertiary);
}

.countdown-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  color: var(--accent-warning);
  background: rgba(255, 181, 71, 0.08);
  border: 1px solid rgba(255, 181, 71, 0.22);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.section-expand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}

.section-expand-btn {
  height: 34px;
  padding: 0 18px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text-secondary);
  background: var(--bg-surface-1);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration-fast) ease;
}

.section-expand-btn:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.section-dots {
  display: flex;
  align-items: center;
  gap: 5px;
}

.section-dot {
  width: 14px;
  height: 2px;
  background: var(--border-strong);
  transition: width var(--duration-normal) var(--ease-out), background var(--duration-normal) ease;
}

.section-dot.active {
  width: 22px;
  background: var(--brand);
}

/* ================= PODIUM ================= */
.podium {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 16px;
  max-width: 840px;
  margin: 0 auto;
}

.podium-slot {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  min-width: 0;
  max-width: 260px;
}

.podium-slot--gold {
  position: relative;
  padding-top: 26px;
}

.podium-crown {
  position: absolute;
  top: 0;
  left: 50%;
  width: 26px;
  height: 18px;
  color: var(--gold);
  transform: translateX(-50%);
  filter: drop-shadow(0 0 10px rgba(216, 178, 92, 0.35));
}

.podium-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 20px 14px 16px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.022), transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-bottom: 0;
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  transition: border-color var(--duration-normal) ease, background var(--duration-normal) ease;
}

.podium-slot--gold .podium-card {
  border-color: rgba(216, 178, 92, 0.32);
  background:
    linear-gradient(180deg, rgba(216, 178, 92, 0.11), transparent 58%),
    var(--bg-surface-1);
  box-shadow: 0 0 34px rgba(216, 178, 92, 0.08);
  padding-bottom: 20px;
}

.podium-slot--silver .podium-card {
  border-color: rgba(169, 176, 190, 0.24);
  background:
    linear-gradient(180deg, rgba(169, 176, 190, 0.07), transparent 58%),
    var(--bg-surface-1);
}

.podium-slot--bronze .podium-card {
  border-color: rgba(192, 122, 69, 0.26);
  background:
    linear-gradient(180deg, rgba(192, 122, 69, 0.08), transparent 58%),
    var(--bg-surface-1);
}

.podium-plate {
  position: absolute;
  top: 9px;
  left: 9px;
  padding: 1px 4px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sharp);
}

.podium-slot--gold .podium-plate {
  color: var(--gold);
  border-color: rgba(216, 178, 92, 0.35);
}

.podium-slot--silver .podium-plate {
  color: var(--silver);
  border-color: rgba(169, 176, 190, 0.28);
}

.podium-slot--bronze .podium-plate {
  color: var(--bronze);
  border-color: rgba(192, 122, 69, 0.3);
}

.podium-avatar-ring {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  padding: 4px;
  margin-top: 6px;
  border-radius: 50%;
  background: linear-gradient(160deg, var(--silver), rgba(169, 176, 190, 0.14));
  flex-shrink: 0;
}

.podium-slot--bronze .podium-avatar-ring {
  background: linear-gradient(160deg, var(--bronze), rgba(192, 122, 69, 0.14));
}

.podium-avatar-ring--gold {
  width: 80px;
  height: 80px;
  background: linear-gradient(160deg, var(--gold), rgba(216, 178, 92, 0.16));
  box-shadow: 0 0 22px rgba(216, 178, 92, 0.18);
}

.podium-avatar-ring :deep(.el-avatar) {
  border: 2px solid var(--bg-surface-1);
  background: var(--bg-surface-3);
}

.podium-name {
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--text-primary);
}

.podium-points {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
}

.podium-points-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 15px;
  font-weight: 700;
  color: var(--accent-primary);
}

.podium-points-unit {
  font-family: var(--font-mono);
  font-size: 8px;
  letter-spacing: 0.12em;
  color: var(--text-faint);
}

.podium-plinth {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 48px;
  overflow: hidden;
  background-color: var(--bg-surface-2);
  background-image: repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.03) 0 1px, transparent 1px 8px);
  border: 1px solid var(--border-subtle);
  border-top: 0;
  border-radius: 0 0 var(--radius-md) var(--radius-md);
}

.podium-plinth--gold {
  height: 88px;
  background-color: var(--bg-surface-2);
  background-image:
    linear-gradient(180deg, rgba(216, 178, 92, 0.1), transparent 80%),
    repeating-linear-gradient(-45deg, rgba(216, 178, 92, 0.05) 0 1px, transparent 1px 8px);
  border-color: rgba(216, 178, 92, 0.22);
}

.podium-plinth--silver {
  height: 60px;
  background-image:
    linear-gradient(180deg, rgba(169, 176, 190, 0.07), transparent 80%),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.03) 0 1px, transparent 1px 8px);
  border-color: rgba(169, 176, 190, 0.18);
}

.podium-plinth--bronze {
  background-image:
    linear-gradient(180deg, rgba(192, 122, 69, 0.07), transparent 80%),
    repeating-linear-gradient(-45deg, rgba(255, 255, 255, 0.03) 0 1px, transparent 1px 8px);
  border-color: rgba(192, 122, 69, 0.18);
}

.podium-plinth-rank {
  font-family: var(--font-display);
  font-size: 28px;
  line-height: 1;
  letter-spacing: 0.06em;
  color: var(--text-faint);
}

.podium-plinth--gold .podium-plinth-rank {
  font-size: 36px;
  color: var(--gold);
}

.podium-plinth--silver .podium-plinth-rank {
  color: var(--silver);
}

.podium-plinth--bronze .podium-plinth-rank {
  color: var(--bronze);
}

/* ================= EMPTY ================= */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 52px 24px;
  text-align: center;
  background: rgba(255, 255, 255, 0.012);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.empty-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 14px;
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.empty-title {
  margin-bottom: 6px;
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.empty-desc {
  max-width: 320px;
  margin-bottom: 16px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-tertiary);
}

.empty-cta {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 14px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-primary);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
  transition: background var(--duration-fast) ease, border-color var(--duration-fast) ease;
}

.empty-cta:hover {
  background: rgba(255, 70, 85, 0.16);
  border-color: var(--brand);
}

/* ================= SKELETON ================= */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  pointer-events: none;
}

.skeleton-line {
  display: block;
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  border-radius: var(--radius-sm);
  animation: shimmer 1.5s ease-in-out infinite;
}

.sk-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 18px;
}

.sk-chip {
  width: 84px;
  height: 18px;
}

.sk-badge {
  width: 52px;
  height: 16px;
}

.sk-teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.sk-team {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.sk-logo {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
}

.sk-name {
  width: 66px;
  height: 10px;
  max-width: 100%;
}

.sk-vs {
  width: 22px;
  height: 18px;
}

.sk-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
}

.sk-time {
  width: 72px;
  height: 10px;
}

.sk-odds {
  width: 56px;
  height: 16px;
}

.loading-full {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 8px;
}

.match-card--skeleton {
  min-height: 178px;
  cursor: default;
}

.match-card--skeleton:hover {
  transform: none;
  border-color: var(--border-subtle);
  box-shadow: none;
}

.podium--loading .podium-card {
  align-items: center;
  padding-top: 24px;
  padding-bottom: 20px;
}

.sk-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  margin: 0 auto 12px;
}

.sk-avatar--lg {
  width: 72px;
  height: 72px;
}

.sk-rank-name {
  width: 72%;
  height: 12px;
  margin: 0 auto 8px;
}

.sk-rank-pts {
  width: 46%;
  height: 10px;
  margin: 0 auto;
}

/* ================= FLOAT NAV (hidden, kept for rollback) ================= */
.float-nav {
  display: none;
}

.float-nav-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  cursor: pointer;
  box-shadow: var(--shadow-md);
  transition: all var(--duration-fast) ease;
}

.float-nav-btn:hover {
  color: var(--brand);
  border-color: var(--brand-line);
  transform: translateY(-2px);
}

/* ================= RESPONSIVE ================= */
@media (max-width: 1024px) {
  .hero-inner {
    gap: 30px;
    padding: 32px;
  }
  .hero-score {
    font-size: 40px;
  }
  .hero-vs {
    font-size: 36px;
  }
  .card-list,
  .loading-full {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .hero-inner {
    grid-template-columns: minmax(0, 1fr);
    gap: 26px;
  }
  .hero-copy {
    max-width: 560px;
  }
  .hero-title {
    font-size: 34px;
  }
  .home-section {
    margin-bottom: 40px;
  }
  .podium {
    gap: 12px;
  }
  .podium-card {
    padding: 18px 10px 14px;
  }
  .podium-plinth {
    height: 48px;
  }
  .podium-plinth--gold {
    height: 72px;
  }
  .podium-name {
    font-size: 12.5px;
  }
}

@media (max-width: 640px) {
  .hero {
    margin-bottom: 16px;
  }
  .hero-inner {
    padding: 22px 16px;
    gap: 20px;
  }
  .hero-title {
    margin-top: 12px;
    font-size: 30px;
  }
  .hero-sub {
    margin-top: 10px;
    font-size: 12px;
  }
  .hero-actions {
    margin-top: 18px;
  }
  .hero-stats {
    margin-top: 20px;
  }
  .hero-stat {
    padding: 0 14px;
  }
  .hero-stat-value {
    font-size: 16px;
  }
  .hero-feature {
    padding: 16px 14px;
  }
  .hero-feature-head {
    margin-bottom: 14px;
  }
  .hero-side-name {
    font-size: 13px;
  }
  .hero-score {
    font-size: 38px;
  }
  .hero-vs {
    font-size: 34px;
  }
  .hero-countdown-box {
    min-width: 42px;
    padding: 5px 7px;
  }
  .hero-feature-cta {
    flex: 1;
    justify-content: center;
    margin-left: 0;
  }
  .card-list,
  .loading-full {
    grid-template-columns: minmax(0, 1fr);
  }
  .match-card {
    padding: 14px;
  }
  .match-card--live {
    padding-left: 12px;
  }
  .section-head {
    margin-bottom: 16px;
    gap: 10px;
  }
  .section-title {
    font-size: 17px;
  }
  .section-meta {
    font-size: 9.5px;
  }
  .section-head-right {
    gap: 10px;
  }
  .podium {
    gap: 8px;
  }
  .podium-slot--gold {
    padding-top: 20px;
  }
  .podium-crown {
    width: 21px;
    height: 15px;
  }
  .podium-card {
    padding: 16px 7px 12px;
    gap: 5px;
  }
  .podium-plate {
    display: none;
  }
  .podium-name {
    font-size: 11.5px;
  }
  .podium-points-num {
    font-size: 13px;
  }
  .podium-plinth {
    height: 38px;
  }
  .podium-plinth--silver {
    height: 46px;
  }
  .podium-plinth--gold {
    height: 60px;
  }
  .podium-plinth-rank {
    font-size: 22px;
  }
  .podium-plinth--gold .podium-plinth-rank {
    font-size: 28px;
  }
  .home-section {
    margin-bottom: 32px;
  }
}

@media (max-width: 420px) {
  .hero-eyebrow {
    font-size: 9px;
    letter-spacing: 0.16em;
    white-space: normal;
  }
  .hero-feature-tag {
    display: none;
  }
  .hero-feature-left .tournament-chip,
  .mc-head .tournament-chip {
    max-width: 126px;
  }
  .hero-feature-cta {
    font-size: 11px;
  }
  .section-count {
    display: none;
  }
  .mc-time {
    font-size: 9.5px;
  }
  .podium-name {
    font-size: 11px;
  }
}

/* 移动端触控：榜单入口补足可点击高度 */
@media (max-width: 768px) {
  .section-link {
    display: inline-flex;
    align-items: center;
    min-height: 40px;
    padding: 8px 0;
  }
}

</style>
