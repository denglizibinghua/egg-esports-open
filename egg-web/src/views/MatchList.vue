<template>
  <div class="match-list">
    <div class="page-header">
      <h2 class="page-title">赛事列表</h2>
    </div>

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

    <AnnouncementBar :game-id="selectedGameId" />

    <div v-if="hotMatches.length > 0" class="hot-section">
      <div class="hot-header">
        <h3 class="hot-title">热门赛事</h3>
        <div class="hot-separator"></div>
      </div>
      <div class="hot-grid">
        <div
          v-for="match in hotMatches"
          :key="'hot-' + match.id"
          class="hot-card"
          @click="goDetail(match.id)"
        >
          <div class="hot-card-top">
            <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
            <span v-if="match.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>进行中
            </span>
            <span v-else-if="match.status === 0" class="status-badge status-badge--upcoming">
              <span class="status-dot"></span>未开始
            </span>
          </div>
          <div class="hot-card-teams">
            <div class="hot-team">
              <div class="team-logo">
                <el-image :src="match.teamALogo" alt="" fit="contain" lazy>
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
                <el-image :src="match.teamBLogo" alt="" fit="contain" lazy>
                  <template #error><span class="team-logo-placeholder">{{ match.teamB === 'TBD' ? '?' : match.teamB.charAt(0) }}</span></template>
                </el-image>
              </div>
              <span class="hot-team-name" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
            </div>
          </div>
          <div class="hot-card-footer">
            <span class="match-time">{{ formatTime(match.matchTime) }}</span>
          </div>
          <div class="hot-card-odds" v-if="match.oddsA !== null && match.oddsA !== undefined">
            <span class="odds-item odds-item--a">{{ match.oddsA.toFixed(2) }}</span>
            <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
              <el-icon style="font-size:10px;margin:0 4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
            </el-tooltip>
            <span class="odds-vs">/</span>
            <span class="odds-item odds-item--b">{{ match.oddsB.toFixed(2) }}</span>
          </div>
        </div>
      </div>
    </div>

    <el-tabs v-model="activeStatus" @tab-click="handleTabChange">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="即将开始" name="0" />
      <el-tab-pane label="进行中" name="1" />
      <el-tab-pane label="已结束" name="2" />
    </el-tabs>

    <div class="match-cards">
      <!-- Loading Skeleton -->
      <template v-if="loading">
        <div v-for="i in 5" :key="'skeleton-'+i" class="match-card skeleton">
          <div class="skeleton-tournament">
            <div class="skeleton-line skeleton-line--chip"></div>
            <div class="skeleton-line skeleton-line--time"></div>
          </div>
          <div class="skeleton-teams">
            <div class="skeleton-team">
              <div class="skeleton-line skeleton-line--logo"></div>
              <div class="skeleton-line skeleton-line--name"></div>
            </div>
            <div class="skeleton-line skeleton-line--vs"></div>
            <div class="skeleton-team skeleton-team--reverse">
              <div class="skeleton-line skeleton-line--logo"></div>
              <div class="skeleton-line skeleton-line--name"></div>
            </div>
          </div>
          <div class="skeleton-footer">
            <div class="skeleton-line skeleton-line--status"></div>
          </div>
        </div>
      </template>

      <!-- Empty State -->
      <div v-else-if="matches.length === 0" class="empty-state">
        <svg class="empty-svg" viewBox="0 0 120 80" fill="none" xmlns="http://www.w3.org/2000/svg">
          <rect x="20" y="18" width="80" height="45" rx="12" stroke="#6b6b7c" stroke-width="2" fill="rgba(107,107,124,0.06)"/>
          <circle cx="42" cy="40" r="9" stroke="#5b8cff" stroke-width="1.5" fill="rgba(91,140,255,0.08)"/>
          <circle cx="78" cy="40" r="9" stroke="#00e5a0" stroke-width="1.5" fill="rgba(0,229,160,0.08)"/>
          <line x1="38" y1="40" x2="46" y2="40" stroke="#5b8cff" stroke-width="1.5" stroke-linecap="round"/>
          <line x1="42" y1="36" x2="42" y2="44" stroke="#5b8cff" stroke-width="1.5" stroke-linecap="round"/>
          <circle cx="74" cy="36" r="2.5" fill="#00e5a0" opacity="0.7"/>
          <circle cx="82" cy="36" r="2.5" fill="#00e5a0" opacity="0.7"/>
          <circle cx="74" cy="44" r="2.5" fill="#00e5a0" opacity="0.7"/>
          <circle cx="82" cy="44" r="2.5" fill="#00e5a0" opacity="0.7"/>
          <rect x="52" y="28" width="16" height="4" rx="2" fill="rgba(107,107,124,0.15)"/>
          <path d="M35 18 C35 10, 45 6, 60 6 C75 6, 85 10, 85 18" stroke="#6b6b7c" stroke-width="1.5" fill="none" stroke-linecap="round" opacity="0.3"/>
          <line x1="55" y1="50" x2="65" y2="50" stroke="#6b6b7c" stroke-width="1" stroke-linecap="round" opacity="0.3"/>
        </svg>
        <h3 class="empty-title">{{ emptyTitle }}</h3>
        <p class="empty-desc">{{ emptyDesc }}</p>
        <el-button v-if="isAdminUser" type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
        <el-button v-else type="primary" class="empty-cta" @click="$router.push('/')">回首页看看</el-button>
      </div>

      <!-- Tournament Groups -->
      <template v-else>
        <div
          v-for="group in tournamentGroups"
          :key="'tg-' + group.name"
          class="tournament-group"
        >
          <div class="tournament-group-header" @click="toggleTournament(group.name)">
            <span class="tournament-group-arrow" :class="{ 'is-expanded': isTournamentExpanded(group.name) }">&#9654;</span>
            <span class="tournament-group-name">{{ group.name }}</span>
            <span class="tournament-group-count">{{ group.matches.length }} 场</span>
          </div>
          <div v-show="isTournamentExpanded(group.name)" class="tournament-group-body">
            <div
              v-for="match in group.matches"
              :key="match.id"
              class="match-card"
              @click="goDetail(match.id)"
            >
              <div class="match-tournament">
                <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
                <span class="match-time">{{ formatTime(match.matchTime) }}</span>
              </div>

              <div class="match-teams">
                <div class="team team-a">
                  <div class="team-logo">
                    <el-image :src="match.teamALogo" alt="" fit="contain" lazy>
                      <template #error><span class="team-logo-placeholder">{{ match.teamA === 'TBD' ? '?' : match.teamA.charAt(0) }}</span></template>
                    </el-image>
                  </div>
                  <span class="team-name" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
                </div>

                <div v-if="hasScore(match)" class="score-display" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
                  <span class="score-a">{{ match.scoreA }}</span>
                  <span class="score-sep">:</span>
                  <span class="score-b">{{ match.scoreB }}</span>
                </div>
                <div v-else class="vs-badge">VS</div>

                <div class="team team-b">
                  <div class="team-logo">
                    <el-image :src="match.teamBLogo" alt="" fit="contain" lazy>
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

              <div class="match-odds" v-if="match.oddsA !== null && match.oddsA !== undefined">
                <span class="odds-item odds-item--a">{{ match.oddsA.toFixed(2) }}</span>
                <el-tooltip effect="dark" placement="top" content="赔率=1/中奖概率。例：2.0表示市场认为胜率约50%。最终按彩池制(Parimutuel)结算，你的回报=押注积分×最终赔率。">
                  <el-icon style="font-size:10px;margin:0 4px;cursor:help;color:var(--text-tertiary)"><QuestionFilled /></el-icon>
                </el-tooltip>
                <span class="odds-vs">-</span>
                <span class="odds-item odds-item--b">{{ match.oddsB.toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

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
      selectedGameId: null,
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
    }
  },
  created() {
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
        if (Array.isArray(data)) {
          this.games = data
        } else if (data.records) {
          this.games = data.records
        } else {
          this.games = data.list || data.content || []
        }
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
      if (this.selectedGameId) {
        params.gameId = this.selectedGameId
      }
      request.get('/match/list', { params }).then(function (res) {
        var data = res.data || res
        if (Array.isArray(data)) {
          this.matches = data
          this.total = data.length
        } else if (data.records) {
          this.matches = data.records
          this.total = data.total || data.records.length
        } else {
          this.matches = data.list || data.content || []
          this.total = data.total || data.totalElements || 0
        }
      }.bind(this)).catch(() => { this.$message.error('加载失败，请刷新重试') }).finally(function () {
        if (!silent) this.loading = false
      }.bind(this))
    },
    handleTabChange() {
      this.currentPage = 1
      this.fetchMatches()
    },
    fetchHotMatches(silent) {
      if (!silent) this.hotLoading = true
      var params = { limit: 6 }
      if (this.selectedGameId) {
        params.gameId = this.selectedGameId
      }
      request.get('/match/hot', { params: params }).then(function (res) {
        var data = res.data || res
        if (Array.isArray(data)) {
          this.hotMatches = data
        } else if (data.records) {
          this.hotMatches = data.records
        } else {
          this.hotMatches = data.list || data.content || []
        }
      }.bind(this)).catch(() => { this.$message.error('加载失败，请刷新重试') }).finally(function () {
        if (!silent) this.hotLoading = false
      }.bind(this))
    },
    goDetail(id) {
      this.$router.push('/match/' + id)
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var month = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var hour = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return month + '-' + day + ' ' + hour + ':' + min
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 20px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

/* Game Tabs */
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
  -webkit-overflow-scrolling: touch;
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
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--duration-fast) ease,
              background var(--duration-fast) ease,
              border-color var(--duration-fast) ease;
  font-family: var(--font-body);
  flex-shrink: 0;
}

.game-tab:hover {
  color: var(--text-primary);
  background: var(--bg-surface-2);
}

.game-tab.active {
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.08);
  border-color: rgba(0, 229, 160, 0.20);
  font-weight: 600;
}

.game-icon {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-sm);
  object-fit: contain;
  flex-shrink: 0;
}

.match-cards {
  min-height: 200px;
}

/* Tournament Grouping */
.tournament-group {
  margin-bottom: 20px;
}

.tournament-group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--duration-fast) ease,
              border-color var(--duration-fast) ease;
  margin-bottom: 8px;
  user-select: none;
}

.tournament-group-header:hover {
  background: var(--bg-surface-3);
  border-color: var(--border-default);
}

.tournament-group-arrow {
  font-size: 10px;
  color: var(--text-tertiary);
  transition: transform var(--duration-fast) var(--ease-out);
  display: inline-block;
  flex-shrink: 0;
  width: 14px;
  text-align: center;
}

.tournament-group-arrow.is-expanded {
  transform: rotate(90deg);
}

.tournament-group-name {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--text-primary);
  letter-spacing: 0.5px;
  flex: 1;
}

.tournament-group-count {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.tournament-group-body {
  /* body container */
}

/* Score Display */
.score-display {
  display: flex;
  align-items: center;
  gap: 6px;
  font-family: var(--font-mono);
  font-weight: 700;
  font-size: 20px;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1px;
}

.score-display--sm {
  font-size: 15px;
  gap: 4px;
}

.score--live {
  color: var(--accent-primary);
  text-shadow: 0 0 12px rgba(0, 229, 160, 0.3);
}

.score--finished {
  color: var(--text-secondary);
}

.score-a,
.score-b {
  font-variant-numeric: tabular-nums;
}

.score-sep {
  opacity: 0.5;
  font-weight: 400;
}

.match-card {
  display: grid;
  grid-template-columns: 80px 1fr auto;
  align-items: center;
  gap: 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
  animation: match-enter 0.4s var(--ease-out) both;
}

.tournament-group-body .match-card:nth-child(1) { animation-delay: 0ms; }
.tournament-group-body .match-card:nth-child(2) { animation-delay: 20ms; }
.tournament-group-body .match-card:nth-child(3) { animation-delay: 40ms; }
.tournament-group-body .match-card:nth-child(4) { animation-delay: 60ms; }
.tournament-group-body .match-card:nth-child(5) { animation-delay: 80ms; }
.tournament-group-body .match-card:nth-child(6) { animation-delay: 100ms; }
.tournament-group-body .match-card:nth-child(7) { animation-delay: 120ms; }
.tournament-group-body .match-card:nth-child(8) { animation-delay: 140ms; }
.tournament-group-body .match-card:nth-child(9) { animation-delay: 160ms; }
.tournament-group-body .match-card:nth-child(10) { animation-delay: 180ms; }

@keyframes match-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.match-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-lg);
}

.match-card:has(.status-badge--live) {
  border-left: 2px solid var(--accent-primary);
  background: linear-gradient(90deg, rgba(0,229,160,0.03) 0%, var(--bg-surface-1) 30%);
}

.match-tournament {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.match-time {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
}

.match-teams {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.team {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.team-b {
  flex-direction: row-reverse;
  text-align: right;
}

.team-logo {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.team-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-logo-placeholder {
  font-size: 14px;
  font-weight: 700;
  color: var(--accent-primary);
}

.team-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vs-badge {
  font-family: var(--font-display);
  font-size: 18px;
  color: var(--accent-primary);
  letter-spacing: 2px;
  flex-shrink: 0;
}

.match-footer {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.match-result {
  color: var(--accent-primary);
  font-weight: 600;
  font-size: 14px;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
  max-width: 400px;
  margin: 0 auto;
}

.empty-svg {
  width: 100px;
  height: 68px;
  margin-bottom: 20px;
  opacity: 0.7;
  animation: empty-float 3s ease-in-out infinite;
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.6;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--text-tertiary);
  margin-bottom: 24px;
  max-width: 300px;
}

.empty-action {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
}

.empty-cta {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 28px;
  box-shadow: 0 0 20px rgba(0, 229, 160, 0.2);
  transition: all 0.2s var(--ease-out);
}

.empty-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0, 229, 160, 0.35);
}

/* Skeleton Loading */
@keyframes shimmer {
  0% { background-position: -200% 0; transform: scaleY(1); }
  50% { transform: scaleY(1.012); }
  100% { background-position: 200% 0; transform: scaleY(1); }
}
@keyframes skeleton-avatar-pulse {
  0%, 100% { transform: scale(0.98); opacity: 0.85; }
  50% { transform: scale(1.02); opacity: 1; }
}
@keyframes skeleton-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.skeleton {
  pointer-events: none;
  animation: skeleton-fade-in 0.3s ease both;
}

.skeleton .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-tournament {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.skeleton-line--chip { width: 72px; height: 20px; border-radius: var(--radius-md); }
.skeleton-line--time { width: 60px; height: 14px; }
.skeleton-line--name { width: 80px; height: 14px; }
.skeleton-line--vs { width: 28px; height: 16px; border-radius: var(--radius-sm); }
.skeleton-line--logo {
  width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0;
  animation: shimmer 2s ease-in-out infinite, skeleton-avatar-pulse 2.5s ease-in-out infinite;
}
.skeleton-line--status { width: 64px; height: 20px; border-radius: var(--radius-md); }

.skeleton-teams {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
}

.skeleton-team {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.skeleton-team--reverse {
  flex-direction: row-reverse;
}

.skeleton-footer {
  grid-column: 1 / -1;
  margin-top: 8px;
}

/* Hot Section */
.hot-section {
  margin-bottom: 24px;
  animation: match-enter 0.4s var(--ease-out) both;
}

.hot-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.hot-title {
  font-family: var(--font-display);
  font-size: 20px;
  color: var(--text-primary);
  letter-spacing: 1px;
  white-space: nowrap;
}

.hot-separator {
  flex: 1;
  height: 1px;
  background: linear-gradient(90deg, var(--border-subtle) 0%, transparent 100%);
}

.hot-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.hot-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 14px 16px;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  animation: match-enter 0.4s var(--ease-out) both;
}

.hot-card:nth-child(1) { animation-delay: 0ms; }
.hot-card:nth-child(2) { animation-delay: 40ms; }
.hot-card:nth-child(3) { animation-delay: 80ms; }
.hot-card:nth-child(4) { animation-delay: 120ms; }
.hot-card:nth-child(5) { animation-delay: 160ms; }
.hot-card:nth-child(6) { animation-delay: 200ms; }

.hot-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-lg);
}

.hot-card:has(.status-badge--live) {
  border-left: 2px solid var(--accent-primary);
  background: linear-gradient(90deg, rgba(0, 229, 160, 0.03) 0%, var(--bg-surface-1) 30%);
}

.hot-card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.hot-card-teams {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 10px;
}

.hot-team {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.hot-team--reverse {
  flex-direction: row-reverse;
  text-align: right;
}

.hot-team-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.vs-badge--sm {
  font-size: 14px;
}

.hot-card-footer {
  display: flex;
  justify-content: center;
}

@media (max-width: 768px) {
  .hot-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  /* 页面标题缩小 */
  .page-title {
    font-size: 26px;
  }
  /* 赛事卡片收紧 */
  .match-card {
    padding: 14px 14px;
    gap: 10px;
  }
  .match-teams {
    gap: 14px;
  }
  .team-name {
    font-size: 14px;
  }
  .team-logo {
    width: 28px;
    height: 28px;
  }
  /* 锦标赛分组标题收紧 */
  .tournament-group-header {
    padding: 10px 12px;
    gap: 8px;
  }
  .tournament-group-name {
    font-size: 15px;
  }
  /* 热门卡片收紧 */
  .hot-card {
    padding: 12px 12px;
  }
  .hot-team-name {
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .hot-grid {
    grid-template-columns: 1fr;
  }
  .page-title {
    font-size: 22px;
  }
  /* 小屏队伍名允许换行，避免横向溢出 */
  .team-name {
    font-size: 13px;
    white-space: normal;
    word-break: break-word;
    line-height: 1.3;
  }
  .hot-team-name {
    font-size: 12px;
    white-space: normal;
    word-break: break-word;
    line-height: 1.3;
  }
  /* 队伍区 gap 进一步收紧 */
  .match-teams {
    gap: 8px;
  }
  .team {
    gap: 8px;
  }
  /* score 字号略小 */
  .score-display {
    font-size: 17px;
  }
  .vs-badge {
    font-size: 15px;
  }
}

/* TBD Team Name */
.team-name-tbd {
  color: var(--text-tertiary) !important;
  font-style: italic;
}

/* Hot Card Odds */
.hot-card-odds {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
}

.odds-item {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
}

.odds-item--a {
  color: var(--text-secondary);
}

.odds-item--b {
  color: var(--text-secondary);
}

.odds-vs {
  font-size: 10px;
  color: var(--text-tertiary);
}

/* Match Card Odds */
.match-odds {
  grid-column: 1 / -1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--border-subtle);
}
</style>
