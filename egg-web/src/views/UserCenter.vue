<template>
  <div class="user-center" v-loading="loading">
    <div class="page-header">
      <h2 class="page-title">个人中心</h2>
    </div>

    <div v-if="user" class="user-content">
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

        <el-table :data="bets" style="width: 100%" v-loading="betsLoading" empty-text="暂无押注记录">
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
              <el-tag v-if="scope.row.status === 0" type="warning" size="small">待结算</el-tag>
              <el-tag v-else-if="scope.row.status === 1" type="success" size="small">猜中</el-tag>
              <el-tag v-else-if="scope.row.status === 2" type="danger" size="small">未中</el-tag>
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
  margin-bottom: 20px;
}

.page-title {
  color: #e0e0e0;
  font-size: 24px;
  font-weight: 700;
}

.user-info-card {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-nickname {
  color: #e0e0e0;
  font-size: 20px;
  font-weight: 600;
}

.user-join-date {
  color: #a0a0b0;
  font-size: 13px;
  margin-top: 4px;
}

.user-points {
  text-align: center;
}

.points-value {
  color: #FF4655;
  font-size: 32px;
  font-weight: 800;
}

.points-label {
  color: #a0a0b0;
  font-size: 13px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}

.stat-card {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}

.stat-value {
  color: #FF4655;
  font-size: 24px;
  font-weight: 700;
}

.stat-label {
  color: #a0a0b0;
  font-size: 12px;
  margin-top: 4px;
}

.bet-history {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  color: #e0e0e0;
  font-size: 18px;
  font-weight: 600;
}
</style>