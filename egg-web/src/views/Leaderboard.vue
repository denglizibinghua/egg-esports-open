<template>
  <div class="leaderboard">
    <div class="page-header">
      <h2 class="page-title">排行榜</h2>
    </div>

    <!-- Top 3 Podium -->
    <div v-if="top3.length > 0" class="podium">
      <div v-if="top3[1]" class="podium-item silver">
        <div class="podium-rank">2</div>
        <div class="podium-avatar">
          <el-avatar :size="56" icon="el-icon-user-solid" />
        </div>
        <div class="podium-name">{{ top3[1].nickname || top3[1].username }}</div>
        <div class="podium-points">{{ top3[1].points }} 积分</div>
      </div>

      <div v-if="top3[0]" class="podium-item gold">
        <div class="podium-crown">👑</div>
        <div class="podium-rank">1</div>
        <div class="podium-avatar">
          <el-avatar :size="72" icon="el-icon-user-solid" />
        </div>
        <div class="podium-name">{{ top3[0].nickname || top3[0].username }}</div>
        <div class="podium-points">{{ top3[0].points }} 积分</div>
      </div>

      <div v-if="top3[2]" class="podium-item bronze">
        <div class="podium-rank">3</div>
        <div class="podium-avatar">
          <el-avatar :size="56" icon="el-icon-user-solid" />
        </div>
        <div class="podium-name">{{ top3[2].nickname || top3[2].username }}</div>
        <div class="podium-points">{{ top3[2].points }} 积分</div>
      </div>
    </div>

    <!-- Full Leaderboard Table -->
    <el-table :data="leaderboard" style="width: 100%" v-loading="loading" class="leaderboard-table">
      <el-table-column label="排名" width="80" align="center">
        <template slot-scope="scope">
          <span :class="getRankClass(scope.$index + 1 + (currentPage - 1) * pageSize)">
            {{ scope.$index + 1 + (currentPage - 1) * pageSize }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="昵称" min-width="150">
        <template slot-scope="scope">
          <span>{{ scope.row.nickname || scope.row.username }}</span>
        </template>
      </el-table-column>
      <el-table-column label="积分" width="100" align="center" prop="points" />
      <el-table-column label="胜率" width="100" align="center">
        <template slot-scope="scope">
          {{ getAccuracy(scope.row) }}%
        </template>
      </el-table-column>
      <el-table-column label="连胜" width="80" align="center" prop="winStreak" />
    </el-table>

    <el-pagination
      v-if="total > 0"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      :current-page.sync="currentPage"
      @current-change="fetchLeaderboard"
    />
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'LeaderboardPage',
  data() {
    return {
      leaderboard: [],
      top3: [],
      loading: false,
      currentPage: 1,
      pageSize: 20,
      total: 0
    }
  },
  created() {
    this.fetchTop3()
    this.fetchLeaderboard()
  },
  methods: {
    fetchTop3() {
      request.get('/leaderboard/top/3').then(res => {
        const data = res.data || res
        this.top3 = Array.isArray(data) ? data : []
      }).catch(() => {})
    },
    fetchLeaderboard() {
      this.loading = true
      request.get('/leaderboard/points', {
        params: {
          page: this.currentPage,
          size: this.pageSize
        }
      }).then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.leaderboard = data
          this.total = data.length
        } else if (data.records) {
          this.leaderboard = data.records
          this.total = data.total || data.records.length
        } else {
          this.leaderboard = data.list || data.content || []
          this.total = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
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
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 40px;
  padding: 32px 0;
}

.podium-item {
  background: var(--bg-surface-1);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  text-align: center;
  min-width: 160px;
  position: relative;
  transition: transform var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.podium-item:hover {
  transform: translateY(-4px);
}

.podium-item.gold {
  border: 2px solid #d4a843;
  padding-bottom: 32px;
  min-width: 180px;
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

.podium-crown {
  font-size: 28px;
  margin-bottom: 4px;
}

.podium-rank {
  font-family: var(--font-display);
  font-size: 24px;
  margin-bottom: 8px;
}

.gold .podium-rank {
  color: #d4a843;
}

.silver .podium-rank {
  color: #a0a0a0;
}

.bronze .podium-rank {
  color: #c47a3a;
}

.podium-name {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.podium-points {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.rank-gold {
  color: #d4a843;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.rank-silver {
  color: #a0a0a0;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.rank-bronze {
  color: #c47a3a;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.leaderboard-table {
  margin-top: 8px;
}
</style>