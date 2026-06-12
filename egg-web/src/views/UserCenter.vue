<template>
  <div class="user-center">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>

    <!-- Loading Skeleton -->
    <template v-if="loading">
      <div class="user-info-card skeleton">
        <div class="user-avatar-section">
          <div class="skeleton-line skeleton-line--avatar"></div>
          <div class="user-meta">
            <div class="skeleton-line skeleton-line--nickname"></div>
            <div class="skeleton-line skeleton-line--date"></div>
          </div>
        </div>
        <div class="user-points">
          <div class="skeleton-line skeleton-line--points"></div>
          <div class="skeleton-line skeleton-line--label"></div>
        </div>
      </div>
      <div class="stats-grid">
        <div v-for="i in 5" :key="'stat-skel-'+i" class="stat-card skeleton">
          <div class="skeleton-line skeleton-line--stat-val"></div>
          <div class="skeleton-line skeleton-line--stat-lbl"></div>
        </div>
      </div>
      <div class="bet-history skeleton">
        <div class="section-header">
          <div class="skeleton-line skeleton-line--section-title"></div>
        </div>
        <div class="skeleton-table">
          <div v-for="i in 4" :key="'bet-skel-'+i" class="skeleton-table-row">
            <div class="skeleton-line skeleton-line--cell-lg"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-sm"></div>
            <div class="skeleton-line skeleton-line--cell-md"></div>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="user" class="user-content">
      <!-- User Info Card -->
      <div class="user-info-card">
        <div class="user-avatar-section">
          <el-avatar :size="80" icon="el-icon-user-solid" />
          <div class="user-meta">
            <div class="user-nickname">{{ user.nickname || user.username }}</div>
            <div class="user-join-date">加入于 {{ formatDate(user.createTime) }}</div>
          </div>
        </div>
        <div class="user-points">
          <div class="points-value">{{ user.points }}</div>
          <div class="points-label">积分</div>
        </div>
      </div>

      <!-- Stats Cards -->
      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-value">{{ user.totalBets || 0 }}</div>
          <div class="stat-label">总押注</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ user.correctBets || 0 }}</div>
          <div class="stat-label">猜中</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ accuracy }}%</div>
          <div class="stat-label">胜率</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ user.winStreak || 0 }}</div>
          <div class="stat-label">连胜</div>
        </div>
        <div class="stat-card">
          <div class="stat-value">{{ user.maxWinStreak || 0 }}</div>
          <div class="stat-label">最高连胜</div>
        </div>
      </div>

      <!-- Bet History -->
      <div class="bet-history">
        <div class="section-header">
          <h3 class="section-title">押注记录</h3>
          <el-radio-group v-model="betStatus" size="small" @change="fetchBets">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="0">待结算</el-radio-button>
            <el-radio-button label="1">猜中</el-radio-button>
            <el-radio-button label="2">未中</el-radio-button>
          </el-radio-group>
        </div>

        <!-- Bet History Empty State -->
        <div v-if="!betsLoading && bets.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <h3 class="empty-title">暂无投注记录</h3>
          <p class="empty-desc">去赛事列表选一场比赛，试试你的判断力！</p>
          <el-button type="primary" class="empty-action" @click="$router.push('/')">去看看</el-button>
        </div>

        <template v-else>
          <el-table :data="bets" style="width: 100%" v-loading="betsLoading">
            <el-table-column label="赛事" min-width="180">
              <template slot-scope="scope">
                {{ scope.row.matchTitle || ('赛事 #' + scope.row.matchId) }}
              </template>
            </el-table-column>
            <el-table-column label="押注方" width="100" align="center">
              <template slot-scope="scope">
                <el-tag size="small" :type="scope.row.betSide === 0 ? 'danger' : 'primary'">
                  {{ scope.row.betSide === 0 ? 'A队' : 'B队' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="积分" width="80" align="center" prop="betAmount" />
            <el-table-column label="状态" width="100" align="center">
              <template slot-scope="scope">
                <span v-if="scope.row.status === 0" class="status-badge status-badge--pending">待结算</span>
                <span v-else-if="scope.row.status === 1" class="status-badge status-badge--won">
                  <span class="status-dot"></span>猜中
                </span>
                <span v-else-if="scope.row.status === 2" class="status-badge status-badge--lost">
                  <span class="status-dot"></span>未中
                </span>
              </template>
            </el-table-column>
            <el-table-column label="时间" width="160" align="center">
              <template slot-scope="scope">
                {{ formatDate(scope.row.createTime) }}
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="betTotal > 0"
            background
            layout="prev, pager, next"
            :total="betTotal"
            :page-size="betPageSize"
            :current-page.sync="betCurrentPage"
            @current-change="fetchBets"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken, setUser } from '@/utils/auth'

export default {
  name: 'UserCenter',
  data() {
    return {
      loading: false,
      betsLoading: false,
      user: null,
      bets: [],
      betStatus: 'all',
      betCurrentPage: 1,
      betPageSize: 10,
      betTotal: 0
    }
  },
  computed: {
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.round((this.user.correctBets / this.user.totalBets) * 100)
    }
  },
  created() {
    if (!getToken()) {
      this.$router.push('/login')
      return
    }
    this.fetchUserInfo()
    this.fetchBets()
  },
  methods: {
    fetchUserInfo() {
      this.loading = true
      request.get('/user/me').then(res => {
        this.user = res.data || res
        setUser(this.user)
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    fetchBets() {
      this.betsLoading = true
      const params = {
        page: this.betCurrentPage,
        size: this.betPageSize
      }
      if (this.betStatus !== 'all') {
        params.status = this.betStatus
      }
      request.get('/bet/my', { params }).then(res => {
        const data = res.data || res
        if (Array.isArray(data)) {
          this.bets = data
          this.betTotal = data.length
        } else if (data.records) {
          this.bets = data.records
          this.betTotal = data.total || data.records.length
        } else {
          this.bets = data.list || data.content || []
          this.betTotal = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.betsLoading = false
      })
    },
    formatDate(timeStr) {
      if (!timeStr) return '-'
      const d = new Date(timeStr)
      const y = d.getFullYear()
      const m = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const h = d.getHours().toString().padStart(2, '0')
      const min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + m + '-' + day + ' ' + h + ':' + min
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

.user-info-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  box-shadow: var(--shadow-md);
}

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-nickname {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 600;
}

.user-join-date {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 4px;
}

.user-points {
  text-align: center;
}

.points-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 32px;
  font-weight: 800;
  font-variant-numeric: tabular-nums;
}

.points-label {
  color: var(--text-tertiary);
  font-size: 13px;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px 16px;
  text-align: center;
  transition: transform var(--duration-normal) var(--ease-out),
              border-color var(--duration-normal) var(--ease-out);
}

.stat-card:hover {
  transform: translateY(-2px);
  border-color: var(--border-strong);
}

.stat-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.stat-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-top: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.bet-history {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px;
  box-shadow: var(--shadow-md);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 1px;
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

.skeleton .skeleton-line,
.skeleton > .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--avatar { width: 80px; height: 80px; border-radius: 50%; flex-shrink: 0; }
.skeleton-line--nickname { width: 120px; height: 20px; margin-bottom: 6px; }
.skeleton-line--date { width: 100px; height: 13px; }
.skeleton-line--points { width: 64px; height: 32px; margin: 0 auto 4px; }
.skeleton-line--label { width: 40px; height: 13px; margin: 0 auto; }
.skeleton-line--stat-val { width: 40px; height: 24px; margin: 0 auto 6px; }
.skeleton-line--stat-lbl { width: 48px; height: 12px; margin: 0 auto; }
.skeleton-line--section-title { width: 100px; height: 22px; }

.skeleton-table {
  margin-top: 8px;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-table-row:last-child {
  border-bottom: none;
}

.skeleton-table-row .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
  height: 14px;
}

.skeleton-line--cell-lg { width: 180px; }
.skeleton-line--cell-md { width: 120px; }
.skeleton-line--cell-sm { width: 60px; }
</style>