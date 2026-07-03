<template>
  <div class="home">
    <!-- 新手引导卡片 -->
    <OnboardingCard />
    <!-- 1. HERO 区域 -->
    <div class="hero" :class="heroThemeClass">
      <div class="grain-overlay-hero" aria-hidden="true"></div>
      <div v-if="heroMatch" class="hero-content">
        <div class="hero-tournament">
          <span class="tournament-chip">{{ heroMatch.tournament || '常规赛事' }}</span>
          <span v-if="heroMatch.status === 1" class="status-badge status-badge--live">
            <span class="status-dot status-dot--pulse"></span>进行中
          </span>
          <span v-else-if="heroMatch.status === 0" class="status-badge status-badge--upcoming">
            <span class="status-dot"></span>即将开始
          </span>
        </div>
        <div class="hero-teams">
          <div class="hero-team">
            <div class="team-logo">
              <el-image :src="heroMatch.teamALogo" alt="" fit="contain">
                <template #error><span class="team-logo-placeholder">{{ heroMatch.teamA.charAt(0) }}</span></template>
              </el-image>
            </div>
            <span class="hero-team-name">{{ heroMatch.teamA }}</span>
          </div>
          <div class="hero-center">
            <div v-if="hasScore(heroMatch)" class="hero-score" :class="{ 'score--live': heroMatch.status === 1 }">
              <span>{{ heroMatch.scoreA }}</span>
              <span class="hero-score-sep">:</span>
              <span>{{ heroMatch.scoreB }}</span>
            </div>
            <div v-else class="hero-vs">VS</div>
            <div class="hero-time">{{ formatTime(heroMatch.matchTime) }}</div>
          </div>
          <div class="hero-team">
            <div class="team-logo">
              <el-image :src="heroMatch.teamBLogo" alt="" fit="contain">
                <template #error><span class="team-logo-placeholder">{{ heroMatch.teamB.charAt(0) }}</span></template>
              </el-image>
            </div>
            <span class="hero-team-name">{{ heroMatch.teamB }}</span>
          </div>
        </div>
        <div class="hero-footer">
          <div class="hero-odds" v-if="heroMatch.oddsA !== null && heroMatch.oddsA !== undefined">
            <span class="hero-odds-item">{{ heroMatch.oddsA.toFixed(2) }}</span>
            <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。最终按彩池制结算。">
              <el-icon style="font-size:11px;margin:0 4px;cursor:help;opacity:0.5"><QuestionFilled /></el-icon>
            </el-tooltip>
            <span class="hero-odds-sep">/</span>
            <span class="hero-odds-item">{{ heroMatch.oddsB.toFixed(2) }}</span>
          </div>
          <el-button class="hero-btn" @click="goDetail(heroMatch.id)">立即竞猜</el-button>
        </div>
      </div>
      <div v-else class="hero-empty">
        <div class="hero-empty-icon">🎮</div>
        <div class="hero-empty-text">暂无赛事，管理员正在筹备中</div>
      </div>
    </div>

    <!-- 2. 游戏筛选栏 -->
    <div class="game-tabs" v-if="games.length > 0">
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
          <template #error><span class="img-fallback">{{ game.name.charAt(0) }}</span></template>
        </el-image>
        {{ game.name }}
      </button>
    </div>

    <!-- 3. 公告栏 -->
    <AnnouncementBar :game-id="selectedGameId" />

    <!-- ====== 堆叠分区 ====== -->

    <!-- 🔥 热门赛事 -->
    <div v-if="hotMatches.length > 0" ref="hotSection" class="home-section">
      <div class="section-header">
        <span class="section-header-title">🔥 热门赛事</span>
        <span class="section-header-count">共 {{ hotMatches.length }} 场</span>
      </div>
      <div class="card-list">
        <div
          v-for="match in hotMatches"
          :key="'hot-'+match.id"
          class="match-card"
          :class="{ 'match-card--live': match.status === 1 }"
          @click="goDetail(match.id)"
        >
          <div class="match-card-top">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span v-if="match.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
            <span v-else-if="match.status === 0" class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
            <span v-else class="status-badge status-badge--finished">已结束</span>
          </div>
          <div class="match-card-teams">
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamALogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamA.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamA }}</span>
            </div>
            <div v-if="hasScore(match)" class="match-card-score" :class="{ 'score--live': match.status === 1 }">
              <span>{{ match.scoreA }}</span>
              <span class="match-card-score-sep">:</span>
              <span>{{ match.scoreB }}</span>
            </div>
            <div v-else class="match-card-vs">VS</div>
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamBLogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamB.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamB }}</span>
            </div>
          </div>
          <div class="match-card-footer">
            <span class="match-time">{{ formatTime(match.matchTime) }}</span>
            <div class="match-card-footer-right">
              <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-card-odds">
                {{ match.oddsA.toFixed(2) }}/{{ match.oddsB.toFixed(2) }}
              </span>
              <span v-if="match.betCount" class="match-card-bet-count">👥 {{ match.betCount }}人押注</span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="section-empty">
      <span>暂无赛事</span>
    </div>

    <!-- 🔴 进行中 -->
    <div v-if="liveMatches.length > 0" ref="liveSection" class="home-section">
      <div class="section-header">
        <span class="section-header-title">🔴 进行中的比赛</span>
        <span class="section-header-count">共 {{ liveMatches.length }} 场</span>
      </div>
      <div class="card-list">
        <div
          v-for="match in liveMatches"
          :key="'live-'+match.id"
          class="match-card match-card--live"
          @click="goDetail(match.id)"
        >
          <div class="match-card-top">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
          </div>
          <div class="match-card-teams">
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamALogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamA.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamA }}</span>
            </div>
            <div v-if="hasScore(match)" class="match-card-score score--live">
              <span>{{ match.scoreA }}</span>
              <span class="match-card-score-sep">:</span>
              <span>{{ match.scoreB }}</span>
            </div>
            <div v-else class="match-card-vs">VS</div>
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamBLogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamB.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamB }}</span>
            </div>
          </div>
          <div class="match-card-footer">
            <span class="match-time">{{ formatTime(match.matchTime) }}</span>
            <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-card-odds">
              {{ match.oddsA.toFixed(2) }}/{{ match.oddsB.toFixed(2) }}
            </span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="section-empty">
      <span>暂无赛事</span>
    </div>

    <!-- ⏰ 即将开始 -->
    <div v-if="upcomingMatches.length > 0" ref="upcomingSection" class="home-section">
      <div class="section-header">
        <span class="section-header-title">⏰ 即将开始的比赛</span>
        <span class="section-header-count">共 {{ upcomingMatches.length }} 场</span>
      </div>
      <div class="card-list">
        <div
          v-for="match in upcomingMatches"
          :key="'up-'+match.id"
          class="match-card"
          @click="goDetail(match.id)"
        >
          <div class="match-card-top">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
          </div>
          <div class="match-card-teams">
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamALogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamA.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamA }}</span>
            </div>
            <div class="match-card-vs">VS</div>
            <div class="match-card-team">
              <div class="team-logo">
                <el-image :src="match.teamBLogo" alt="" fit="contain">
                  <template #error><span class="team-logo-placeholder">{{ match.teamB.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="match-card-team-name">{{ match.teamB }}</span>
            </div>
          </div>
          <div class="match-card-footer">
            <span class="match-time">{{ formatTime(match.matchTime) }}</span>
            <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-card-odds">
              {{ match.oddsA.toFixed(2) }}/{{ match.oddsB.toFixed(2) }}
            </span>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="section-empty">
      <span>暂无赛事</span>
    </div>

    <!-- 🏆 排行榜 -->
    <div ref="rankSection" class="home-section">
      <div class="section-header">
        <span class="section-header-title">🏆 预言家排行</span>
        <span class="section-header-count" v-if="leaderboard.length > 0">Top {{ leaderboard.length }}</span>
      </div>
      <template v-if="rankLoading">
        <div class="podium">
          <div class="podium-item silver skeleton">
            <div class="skeleton-line skeleton-line--avatar"></div>
            <div class="skeleton-line skeleton-line--name"></div>
          </div>
          <div class="podium-item gold skeleton">
            <div class="skeleton-line skeleton-line--avatar-lg"></div>
            <div class="skeleton-line skeleton-line--name"></div>
          </div>
          <div class="podium-item bronze skeleton">
            <div class="skeleton-line skeleton-line--avatar"></div>
            <div class="skeleton-line skeleton-line--name"></div>
          </div>
        </div>
      </template>
      <div v-else-if="top3.length === 0 && leaderboard.length === 0" class="empty-state">
        <div class="empty-icon">🏆</div>
        <h3 class="empty-title">排行暂无数据</h3>
        <p class="empty-desc">还没有人参与竞猜，成为第一个上榜的人吧</p>
      </div>
      <template v-else>
        <div v-if="top3.length > 0" class="podium">
          <div v-if="top3[1]" class="podium-item silver">
            <div class="podium-rank">2</div>
            <div class="podium-avatar"><el-avatar :size="56" :icon="UserFilled" /></div>
            <div class="podium-name">{{ top3[1].nickname || top3[1].username }}</div>
            <div class="podium-points">{{ top3[1].points }} 积分</div>
          </div>
          <div v-if="top3[0]" class="podium-item gold">
            <div class="podium-crown">👑</div>
            <div class="podium-rank">1</div>
            <div class="podium-avatar"><el-avatar :size="72" :icon="UserFilled" /></div>
            <div class="podium-name">{{ top3[0].nickname || top3[0].username }}</div>
            <div class="podium-points">{{ top3[0].points }} 积分</div>
          </div>
          <div v-if="top3[2]" class="podium-item bronze">
            <div class="podium-rank">3</div>
            <div class="podium-avatar"><el-avatar :size="56" :icon="UserFilled" /></div>
            <div class="podium-name">{{ top3[2].nickname || top3[2].username }}</div>
            <div class="podium-points">{{ top3[2].points }} 积分</div>
          </div>
        </div>
        <el-table v-if="leaderboard.length > 0" :data="leaderboard" style="width: 100%" class="rank-table">
          <el-table-column label="排名" width="80" align="center">
            <template #default="scope">
              <span :class="getRankClass(scope.$index + 1)">{{ scope.$index + 1 }}</span>
            </template>
          </el-table-column>
          <el-table-column label="昵称" min-width="150">
            <template #default="scope">
              <span>{{ scope.row.nickname || scope.row.username }}</span>
            </template>
          </el-table-column>
          <el-table-column label="积分" width="100" align="center" prop="points" />
          <el-table-column label="胜率" width="100" align="center">
            <template #default="scope">{{ getAccuracy(scope.row) }}%</template>
          </el-table-column>
          <el-table-column label="连胜" width="80" align="center" prop="winStreak" />
        </el-table>
      </template>
    </div>

    <!-- 加载骨架（全空时） -->
    <div v-if="!loaded && hotMatches.length === 0 && liveMatches.length === 0" class="loading-full">
      <div v-for="i in 4" :key="'sk-'+i" class="match-card skeleton">
        <div class="skeleton-line skeleton-line--lg"></div>
        <div class="skeleton-line skeleton-line--md"></div>
        <div class="skeleton-line skeleton-line--sm"></div>
      </div>
    </div>

    <!-- 右侧浮动导航 -->
    <div class="float-nav">
      <button v-if="hotMatches.length > 0" class="float-nav-btn" title="热门赛事" @click="scrollToSection('hot')">🔥</button>
      <button v-if="liveMatches.length > 0" class="float-nav-btn" title="进行中" @click="scrollToSection('live')">🔴</button>
      <button v-if="upcomingMatches.length > 0" class="float-nav-btn" title="即将开始" @click="scrollToSection('upcoming')">⏰</button>
      <button v-if="top3.length > 0 || leaderboard.length > 0" class="float-nav-btn" title="排行榜" @click="scrollToSection('rank')">🏆</button>
      <button class="float-nav-btn" title="回顶部" @click="scrollToTop">⬆</button>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import AnnouncementBar from '@/components/AnnouncementBar.vue'
import OnboardingCard from '@/components/OnboardingCard.vue'
import { markRaw } from 'vue'
import { QuestionFilled, UserFilled } from '@element-plus/icons-vue'

export default {
  name: 'HomePage',
  components: { AnnouncementBar, OnboardingCard, QuestionFilled, UserFilled },
  data() {
    return {
      // 暴露给模板 :icon 绑定的图标组件（用 markRaw 避免响应化）
      UserFilled: markRaw(UserFilled),
      games: [],
      selectedGameId: null,
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
      pollTimer: null
    }
  },
  computed: {
    heroThemeClass() {
      if (this.selectedGameId === 1) return 'hero--valorant'
      if (this.selectedGameId === 2) return 'hero--lol'
      if (this.selectedGameId === 3) return 'hero--dota'
      if (this.selectedGameId === 4) return 'hero--cs2'
      return 'hero--default'
    }
  },
  created() {
    this.fetchGames()
    this.loadAll()
    this.startPolling()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    // ===== 工具 =====
    extractList(res) {
      var data = res.data || res
      if (Array.isArray(data)) return data
      if (data.records) return data.records
      return data.list || data.content || []
    },
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
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return m + '-' + day + ' ' + h + ':' + min
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
    goDetail(id) {
      this.$router.push('/match/' + id)
    },

    // ===== 游戏筛选 =====
    fetchGames() {
      request.get('/game/list').then(function (res) {
        this.games = this.extractList(res)
      }.bind(this)).catch(function () {
        this.games = []
      }.bind(this))
    },
    switchGame(gameId) {
      this.selectedGameId = gameId
      this.loadAll()
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
      var params = { limit: 12 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/hot', { params: params }).then(function (res) {
        this.hotMatches = this.filterTBD(this.extractList(res))
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
      var params = { page: 1, size: 20, status: 1 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/list', { params: params }).then(function (res) {
        this.liveMatches = this.filterTBD(this.extractList(res))
        if (!this.heroMatch) this.updateHero()
      }.bind(this)).catch(function () {
        this.liveMatches = []
      }.bind(this))
    },

    // ===== 即将开始 =====
    fetchUpcoming() {
      var params = { page: 1, size: 20, status: 0 }
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/match/list', { params: params }).then(function (res) {
        this.upcomingMatches = this.filterTBD(this.extractList(res))
        if (!this.heroMatch) this.updateHero()
        this.loaded = true
      }.bind(this)).catch(function () {
        this.upcomingMatches = []
        this.loaded = true
      }.bind(this))
    },

    // ===== 排行榜 =====
    fetchRank() {
      this.rankLoading = true
      var self = this
      var params = {}
      if (this.selectedGameId) params.gameId = this.selectedGameId
      request.get('/leaderboard/top/3', { params: params }).then(function (res) {
        var data = res.data || res
        self.top3 = Array.isArray(data) ? data : []
      }.bind(self)).catch(function () { self.top3 = [] }.bind(self))
      var params2 = { page: 1, size: 10 }
      if (this.selectedGameId) params2.gameId = this.selectedGameId
      request.get('/leaderboard/points', { params: params2 }).then(function (res) {
        this.leaderboard = this.extractList(res)
      }.bind(this)).catch(function () {
        this.leaderboard = []
      }.bind(this)).finally(function () {
        this.rankLoading = false
      }.bind(this))
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
      var refMap = { hot: 'hotSection', live: 'liveSection', upcoming: 'upcomingSection', rank: 'rankSection' }
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
/* ===== HERO 区域 ===== */
.hero {
  height: 240px;
  width: 100%;
  border-radius: var(--radius-xl);
  position: relative;
  overflow: hidden;
  margin-bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.4s var(--ease-out);
  box-shadow:
    0 0 60px rgba(0, 229, 160, 0.06),
    0 8px 32px rgba(0, 0, 0, 0.5),
    inset 0 1px 0 rgba(255, 255, 255, 0.03);
}

.hero--valorant {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(255,77,109,0.30) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(255,181,71,0.18) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(255,77,109,0.10) 0%, transparent 60%),
    #0d0d14;
}

.hero--lol {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(91,140,255,0.30) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(212,168,67,0.20) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(0,229,160,0.10) 0%, transparent 60%),
    #0a0d18;
}

.hero--dota {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(140,91,255,0.30) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(255,77,109,0.20) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(140,91,255,0.12) 0%, transparent 60%),
    #0d0a14;
}

/* CS2 主题：橙金/暗色底（经典 CS 配色） */
.hero--cs2 {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(222,155,40,0.30) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(235,179,52,0.20) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(222,155,40,0.10) 0%, transparent 60%),
    #0d0c09;
}

.hero--default {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(0,229,160,0.25) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(91,140,255,0.15) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(0,229,160,0.08) 0%, transparent 60%),
    var(--bg-page);
}

.grain-overlay-hero {
  position: absolute;
  inset: 0;
  pointer-events: none;
  opacity: 0.04;
  mix-blend-mode: overlay;
  background-image: url("data:image/svg+xml;utf8,<svg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>");
}

.hero-content {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 100%;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
}

.hero-tournament {
  display: flex;
  align-items: center;
  gap: 10px;
}

.hero-teams {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 32px;
}

.hero-team {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.hero-team-name {
  font-family: var(--font-display);
  font-size: 26px;
  color: var(--text-primary);
  letter-spacing: 2px;
  white-space: nowrap;
  text-shadow: 0 0 20px rgba(255, 255, 255, 0.08);
}

.hero-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.hero-score {
  font-family: var(--font-mono);
  font-size: 40px;
  font-weight: 800;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 12px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;
}

.hero-score.score--live {
  color: var(--accent-primary);
  text-shadow: 0 0 24px rgba(0, 229, 160, 0.5), 0 0 60px rgba(0, 229, 160, 0.15);
  animation: score-glow 3s ease-in-out infinite;
}

@keyframes score-glow {
  0%, 100% { text-shadow: 0 0 24px rgba(0, 229, 160, 0.5), 0 0 60px rgba(0, 229, 160, 0.15); }
  50% { text-shadow: 0 0 36px rgba(0, 229, 160, 0.7), 0 0 80px rgba(0, 229, 160, 0.25); }
}

.hero-score-sep {
  opacity: 0.35;
  font-weight: 400;
}

.hero-vs {
  font-family: var(--font-display);
  font-size: 36px;
  color: var(--accent-primary);
  letter-spacing: 4px;
  text-shadow: 0 0 16px rgba(0, 229, 160, 0.3);
}

.hero-time {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-tertiary);
  letter-spacing: 0.5px;
}

.hero-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
}

.hero-odds {
  display: flex;
  align-items: center;
  gap: 8px;
}

.hero-odds-item {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.04);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
}

.hero-odds-sep {
  font-size: 12px;
  color: var(--text-tertiary);
}

.hero-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 700;
  font-size: 14px !important;
  border-radius: var(--radius-md) !important;
  padding: 10px 24px !important;
  box-shadow: 0 0 24px rgba(0, 229, 160, 0.25);
  transition: all 0.2s var(--ease-out);
  letter-spacing: 0.5px;
}

.hero-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 36px rgba(0, 229, 160, 0.4);
  opacity: 1;
}

.hero-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
}

.hero-empty-icon { font-size: 36px; opacity: 0.5; }
.hero-empty-text { font-size: 14px; }

/* ===== 游戏筛选栏 ===== */
.game-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  padding: 4px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.game-tabs::-webkit-scrollbar { display: none; }

.game-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--duration-fast) ease, background var(--duration-fast) ease, border-color var(--duration-fast) ease;
  font-family: var(--font-body);
  flex-shrink: 0;
}
.game-tab:hover { color: var(--text-primary); background: var(--bg-surface-2); }
.game-tab.active {
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.08);
  border-color: rgba(0, 229, 160, 0.20);
  font-weight: 600;
}
.game-icon {
  width: 18px; height: 18px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
}

/* ===== 堆叠分区 ===== */
/* 分区无数据时的空态提示 */
.section-empty {
  text-align: center;
  color: var(--text-tertiary);
  padding: 40px 0;
}

.home-section {
  margin-bottom: 36px;
  animation: fade-in 0.4s var(--ease-out);
}
@keyframes fade-in {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== 分区标题 ===== */
.section-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-bottom: 14px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
  position: relative;
}
.section-header::after {
  content: '';
  position: absolute;
  bottom: -1px; left: 0;
  width: 48px; height: 2px;
  background: var(--accent-primary);
  border-radius: 1px;
}
.section-header-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  font-family: var(--font-display);
  letter-spacing: 1px;
}
.section-header-count {
  font-size: 13px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* ===== 卡片列表 ===== */
.card-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.match-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 18px 20px;
  cursor: pointer;
  transition: transform 0.25s var(--ease-out), border-color 0.25s var(--ease-out), box-shadow 0.25s var(--ease-out);
  position: relative;
  overflow: hidden;
  animation: card-rise 0.5s var(--ease-out) both;
}
.match-card:nth-child(1) { animation-delay: 0ms; }
.match-card:nth-child(2) { animation-delay: 30ms; }
.match-card:nth-child(3) { animation-delay: 60ms; }
.match-card:nth-child(4) { animation-delay: 90ms; }
.match-card:nth-child(5) { animation-delay: 120ms; }
.match-card:nth-child(6) { animation-delay: 150ms; }
.match-card:nth-child(7) { animation-delay: 180ms; }
.match-card:nth-child(8) { animation-delay: 210ms; }

@keyframes card-rise {
  from { opacity: 0; transform: translateY(12px); }
  to   { opacity: 1; transform: translateY(0); }
}

.match-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: inherit;
  background: radial-gradient(ellipse at 50% 0%, rgba(255, 255, 255, 0.03), transparent 70%);
  opacity: 0;
  transition: opacity 0.25s ease;
}
.match-card:hover { transform: translateY(-3px); border-color: var(--border-strong); box-shadow: var(--shadow-lg); }
.match-card:hover::before { opacity: 1; }

.match-card--live {
  border-left: 3px solid var(--accent-primary);
  background: linear-gradient(135deg, rgba(0,229,160,0.05) 0%, var(--bg-surface-1) 40%);
  box-shadow: 0 0 20px rgba(0, 229, 160, 0.04);
}
.match-card--live:hover { box-shadow: 0 0 32px rgba(0, 229, 160, 0.08), var(--shadow-lg); }

.match-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.match-card-teams {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 20px;
  margin-bottom: 12px;
}
.match-card-team {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.match-card-team-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.match-card-score {
  font-family: var(--font-mono);
  font-size: 22px;
  font-weight: 800;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}
.match-card-score.score--live {
  color: var(--accent-primary);
  text-shadow: 0 0 16px rgba(0, 229, 160, 0.35);
  animation: live-pulse-glow 2s ease-in-out infinite;
}
@keyframes live-pulse-glow {
  0%, 100% { text-shadow: 0 0 16px rgba(0, 229, 160, 0.35); }
  50% { text-shadow: 0 0 28px rgba(0, 229, 160, 0.55); }
}
.match-card-score-sep { opacity: 0.5; font-weight: 400; }

.match-card-vs {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--accent-primary);
  letter-spacing: 2px;
  flex-shrink: 0;
}

.match-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.match-card-footer-right {
  display: flex;
  align-items: center;
  gap: 8px;
}
.match-card-odds {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-tertiary);
}
.match-card-bet-count {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}
.match-time {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
}

/* ===== team-logo ===== */
.team-logo {
  width: 32px; height: 32px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.team-logo img {
  width: 100%; height: 100%;
  object-fit: contain;
}
.team-logo-placeholder {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent-primary);
}

/* ===== 排行榜 Podium ===== */
.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 32px;
  padding: 24px 0;
}
.podium-item {
  background: var(--bg-surface-1);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  text-align: center;
  min-width: 140px;
  position: relative;
  transition: transform var(--duration-normal) var(--ease-out), box-shadow var(--duration-normal) var(--ease-out);
}
.podium-item:hover { transform: translateY(-4px); }
.podium-item.gold {
  border: 2px solid #d4a843;
  padding-bottom: 28px;
  min-width: 160px;
  box-shadow: 0 0 24px rgba(212,168,67,0.15), 0 8px 24px rgba(0,0,0,0.4);
}
.podium-item.silver {
  border: 2px solid #a0a0a0;
  box-shadow: 0 0 16px rgba(160,160,160,0.10), 0 4px 16px rgba(0,0,0,0.35);
}
.podium-item.bronze {
  border: 2px solid #c47a3a;
  box-shadow: 0 0 16px rgba(196,122,58,0.10), 0 4px 16px rgba(0,0,0,0.35);
}
.podium-crown { font-size: 28px; margin-bottom: 4px; }
.podium-rank { font-family: var(--font-display); font-size: 24px; margin-bottom: 8px; }
.gold .podium-rank { color: #d4a843; }
.silver .podium-rank { color: #a0a0a0; }
.bronze .podium-rank { color: #c47a3a; }
.podium-name { color: var(--text-primary); font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.podium-points {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.rank-gold { color: #d4a843; font-family: var(--font-display); font-size: 18px; font-weight: 700; }
.rank-silver { color: #a0a0a0; font-family: var(--font-display); font-size: 18px; font-weight: 700; }
.rank-bronze { color: #c47a3a; font-family: var(--font-display); font-size: 18px; font-weight: 700; }
.rank-table { margin-top: 8px; }

/* ===== 空态 ===== */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
}
.empty-icon { font-size: 48px; margin-bottom: 16px; opacity: 0.6; }
.empty-title { font-size: 18px; font-weight: 600; color: var(--text-secondary); margin-bottom: 8px; }
.empty-desc { font-size: 14px; color: var(--text-tertiary); max-width: 300px; }

/* ===== Skeleton 加载 ===== */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}
.skeleton { pointer-events: none; }
.skeleton .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}
.skeleton-line--lg { width: 80%; height: 32px; margin-bottom: 12px; }
.skeleton-line--md { width: 60%; height: 20px; margin-bottom: 8px; }
.skeleton-line--sm { width: 40%; height: 14px; }
.skeleton-line--avatar { width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 8px; }
.skeleton-line--avatar-lg { width: 72px; height: 72px; border-radius: 50%; margin: 0 auto 8px; }
.skeleton-line--name { width: 80px; height: 14px; margin: 0 auto; }

.loading-full {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

/* ===== 右侧浮动导航 ===== */
.float-nav {
  position: fixed;
  right: 12px;
  bottom: 40px;
  width: 40px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 200;
}
.float-nav-btn {
  width: 40px; height: 40px;
  border-radius: var(--radius-full);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-fast) ease;
  box-shadow: var(--shadow-md);
}
.float-nav-btn:hover {
  background: var(--bg-surface-3);
  border-color: var(--border-strong);
  transform: translateY(-2px);
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .card-list, .loading-full { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 768px) {
  .card-list, .loading-full { grid-template-columns: repeat(2, 1fr); }
  .hero { height: 200px; }
  .hero-content { padding: 14px 12px; gap: 10px; }
  .hero-teams { gap: 16px; }
  .hero-team-name { font-size: 20px; }
  .hero-score { font-size: 32px; }
  .hero-vs { font-size: 28px; }
  /* hero 底部按钮区允许换行，避免溢出 */
  .hero-footer { gap: 12px; flex-wrap: wrap; }
  .hero-btn { padding: 8px 18px !important; font-size: 13px !important; }
  .podium { gap: 10px; }
  .podium-item { min-width: 100px; padding: 14px 12px; }
  .podium-item.gold { min-width: 120px; }
  /* 分区标题与卡片收紧 */
  .home-section { margin-bottom: 24px; }
  .section-header { margin-bottom: 14px; padding-bottom: 10px; }
  .section-header-title { font-size: 16px; }
  .match-card { padding: 14px 14px; }
  .match-card-teams { gap: 12px; }
  .match-card-team-name { font-size: 14px; }
  /* 浮动导航缩小，避免遮挡内容 */
  .float-nav { right: 8px; bottom: 24px; }
  .float-nav-btn { width: 36px; height: 36px; font-size: 16px; }
}
@media (max-width: 480px) {
  .card-list, .loading-full { grid-template-columns: 1fr; }
  .hero { height: 170px; }
  .hero-teams { gap: 16px; }
  .hero-team-name { font-size: 16px; }
  .hero-score { font-size: 26px; }
  .hero-vs { font-size: 22px; }
  /* 长队名允许换行，避免横向溢出 */
  .hero-team-name { white-space: normal; text-align: center; line-height: 1.2; }
  .hero-team { gap: 8px; }
}
</style>
