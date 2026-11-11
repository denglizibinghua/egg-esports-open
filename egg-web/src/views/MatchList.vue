<template>
  <div class="match-list">
    <!-- 共享页头：红色眉标 + 中文 H1 + 副标题 + 右侧场次统计 -->
    <header class="page-header">
      <div class="page-head-main">
        <div class="page-eyebrow">// MATCHES · 02</div>
        <h1 class="page-title">赛事列表</h1>
        <p class="page-sub">实时赔率 · 纯彩池 · <span class="page-sub-accent">0% 抽水</span></p>
      </div>
      <div class="page-count">
        <div class="page-count-row">
          <span class="page-count-pre">共</span>
          <span class="page-count-value">{{ total || matches.length }}</span>
          <span class="page-count-unit">场</span>
        </div>
        <span class="page-count-sub">LIVE ODDS FEED</span>
      </div>
    </header>

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
          <template #error><span class="img-fallback">{{ game.name.charAt(0) }}</span></template>
        </el-image>
        {{ game.name }}
      </button>
    </div>

    <AnnouncementBar :game-id="selectedGameId" />

    <!-- 热门赛事：3 列特色条带 -->
    <section v-if="hotMatches.length > 0" class="hot-section">
      <div class="section-head">
        <div class="section-head-main">
          <span class="section-index">01</span>
          <h2 class="section-title">热门赛事</h2>
          <span class="section-meta">{{ hotMatches.length }} 场 · 精选</span>
        </div>
        <span class="section-count">HOT PICKS</span>
      </div>

      <div class="hot-grid">
        <div
          v-for="match in hotMatches"
          :key="'hot-' + match.id"
          class="hot-card card-entrance"
          :class="{ 'hot-card--live': match.status === 1 }"
          @click="goDetail(match.id)"
        >
          <span class="hot-flag" aria-hidden="true">HOT</span>

          <div class="hot-top">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span v-if="match.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
            <span v-else-if="match.status === 0" class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
          </div>

          <div class="hot-teams">
            <div class="hot-team">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamALogo)" alt="" fit="contain" lazy>
                  <template #error><span class="team-logo-placeholder">{{ match.teamA === 'TBD' ? '?' : match.teamA.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="hot-team-name" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
            </div>

            <div v-if="hasScore(match)" class="score-display score-display--sm" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
              <span class="score-a">{{ match.scoreA }}</span>
              <span class="score-sep">:</span>
              <span class="score-b">{{ match.scoreB }}</span>
            </div>
            <div v-else class="vs-badge vs-badge--sm">VS</div>

            <div class="hot-team hot-team--reverse">
              <div class="team-logo">
                <el-image :src="fixLogoUrl(match.teamBLogo)" alt="" fit="contain" lazy>
                  <template #error><span class="team-logo-placeholder">{{ match.teamB === 'TBD' ? '?' : match.teamB.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="hot-team-name" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
            </div>
          </div>

          <div class="hot-foot">
            <span class="match-time">{{ formatTime(match.matchTime) }}</span>
            <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-odds-capsule">{{ match.oddsA.toFixed(2) }} / {{ match.oddsB.toFixed(2) }}</span>
          </div>

          <div v-if="match.status === 2 && getMapScores(match).length" class="map-chips map-chips--sm">
            <span v-for="(m, idx) in getMapScores(match)" :key="idx" class="map-chip" :class="{ 'map-chip--win': m.winner !== null && m.winner !== undefined }">{{ m.map }} {{ m.scoreA }}-{{ m.scoreB }}</span>
          </div>

          <div class="hot-card-odds" v-if="false && match.oddsA !== null && match.oddsA !== undefined">
            <span class="odds-item odds-item--a">{{ match.oddsA.toFixed(2) }}</span>
            <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
              <el-icon style="font-size:10px;margin:0 4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
            </el-tooltip>
            <span class="odds-vs">/</span>
            <span class="odds-item odds-item--b">{{ match.oddsB.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 状态分段筛选：保留 el-tabs 的 v-model / 事件语义 -->
    <div class="filter-bar">
      <span class="filter-label">STATUS</span>
      <el-tabs v-model="activeStatus" class="match-filter" @tab-click="handleTabChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="即将开始" name="0" />
        <el-tab-pane label="进行中" name="1" />
        <el-tab-pane label="已结束" name="2" />
      </el-tabs>
    </div>

    <!-- 赛程：按日期分组的粘性列表 -->
    <section class="schedule">
      <div class="section-head">
        <div class="section-head-main">
          <span class="section-index">02</span>
          <h2 class="section-title">赛程</h2>
          <span class="section-meta">{{ matches.length }} 场</span>
        </div>
        <span class="section-count">SCHEDULE</span>
      </div>

      <div class="match-cards">
        <!-- Loading Skeleton -->
        <template v-if="loading">
          <div v-for="i in 5" :key="'skeleton-'+i" class="skel-card">
            <div class="skel-row">
              <span class="skel skel-chip"></span>
              <span class="skel skel-meta"></span>
            </div>
            <div class="skel-teams">
              <div class="skel-team">
                <span class="skel skel-logo"></span>
                <span class="skel skel-name"></span>
              </div>
              <span class="skel skel-score"></span>
              <div class="skel-team skel-team--reverse">
                <span class="skel skel-logo"></span>
                <span class="skel skel-name"></span>
              </div>
            </div>
            <div class="skel-row skel-row--foot">
              <span class="skel skel-badge"></span>
              <span class="skel skel-foot"></span>
            </div>
          </div>
        </template>

        <!-- Empty State -->
        <div v-else-if="matches.length === 0" class="empty-state">
          <div class="empty-mark" aria-hidden="true">//</div>
          <h3 class="empty-title">{{ emptyTitle }}</h3>
          <p class="empty-desc">{{ emptyDesc }}</p>
          <el-button v-if="isAdminUser" type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
          <el-button v-else type="primary" class="empty-cta" @click="$router.push('/')">回首页看看</el-button>
        </div>

        <!-- Date Groups — VLR calendar -->
        <template v-else>
          <div
            v-for="group in dateGroups"
            :key="'dg-' + group.date"
            class="date-group"
          >
            <div class="date-group-header" :class="{ 'date-group-header--live': group.hasLive }">
              <span class="date-group-date">{{ group.label }}</span>
              <span v-if="group.hasLive" class="badge-live">LIVE</span>
              <span class="date-group-count">{{ group.matches.length }} 场</span>
            </div>

            <div class="date-group-body">
              <div
                v-for="match in group.matches"
                :key="match.id"
                class="match-card card-entrance"
                :class="{ 'match-card--live': match.status === 1 }"
                @click="goDetail(match.id)"
              >
                <div class="mc-top">
                  <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
                  <div class="mc-top-meta">
                    <span class="match-time">{{ formatTime(match.matchTime) }}</span>
                    <span v-if="match.oddsA !== null && match.oddsA !== undefined" class="match-odds-capsule">{{ match.oddsA.toFixed(2) }} / {{ match.oddsB.toFixed(2) }}</span>
                  </div>
                </div>

                <div class="mc-teams">
                  <div class="mc-team mc-team--a">
                    <div class="team-logo">
                      <el-image :src="fixLogoUrl(match.teamALogo)" alt="" fit="contain" lazy>
                        <template #error><span class="team-logo-placeholder">{{ match.teamA === 'TBD' ? '?' : match.teamA.charAt(0) }}</span></template>
                      </el-image>
                    </div>
                    <span class="team-name" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
                  </div>

                  <div class="mc-center">
                    <div v-if="hasScore(match)" class="score-display" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
                      <span class="score-a">{{ match.scoreA }}</span>
                      <span class="score-sep">:</span>
                      <span class="score-b">{{ match.scoreB }}</span>
                    </div>
                    <div v-else class="vs-badge">VS</div>
                  </div>

                  <div class="mc-team mc-team--b">
                    <div class="team-logo">
                      <el-image :src="fixLogoUrl(match.teamBLogo)" alt="" fit="contain" lazy>
                        <template #error><span class="team-logo-placeholder">{{ match.teamB === 'TBD' ? '?' : match.teamB.charAt(0) }}</span></template>
                      </el-image>
                    </div>
                    <span class="team-name" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
                  </div>
                </div>

                <div class="match-footer">
                  <span v-if="match.status === 0" class="status-badge status-badge--upcoming">
                    <span class="status-dot"></span>未开始
                  </span>
                  <span v-else-if="match.status === 1" class="status-badge status-badge--live">
                    <span class="status-dot status-dot--pulse"></span>进行中
                  </span>
                  <span v-else-if="match.status === 2" class="status-badge status-badge--finished">已结束</span>

                  <span v-if="match.status === 2 && match.result !== null && match.result !== undefined" class="match-result">
                    <template v-if="match.result === 0">{{ match.teamA === 'TBD' ? '待定' : match.teamA }} 获胜!</template>
                    <template v-else-if="match.result === 1">{{ match.teamB === 'TBD' ? '待定' : match.teamB }} 获胜!</template>
                    <template v-else-if="match.result === 2">平局</template>
                  </span>
                </div>

                <div v-if="match.status === 2 && getMapScores(match).length" class="map-chips map-chips--sm">
                  <span
                    v-for="(m, idx) in getMapScores(match)"
                    :key="idx"
                    class="map-chip"
                    :class="{ 'map-chip--win': m.winner !== null && m.winner !== undefined }"
                  >{{ m.map }} {{ m.scoreA }}-{{ m.scoreB }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <el-pagination
      v-if="total > 0 && activeStatus !== 'all'"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="fetchMatches"
    />
  </div>
</template>

<script>
import request from '@/utils/request'
import { isAdmin } from '@/utils/auth'
import AnnouncementBar from '@/components/AnnouncementBar.vue'
import { QuestionFilled } from '@element-plus/icons-vue'
// 时间格式化工具（MM-DD HH:mm），统一提取自 @/utils/format
import { formatTime, fixLogoUrl } from '@/utils/format'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'

export default {
  name: 'MatchList',
  components: {
    AnnouncementBar,
    QuestionFilled
  },
  data() {
    return {
      activeStatus: 'all',
      matches: [],
      loading: false,
      hotMatches: [],
      hotLoading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      pollTimer: null,
      collapsedTournaments: [],
      games: [],
      selectedGameId: 1,
      gamesLoading: false
    }
  },
  computed: {
    isAdminUser() {
      return isAdmin()
    },
    emptyTitle() {
      const map = {
        all: '暂无赛事',
        '0': '暂无即将开始的比赛',
        '1': '当前没有进行中的比赛',
        '2': '暂无已结束的比赛'
      }
      return map[this.activeStatus] || '暂无赛事'
    },
    emptyDesc() {
      const map = {
        all: '管理员正在筹备中，稍后回来',
        '0': '等待管理员录入赛程',
        '1': '比赛还未开始，先看看赛程吧',
        '2': '比赛结束后会在这里展示结果'
      }
      return map[this.activeStatus] || ''
    },
    tournamentGroups() {
      var groups = {}
      var order = []
      this.matches.forEach(function (match) {
        var key = match.tournament || '常规赛事'
        if (!groups[key]) {
          groups[key] = []
          order.push(key)
        }
        groups[key].push(match)
      })
      // Sort by earliest matchTime in each group
      order.sort(function (a, b) {
        var timeA = groups[a][0] && groups[a][0].matchTime ? new Date(groups[a][0].matchTime).getTime() : Infinity
        var timeB = groups[b][0] && groups[b][0].matchTime ? new Date(groups[b][0].matchTime).getTime() : Infinity
        return timeA - timeB
      })
      return order.map(function (name) {
        return { name: name, matches: groups[name] }
      })
    },
    dateGroups() {
      var groups = {}
      var order = []
      this.matches.forEach(function (match) {
        var d = match.matchTime ? new Date(match.matchTime) : null
        var key = d ? d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0') : '待定'
        if (!groups[key]) {
          groups[key] = []
          order.push(key)
        }
        groups[key].push(match)
      })
      // 按日期倒序（近日期在前），待定沉底
      order.sort(function (a, b) {
        if (a === '待定') return 1
        if (b === '待定') return -1
        return b.localeCompare(a)
      })
      var self = this
      return order.map(function (date) {
        var list = groups[date].slice().sort(function (a, b) {
          var ta = a.matchTime ? new Date(a.matchTime).getTime() : 0
          var tb = b.matchTime ? new Date(b.matchTime).getTime() : 0
          return tb - ta
        })
        var hasLive = list.some(function (m) { return m.status === 1 })
        var label = date === '待定' ? '待定' : self.formatDateLabel(date)
        return { date: date, label: label, matches: list, hasLive: hasLive }
      })
    }
  },
  created() {
    var q = this.$route && this.$route.query && this.$route.query.gameId
    if (q !== undefined && q !== null && q !== '') {
      var parsed = Number(q)
      if (!isNaN(parsed)) this.selectedGameId = parsed
    }
    this.fetchGames()
    this.fetchMatches()
    this.fetchHotMatches()
    this.startPolling()
  },
  beforeUnmount() {
    this.stopPolling()
  },
  methods: {
    fetchGames() {
      this.gamesLoading = true
      request.get('/game/list').then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.games = extractList(data)
      }.bind(this)).catch(function () {
        this.games = []
      }.bind(this)).finally(function () {
        this.gamesLoading = false
      }.bind(this))
    },
    switchGame(gameId) {
      this.selectedGameId = gameId
      this.currentPage = 1
      this.fetchMatches()
      this.fetchHotMatches()
    },
    hasScore(match) {
      return (match.status === 1 || match.status === 2) &&
        match.scoreA !== null && match.scoreA !== undefined &&
        match.scoreB !== null && match.scoreB !== undefined
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
    formatDateLabel(isoDate) {
      // isoDate YYYY-MM-DD -> "MM-DD 周X" 保留年仅当跨年
      var parts = isoDate.split('-')
      var m = parts[1] + '-' + parts[2]
      var d = new Date(isoDate + 'T00:00:00')
      var week = ['周日','周一','周二','周三','周四','周五','周六']
      return m + ' ' + week[d.getDay()]
    },
    isTournamentExpanded(name) {
      return this.collapsedTournaments.indexOf(name) === -1
    },
    toggleTournament(name) {
      var idx = this.collapsedTournaments.indexOf(name)
      if (idx >= 0) {
        this.collapsedTournaments.splice(idx, 1)
      } else {
        this.collapsedTournaments.push(name)
      }
    },
    startPolling() {
      var self = this
      this.pollTimer = setInterval(function () {
        if (!document.hidden) {
          self.fetchMatches(true)
          self.fetchHotMatches(true)
        }
      }, 30000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    fetchMatches(silent) {
      if (!silent) this.loading = true
      var params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.activeStatus !== 'all') {
        params.status = this.activeStatus
      }
      if (this.selectedGameId !== null && this.selectedGameId !== '' && this.selectedGameId !== undefined) {
        params.gameId = this.selectedGameId
      } else {
        params.gameId = 1
      }
      request.get('/match/list', { params }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.matches = extractList(data)
        if (Array.isArray(data)) {
          this.total = data.length
        } else if (data.records) {
          this.total = data.total || data.records.length
        } else {
          this.total = data.total || data.totalElements || 0
        }
      }.bind(this)).catch(() => { this.$message.error('加载失败，请刷新重试') }).finally(function () {
        if (!silent) this.loading = false
      }.bind(this))
    },
    handleTabChange(tab) {
      this.currentPage = 1
      this.matches = []
      // tab.paneName 是点击的 tab 的 name 值，比 this.activeStatus 更可靠
      // 因为 @tab-click 可能在 v-model 更新前触发，直接用事件参数避免读到旧值
      this.activeStatus = tab.paneName
      this.fetchMatches()
    },
    fetchHotMatches(silent) {
      if (!silent) this.hotLoading = true
      var params = { limit: 6 }
      if (this.selectedGameId !== null && this.selectedGameId !== '' && this.selectedGameId !== undefined) {
        params.gameId = this.selectedGameId
      } else {
        params.gameId = 1
      }
      request.get('/match/hot', { params: params }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.hotMatches = extractList(data)
      }.bind(this)).catch(() => { this.$message.error('加载失败，请刷新重试') }).finally(function () {
        if (!silent) this.hotLoading = false
      }.bind(this))
    },
    goDetail(id) {
      this.$router.push('/match/' + id)
    },
    // formatTime 已外移到 @/utils/format，此处通过 ES6 简写暴露给模板
    formatTime,
    fixLogoUrl
  }
}
</script>

<style scoped>
/* ── 页头 ── */
.page-header {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
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

.page-head-main {
  min-width: 0;
}

.page-eyebrow {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
}

.page-title {
  margin-top: 8px;
  font-family: var(--font-body);
  font-size: 34px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.page-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
  letter-spacing: 0.01em;
}

.page-sub-accent {
  color: var(--accent-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.page-count {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  flex-shrink: 0;
}

.page-count-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.page-count-pre,
.page-count-unit {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  letter-spacing: 0.08em;
}

.page-count-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 26px;
  font-weight: 700;
  line-height: 1;
  color: var(--text-primary);
}

.page-count-sub {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-faint);
  text-transform: uppercase;
}

/* ── 游戏切换（保留 Adapter，隐藏状态） ── */
.game-tabs {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 20px;
  padding: 4px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow-x: auto;
  scrollbar-width: none;
}

.game-tabs::-webkit-scrollbar {
  display: none;
}

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

.game-tab:hover {
  color: var(--text-primary);
  background: var(--bg-surface-2);
}

.game-tab.active {
  color: var(--accent-primary);
  background: var(--accent-primary-soft);
  border-color: var(--border-default);
  font-weight: 600;
}

.game-icon {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
}

/* ── 区块标题 ── */
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 10px;
  margin-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.section-head-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
}

.section-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--brand);
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  white-space: nowrap;
}

.section-meta {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.section-count {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ── 热门 3 列条带 ── */
.hot-section {
  margin-bottom: 26px;
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hot-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 16px 14px;
  background-color: var(--bg-surface-1);
  background-image: linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 58%);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  overflow: hidden;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.hot-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--brand), var(--brand-soft) 46%, transparent 82%);
}

.hot-card:hover {
  transform: translateY(-3px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-lg);
}

.hot-card--live {
  border-color: var(--brand-line);
  background-image: linear-gradient(90deg, var(--brand-soft) 0%, transparent 42%),
                    linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 58%);
}

.hot-flag {
  position: absolute;
  top: 0;
  left: 14px;
  z-index: 1;
  height: 15px;
  padding: 0 8px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  line-height: 15px;
  letter-spacing: 0.16em;
  color: var(--cream);
  background: var(--brand);
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
}

.hot-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.hot-top .tournament-chip {
  min-width: 0;
  max-width: 100%;
  flex: 0 1 auto;
}

.hot-teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 8px;
}

.hot-team {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.hot-team--reverse {
  flex-direction: row-reverse;
  text-align: right;
}

.hot-team-name {
  min-width: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.hot-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

/* ── 状态筛选（EP tabs → 分段控件） ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
}

.filter-label {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-faint);
}

.match-filter {
  min-width: 0;
}

.match-filter :deep(.el-tabs__header) {
  margin: 0;
}

.match-filter :deep(.el-tabs__nav-wrap)::after {
  display: none;
}

.match-filter :deep(.el-tabs__nav-wrap) {
  margin-bottom: 0;
}

.match-filter :deep(.el-tabs__nav-wrap.is-scrollable) {
  padding: 0;
}

.match-filter :deep(.el-tabs__nav-prev),
.match-filter :deep(.el-tabs__nav-next) {
  display: none;
}

.match-filter :deep(.el-tabs__nav) {
  display: inline-flex;
  gap: 3px;
  float: none;
  padding: 4px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.match-filter :deep(.el-tabs__active-bar) {
  display: none;
}

.match-filter :deep(.el-tabs__item) {
  height: 32px !important;
  line-height: 32px !important;
  padding: 0 16px !important;
  font-family: var(--font-body) !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast) ease, background var(--duration-fast) ease;
}

.match-filter :deep(.el-tabs__item:hover) {
  color: var(--text-primary) !important;
  background: var(--bg-surface-2);
}

.match-filter :deep(.el-tabs__item.is-active) {
  color: var(--cream) !important;
  background: var(--brand);
  font-weight: 700 !important;
  box-shadow: 0 3px 12px var(--brand-soft);
}

.match-filter :deep(.el-tabs__nav-scroll) {
  overflow-x: auto;
  scrollbar-width: none;
}

.match-filter :deep(.el-tabs__nav-scroll)::-webkit-scrollbar {
  display: none;
}

/* ── 赛程列表 ── */
.schedule {
  min-width: 0;
}

.match-cards {
  min-height: 200px;
}

.date-group {
  margin-bottom: 20px;
}

.date-group:last-child {
  margin-bottom: 0;
}

.date-group-header {
  position: sticky;
  top: var(--header-h);
  z-index: 20;
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  padding: 8px 14px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-left: 3px solid var(--border-default);
  border-radius: var(--radius-sm);
  user-select: none;
}

.date-group-header--live {
  border-left-color: var(--brand);
  background: linear-gradient(90deg, var(--brand-soft) 0%, var(--bg-surface-2) 52%);
}

.date-group-date {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--text-primary);
  white-space: nowrap;
}

.date-group-count {
  margin-left: auto;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
  white-space: nowrap;
  flex-shrink: 0;
}

.date-group-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* ── 比赛卡片 ── */
.match-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  background-color: var(--bg-surface-1);
  background-image: linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 52%);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid transparent;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.match-card:hover {
  transform: translateY(-2px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-md);
}

.match-card--live {
  border-left-color: var(--brand);
  background-image: linear-gradient(90deg, var(--brand-soft) 0%, transparent 38%),
                    linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 52%);
}

/* 保留入场动画但结束后释放 transform，避免压住 hover */
.match-card.card-entrance,
.hot-card.card-entrance {
  animation-fill-mode: backwards;
}

.mc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  min-width: 0;
}

.mc-top .tournament-chip {
  min-width: 0;
  max-width: 45%;
  flex: 0 1 auto;
}

.mc-top-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.match-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11px;
  letter-spacing: 0.02em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.mc-teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.mc-team {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.mc-team--b {
  flex-direction: row-reverse;
  text-align: right;
}

.team-logo {
  width: 44px;
  height: 44px;
  padding: 5px;
  border-radius: var(--radius-md);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.team-logo .el-image {
  width: 100%;
  height: 100%;
}

.team-logo-placeholder {
  font-family: var(--font-display);
  font-size: 15px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
}

.team-name {
  min-width: 0;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  overflow: hidden;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.mc-center {
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 56px;
  flex-shrink: 0;
}

.score-display {
  display: flex;
  align-items: baseline;
  gap: 2px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  letter-spacing: -0.02em;
}

.score-display--sm {
  font-size: 16px;
}

.score--live {
  color: var(--brand);
  text-shadow: 0 0 16px var(--brand-line);
}

.score--finished {
  color: var(--text-primary);
}

.score-a,
.score-b {
  font-variant-numeric: tabular-nums;
}

.score-sep {
  padding: 0 1px;
  font-weight: 400;
  color: var(--text-faint);
}

.vs-badge {
  font-family: var(--font-display);
  font-size: 16px;
  line-height: 1;
  letter-spacing: 0.12em;
  color: var(--text-tertiary);
  padding: 4px 7px 3px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: var(--bg-surface-2);
}

.vs-badge--sm {
  font-size: 13px;
  padding: 3px 6px 2px;
}

.match-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

.match-result {
  display: block;
  max-width: 62%;
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* TBD Team Name */
.team-name-tbd {
  color: var(--text-tertiary);
  font-style: italic;
}

/* 地图 chips：跟随卡片栅格 */
.match-card .map-chips {
  margin-top: -2px;
}

/* 已隐藏的旧赔率块保留 DOM，不占位 */
.hot-card-odds {
  display: none;
}

/* ── 骨架屏 ── */
.skel-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid transparent;
  border-radius: var(--radius-md);
  pointer-events: none;
}

.skel {
  display: block;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 50%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: skel-shimmer 1.6s ease-in-out infinite;
}

@keyframes skel-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.skel-row--foot {
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

.skel-chip { width: 92px; height: 18px; }
.skel-meta { width: 128px; height: 12px; }
.skel-badge { width: 64px; height: 18px; }
.skel-foot { width: 86px; height: 12px; }

.skel-teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: center;
  gap: 12px;
}

.skel-team {
  display: flex;
  align-items: center;
  gap: 10px;
}

.skel-team--reverse {
  flex-direction: row-reverse;
}

.skel-logo {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

.skel-name {
  width: 88px;
  height: 14px;
}

.skel-score {
  width: 52px;
  height: 20px;
}

/* ── 空状态 ── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 56px 24px;
  background: var(--bg-surface-1);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.empty-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 18px;
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0.06em;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  max-width: 320px;
  margin-bottom: 22px;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-tertiary);
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .page-title {
    font-size: 28px;
  }

  .hot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .section-title {
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .page-title {
    font-size: 26px;
  }

  .page-count {
    flex-direction: row;
    align-items: baseline;
    gap: 8px;
  }

  .page-count-sub {
    display: none;
  }

  .hot-grid {
    display: flex;
    grid-template-columns: none;
    gap: 12px;
    overflow-x: auto;
    scroll-snap-type: x mandatory;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    margin: 0 -2px;
    padding: 0 2px 6px;
  }

  .hot-grid::-webkit-scrollbar {
    display: none;
  }

  .hot-card {
    flex: 0 0 82%;
    scroll-snap-align: start;
  }

  .filter-bar {
    gap: 0;
  }

  .filter-label {
    display: none;
  }

  .match-filter {
    width: 100%;
  }

  .match-filter :deep(.el-tabs__item) {
    padding: 0 12px !important;
    font-size: 12px !important;
  }

  .section-meta {
    display: none;
  }

  .match-card {
    padding: 12px 12px;
    gap: 10px;
  }

  .mc-teams {
    gap: 8px;
  }

  .mc-team {
    gap: 8px;
  }

  .team-logo {
    width: 38px;
    height: 38px;
    padding: 4px;
  }

  .team-name {
    font-size: 13.5px;
  }

  .mc-center {
    min-width: 48px;
  }

  .score-display {
    font-size: 20px;
  }

  .mc-top .tournament-chip {
    max-width: 40%;
  }

  .match-result {
    max-width: 58%;
    font-size: 11.5px;
  }

  .date-group-header {
    padding: 7px 12px;
  }

  .skel-logo {
    width: 38px;
    height: 38px;
  }

  .skel-name {
    width: 64px;
  }
}
</style>
