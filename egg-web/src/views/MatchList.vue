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

    <div v-loading="loading" class="match-cards">
      <el-empty v-if="!loading && matches.length === 0" description="暂无赛事" />

      <div
        v-for="match in matches"
        :key="match.id"
        class="match-card"
        @click="goDetail(match.id)"
      >
        <div class="match-tournament">
          <el-tag size="mini" type="info">{{ match.tournament || '常规赛事' }}</el-tag>
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
          <el-tag v-if="match.status === 0" type="warning" size="small">即将开始</el-tag>
          <el-tag v-else-if="match.status === 1" type="danger" size="small" class="live-tag">
            <span class="live-dot"></span> LIVE
          </el-tag>
          <el-tag v-else-if="match.status === 2" type="success" size="small">已结束</el-tag>

          <span v-if="match.status === 2 && match.result !== null && match.result !== undefined" class="match-result">
            <template v-if="match.result === 0">{{ match.teamA }} 获胜!</template>
            <template v-else-if="match.result === 1">{{ match.teamB }} 获胜!</template>
            <template v-else-if="match.result === 2">平局</template>
          </span>
        </div>
      </div>
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
  margin-bottom: 16px;
}

.page-title {
  color: #d4d4d4;
  font-size: 24px;
  font-weight: 700;
}

.match-cards {
  min-height: 200px;
}

.match-card {
  background-color: #1a1a1a;
  border: 1px solid #2e2e2e;
  border-radius: 10px;
  padding: 20px 24px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.match-card:hover {
  border-color: #6aaa96;
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(106, 170, 150, 0.15);
}

.match-tournament {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.match-time {
  color: #888888;
  font-size: 13px;
}

.match-teams {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  margin-bottom: 16px;
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
  width: 48px;
  height: 48px;
  border-radius: 50%;
  overflow: hidden;
  background-color: #2e2e2e;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.team-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.team-logo-placeholder {
  font-size: 20px;
  font-weight: 700;
  color: #6aaa96;
}

.team-name {
  font-size: 18px;
  font-weight: 600;
  color: #d4d4d4;
}

.vs-badge {
  font-size: 16px;
  font-weight: 800;
  color: #6aaa96;
  letter-spacing: 2px;
  flex-shrink: 0;
}

.match-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.match-result {
  color: #6aaa96;
  font-weight: 600;
  font-size: 14px;
}

.live-tag {
  animation: livePulse 1.5s ease-in-out infinite;
}

.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #4ade80;
  margin-right: 4px;
  animation: dotBlink 1s ease-in-out infinite;
}

@keyframes livePulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

@keyframes dotBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>