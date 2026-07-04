<template>
  <div class="public-profile">
    <div class="page-header">
      <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <el-icon><Loading /></el-icon> 加载中...
    </div>

    <div v-else-if="user" class="profile-content">
      <div class="user-info-card">
        <div class="user-avatar-section">
          <el-avatar :size="80" :src="user.avatar" :icon="UserFilled" />
          <div class="user-meta">
            <div class="user-nickname">{{ user.nickname }}</div>
            <div class="user-stats-brief">
              <span>积分 {{ user.points }}</span>
              <span class="dot-sep">·</span>
              <span>猜中 {{ user.correctBets || 0 }}</span>
            </div>
          </div>
        </div>
        <div class="user-points">
          <div class="points-value">{{ user.points }}</div>
          <div class="points-label">积分</div>
        </div>
      </div>

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

      <div class="rank-section" v-if="getToken()">
        <h4 class="rank-section-title">🏆 排行榜排名</h4>
        <div class="rank-items">
          <div class="rank-item" @click="cycleRankDimension('points')" :class="{ active: rankDimension === 'points' }">
            <div class="rank-item-label">积分榜</div>
            <div class="rank-item-value" v-if="ranks.points && ranks.points.rank > 0">
              #{{ ranks.points.rank }}
            </div>
            <div class="rank-item-value rank-item-none" v-else>未上榜</div>
          </div>
          <div class="rank-item" @click="cycleRankDimension('accuracy')" :class="{ active: rankDimension === 'accuracy' }">
            <div class="rank-item-label">胜率榜</div>
            <div class="rank-item-value" v-if="ranks.accuracy && ranks.accuracy.rank > 0">
              #{{ ranks.accuracy.rank }}
            </div>
            <div class="rank-item-value rank-item-none" v-else>未上榜</div>
          </div>
          <div class="rank-item" @click="cycleRankDimension('worst-profit')" :class="{ active: rankDimension === 'worst-profit' }">
            <div class="rank-item-label">亏损榜</div>
            <div class="rank-item-value" v-if="ranks['worst-profit'] && ranks['worst-profit'].rank > 0">
              #{{ ranks['worst-profit'].rank }}
            </div>
            <div class="rank-item-value rank-item-none" v-else>未上榜</div>
          </div>
        </div>
      </div>

      <div class="rank-section" v-else>
        <h4 class="rank-section-title">🏆 排行榜排名</h4>
        <div class="rank-login-prompt">
          <svg class="rank-lock-svg" viewBox="0 0 48 48" fill="none">
            <rect x="12" y="20" width="24" height="20" rx="3" stroke="#5b8cff" stroke-width="1.5" fill="rgba(91,140,255,0.06)"/>
            <path d="M17 20 V14 C17 9, 20 6, 24 6 C28 6, 31 9, 31 14 V20" stroke="#5b8cff" stroke-width="1.5" fill="none" stroke-linecap="round"/>
            <circle cx="24" cy="30" r="3" fill="#5b8cff" opacity="0.4"/>
          </svg>
          <p>登录后查看排名</p>
          <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
        </div>
      </div>

      <div class="login-hint">
        <p><el-icon><InfoFilled /></el-icon> 登录后可查看详细投注记录</p>
        <el-button type="primary" size="small" @click="$router.push('/login')">去登录</el-button>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">🔍</div>
      <h3 class="empty-title">用户不存在</h3>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { getToken } from '@/utils/auth'
import { ArrowLeft, Loading, UserFilled, InfoFilled } from '@element-plus/icons-vue'

export default {
  name: 'PublicProfile',
  components: { Loading, InfoFilled },
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
.page-header {
  margin-bottom: 24px;
}

.back-btn {
  color: var(--text-secondary);
  font-size: 14px;
}

.back-btn:hover {
  color: var(--accent-primary);
}

.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 64px;
  color: var(--text-tertiary);
  font-size: 16px;
}

.loading-state i {
  margin-right: 8px;
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
  font-size: 22px;
  font-weight: 600;
}

.user-stats-brief {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 4px;
}

.dot-sep {
  margin: 0 6px;
}

.user-points {
  text-align: center;
}

.points-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 36px;
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

/* ── Rank Section ── */
.rank-section {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
}

.rank-section-title {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.rank-items {
  display: flex;
  gap: 8px;
}

.rank-item {
  flex: 1;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  padding: 14px 12px;
  text-align: center;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  border: 1px solid transparent;
}

.rank-item:hover {
  border-color: var(--border-default);
}

.rank-item.active {
  border-color: var(--accent-primary);
  box-shadow: 0 0 12px rgba(0, 229, 160, 0.15);
}

.rank-item-label {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-bottom: 4px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.rank-item-value {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-primary);
  font-variant-numeric: tabular-nums;
}

.rank-item-total {
  font-size: 12px;
  font-weight: 400;
  color: var(--text-tertiary);
}

.rank-item-none {
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 400;
}

/* Login prompt within rank section */
.rank-login-prompt {
  text-align: center;
  padding: 16px 0;
}

.rank-login-prompt p {
  color: var(--text-tertiary);
  font-size: 13px;
  margin: 12px 0;
}

.rank-lock-svg {
  width: 48px;
  height: 48px;
  opacity: 0.6;
}

.login-hint {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
}

.login-hint p {
  margin-bottom: 12px;
}

.login-hint i {
  margin-right: 4px;
}

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
}

/* ── Tablet / small screens ── */
@media (max-width: 767px) {
  .user-info-card {
    flex-direction: column;
    align-items: center;
    gap: 16px;
  }

  .user-avatar-section {
    justify-content: center;
  }

  .user-nickname {
    font-size: 18px;
  }

  .points-value {
    font-size: 28px;
  }

  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .rank-items {
    flex-direction: column;
  }

  .rank-item {
    padding: 10px 12px;
  }
}

/* ── Mobile ── */
@media (max-width: 479px) {
  .user-info-card {
    padding: 20px;
  }

  .user-nickname {
    font-size: 16px;
  }

  .points-value {
    font-size: 24px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .stat-card {
    padding: 14px 10px;
  }

  .stat-value {
    font-size: 20px;
  }
}
</style>
