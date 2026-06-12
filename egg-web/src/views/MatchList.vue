<template>
  <div class="match-list">
    <div class="page-header">
      <h2 class="page-title">赛事列表</h2>
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
        <div class="empty-icon">🎮</div>
        <h3 class="empty-title">{{ emptyTitle }}</h3>
        <p class="empty-desc">{{ emptyDesc }}</p>
        <el-button v-if="isAdminUser" type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
      </div>

      <!-- Match Cards -->
      <template v-else>
        <div
          v-for="match in matches"
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
                <img v-if="match.teamALogo" :src="match.teamALogo" alt="">
                <span v-else class="team-logo-placeholder">{{ match.teamA.charAt(0) }}</span>
              </div>
              <span class="team-name">{{ match.teamA }}</span>
            </div>

            <div class="vs-badge">VS</div>

            <div class="team team-b">
              <div class="team-logo">
                <img v-if="match.teamBLogo" :src="match.teamBLogo" alt="">
                <span v-else class="team-logo-placeholder">{{ match.teamB.charAt(0) }}</span>
              </div>
              <span class="team-name">{{ match.teamB }}</span>
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
              <template v-if="match.result === 0">{{ match.teamA }} 获胜!</template>
              <template v-else-if="match.result === 1">{{ match.teamB }} 获胜!</template>
              <template v-else-if="match.result === 2">平局</template>
            </span>
          </div>
        </div>
      </template>
    </div>

    <el-pagination
      v-if="total > 0"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page.sync="currentPage"
      @current-change="fetchMatches"
    />
  </div>
</template>

<script>
import request from '@/utils/request'
import { isAdmin } from '@/utils/auth'

export default {
  name: 'MatchList',
  data() {
    return {
      activeStatus: 'all',
      matches: [],
      loading: false,
      currentPage: 1,
      pageSize: 10,
      total: 0
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
    }
  },
  created() {
    this.fetchMatches()
  },
  methods: {
    fetchMatches() {
      this.loading = true
      const params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.activeStatus !== 'all') {
        params.status = this.activeStatus
      }
      request.get('/match/list', { params }).then(res => {
        const data = res.data || res
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
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    handleTabChange() {
      this.currentPage = 1
      this.fetchMatches()
    },
    goDetail(id) {
      this.$router.push('/match/' + id)
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      const d = new Date(timeStr)
      const month = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const hour = d.getHours().toString().padStart(2, '0')
      const min = d.getMinutes().toString().padStart(2, '0')
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

.match-cards {
  min-height: 200px;
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

.match-card:nth-child(1) { animation-delay: 0ms; }
.match-card:nth-child(2) { animation-delay: 20ms; }
.match-card:nth-child(3) { animation-delay: 40ms; }
.match-card:nth-child(4) { animation-delay: 60ms; }
.match-card:nth-child(5) { animation-delay: 80ms; }
.match-card:nth-child(6) { animation-delay: 100ms; }
.match-card:nth-child(7) { animation-delay: 120ms; }
.match-card:nth-child(8) { animation-delay: 140ms; }
.match-card:nth-child(9) { animation-delay: 160ms; }
.match-card:nth-child(10) { animation-delay: 180ms; }

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

/* Skeleton Loading */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton {
  pointer-events: none;
}

.skeleton .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
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
.skeleton-line--logo { width: 32px; height: 32px; border-radius: 50%; flex-shrink: 0; }
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
</style>