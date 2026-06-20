<template>
  <div class="public-profile">
    <div class="page-header">
      <el-button type="text" icon="el-icon-arrow-left" class="back-btn" @click="$router.back()">返回</el-button>
    </div>

    <div v-if="loading" class="loading-state">
      <i class="el-icon-loading"></i> 加载中...
    </div>

    <div v-else-if="user" class="profile-content">
      <div class="user-info-card">
        <div class="user-avatar-section">
          <el-avatar :size="80" :src="user.avatar" icon="el-icon-user-solid" />
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

      <div class="login-hint">
        <p><i class="el-icon-info"></i> 登录后可查看详细投注记录</p>
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
import request from '@/utils/request'

export default {
  name: 'PublicProfile',
  data() {
    return {
      loading: false,
      user: null
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
