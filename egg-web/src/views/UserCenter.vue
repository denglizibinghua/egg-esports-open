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
        <div v-for="i in 6" :key="'stat-skel-'+i" class="stat-card skeleton">
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
      <!-- Tab Navigation -->
      <el-tabs v-model="activeTab" class="user-tabs">
        <el-tab-pane label="个人资料" name="profile">
          <!-- 个人资料：头像 / 昵称 / 排名徽章 / 统计卡片 / 银行入口 / 编辑弹窗 -->
          <profile-tab
            :user="user"
            :my-rank="myRank"
            :rank-dimension="rankDimension"
            :loan-status="loanStatus"
            @update-profile="onUpdateProfile"
            @avatar-updated="onAvatarUpdated"
            @refresh-user="fetchUserInfo"
            @cycle-rank="cycleRankDimension"
          />
        </el-tab-pane>

        <el-tab-pane label="数据仪表盘" name="dashboard">
          <!-- 数据仪表盘：趋势条 / 胜率 / 连胜 / 战绩图 / 收益曲线 -->
          <stats-dashboard :user="user" />
        </el-tab-pane>

        <el-tab-pane name="history">
          <template #label><span>
            投注记录
            <span v-if="settlementCount > 0" class="settlement-badge">{{ settlementCount }}</span>
          </span></template>
          <!-- 投注历史：状态筛选 / 表格 / 分页（子组件自洽加载） -->
          <bet-history-tab :settled-bet-keys="settledBetKeys" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { getToken, setUser } from '@/utils/auth'
import ProfileTab from '@/components/user/ProfileTab.vue'
import BetHistoryTab from '@/components/user/BetHistoryTab.vue'
import StatsDashboard from '@/components/user/StatsDashboard.vue'

var LAST_VISIT_KEY = 'egg_lastVisitTime'

export default {
  name: 'UserCenter',
  components: { ProfileTab, BetHistoryTab, StatsDashboard },
  data() {
    return {
      loading: false,
      // 当前登录用户（核心共享状态）
      user: null,
      // 当前激活的 tab
      activeTab: 'profile',
      // 我的排名信息 + 排行维度
      myRank: null,
      rankDimension: 'points',
      // 贷款 / 银行状态
      loanStatus: null,
      // 自上次访问以来新结算的投注：数量（tab 角标）+ key 列表（行高亮，传给 BetHistoryTab）
      settlementCount: 0,
      settledBetKeys: [],
      // 银行补贴相关（保留以免破坏既有逻辑）
      claimingStipend: false,
      loanConfig: {
        dailyStipend: 100
      }
    }
  },
  created() {
    if (!getToken()) {
      this.$router.push('/login')
      return
    }
    // 父组件只负责：用户信息、排名、新结算角标、贷款状态
    // 投注列表 / 最近投注 / 盈亏曲线由对应子组件自洽加载
    this.fetchUserInfo()
    this.fetchMyRank()
    this.fetchSettledSince()
    this.fetchLoanStatus()
  },
  methods: {
    // 拉取当前用户信息并同步到 localStorage（供其他页面使用）
    fetchUserInfo() {
      this.loading = true
      request.get('/user/me').then(function (res) {
        this.user = res.data || res
        setUser(this.user)
      }.bind(this)).catch(function () {}).finally(function () {
        this.loading = false
      }.bind(this))
    },
    // 查询我的排名（按当前维度）
    fetchMyRank() {
      var params = { dimension: this.rankDimension }
      request.get('/leaderboard/my-rank', { params }).then(function (res) {
        this.myRank = res.data || res
      }.bind(this)).catch(function () {
        this.myRank = { rank: 0, total: 0 }
      }.bind(this))
    },
    // 切换排行维度并重新查询（由 ProfileTab 通过 @cycle-rank 触发）
    cycleRankDimension() {
      var dims = ['points', 'accuracy', 'worst-profit']
      var idx = dims.indexOf(this.rankDimension)
      this.rankDimension = dims[(idx + 1) % dims.length]
      this.fetchMyRank()
    },
    // 查询贷款 / 银行状态
    fetchLoanStatus() {
      request.get('/loan/status').then(function (res) {
        this.loanStatus = res.data || res
      }.bind(this)).catch(function () {})
    },
    claimStipend() {
      this.claimingStipend = true
      request.post('/loan/stipend').then(function (res) {
        var data = res.data || res
        this.$message.success('领取成功! +' + data.amount + ' 积分')
        this.fetchUserInfo()
        this.fetchLoanStatus()
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '领取失败'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.claimingStipend = false
      }.bind(this))
    },
    // 查询自上次访问以来新结算的投注（用于 tab 角标 + 行高亮）
    fetchSettledSince() {
      var lastVisit = localStorage.getItem(LAST_VISIT_KEY)
      var now = new Date().toISOString()
      if (lastVisit) {
        request.get('/bet/settled-since', { params: { since: lastVisit } }).then(function (res) {
          var data = res.data || res
          // 兼容 records/list/content 多种后端返回格式
          var list = extractList(data)
          this.settlementCount = list.length
          this.settledBetKeys = list.map(function (b) {
            return b.matchId + '_' + b.betSide
          })
          // Clear badge when user views the history tab
          if (this.settlementCount > 0) {
            this.$watch('activeTab', function (val) {
              if (val === 'history') {
                this.settlementCount = 0
              }
            })
          }
        }.bind(this)).catch(function () {})
      }
      localStorage.setItem(LAST_VISIT_KEY, now)
    },
    // 子组件通知：昵称更新成功，同步到本地 user 并持久化
    onUpdateProfile(nickname) {
      if (this.user) {
        this.user.nickname = nickname
        setUser(this.user)
      }
    },
    // 子组件通知：头像裁剪完成，同步到本地 user 并持久化
    onAvatarUpdated(url) {
      if (this.user) {
        this.user.avatar = url
        setUser(this.user)
      }
    }
  }
}
</script>

<style scoped>
.user-center {
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 28px;
  letter-spacing: 2px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

/* Tabs */
.user-tabs {
  margin-top: 4px;
}

.user-tabs :deep(.el-tabs__item) {
  color: var(--text-tertiary);
  transition: color var(--duration-fast) ease;
}

.user-tabs :deep(.el-tabs__item:hover) {
  color: var(--text-primary);
}

.user-tabs :deep(.el-tabs__item.is-active) {
  color: var(--accent-primary);
}

.user-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background: linear-gradient(90deg, var(--accent-primary), #00ffb3);
}

/* Settlement Notification Badge */
.settlement-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.15);
  border-radius: 10px;
  margin-left: 6px;
  line-height: 1;
  vertical-align: middle;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

/* Skeleton 布局容器样式（仅用于 Loading Skeleton 呈现，与对应子组件保持一致） */
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

.user-points {
  text-align: center;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
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

.skeleton .skeleton-line,
.skeleton > .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--avatar {
  width: 80px; height: 80px; border-radius: 50%; flex-shrink: 0;
  animation: shimmer 2s ease-in-out infinite, skeleton-avatar-pulse 2.5s ease-in-out infinite;
}
.skeleton-line--nickname { width: 120px; height: 24px; margin-bottom: 6px; }
.skeleton-line--date { width: 100px; height: 13px; }
.skeleton-line--points { width: 80px; height: 32px; margin: 0 auto 8px; }
.skeleton-line--label { width: 40px; height: 13px; margin: 0 auto; }
.skeleton-line--stat-val { width: 60%; height: 28px; margin: 0 auto 8px; }
.skeleton-line--stat-lbl { width: 40%; height: 12px; margin: 0 auto; }
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
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  border-radius: var(--radius-sm);
  height: 14px;
}

.skeleton-line--cell-lg { width: 180px; }
.skeleton-line--cell-md { width: 120px; }
.skeleton-line--cell-sm { width: 60px; }

/* Responsive（仅 skeleton 用到的容器响应式） */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .user-info-card {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  .user-avatar-section {
    flex-direction: column;
  }
}
</style>
