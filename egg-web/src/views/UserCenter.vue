<template>
  <div class="user-center">
    <header class="page-header">
      <div class="page-eyebrow">// ACCOUNT · USER CENTER</div>
      <h1 class="page-title">个人中心</h1>
      <p class="page-sub">身份资料 · 战绩数据 · 投注流水</p>
    </header>

    <!-- Loading Skeleton -->
    <template v-if="loading">
      <div class="user-skeleton">
        <div class="sk-tabs">
          <div class="sk-line shimmer-bg sk-tab"></div>
          <div class="sk-line shimmer-bg sk-tab"></div>
          <div class="sk-line shimmer-bg sk-tab"></div>
        </div>

        <div class="sk-identity u-panel">
          <div class="sk-line shimmer-bg sk-avatar"></div>
          <div class="sk-identity-meta">
            <div class="sk-line shimmer-bg sk-name"></div>
            <div class="sk-chips">
              <div class="sk-line shimmer-bg sk-chip"></div>
              <div class="sk-line shimmer-bg sk-chip"></div>
            </div>
            <div class="sk-line shimmer-bg sk-sub"></div>
          </div>
        </div>

        <div class="sk-stats">
          <div v-for="i in 4" :key="'stat-skel-'+i" class="sk-stat u-panel">
            <div class="sk-line shimmer-bg sk-kicker"></div>
            <div class="sk-line shimmer-bg sk-value"></div>
            <div class="sk-line shimmer-bg sk-label"></div>
          </div>
        </div>

        <div class="sk-panel u-panel">
          <div class="sk-panel-head">
            <div class="sk-line shimmer-bg sk-section-title"></div>
            <div class="sk-line shimmer-bg sk-section-meta"></div>
          </div>
          <div v-for="i in 4" :key="'bet-skel-'+i" class="sk-row">
            <div class="sk-line shimmer-bg sk-cell-lg"></div>
            <div class="sk-line shimmer-bg sk-cell-sm"></div>
            <div class="sk-line shimmer-bg sk-cell-sm"></div>
            <div class="sk-line shimmer-bg sk-cell-md"></div>
          </div>
        </div>
      </div>
    </template>

    <div v-else-if="user" class="user-content">
      <!-- Tab Navigation -->
      <el-tabs v-model="activeTab" class="user-tabs">
        <el-tab-pane name="profile">
          <template #label>
            <span class="tab-label"><span class="tab-idx" aria-hidden="true">01</span>个人资料</span>
          </template>
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

        <el-tab-pane name="dashboard">
          <template #label>
            <span class="tab-label"><span class="tab-idx" aria-hidden="true">02</span>数据仪表盘</span>
          </template>
          <!-- 数据仪表盘：趋势条 / 胜率 / 连胜 / 战绩图 / 收益曲线 -->
          <stats-dashboard :user="user" />
        </el-tab-pane>

        <el-tab-pane name="history">
          <template #label>
            <span class="tab-label">
              <span class="tab-idx" aria-hidden="true">03</span>投注记录
              <span v-if="settlementCount > 0" class="settlement-badge">{{ settlementCount }}</span>
            </span>
          </template>
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
  max-width: 960px;
  margin: 0 auto;
}

/* ---------- Page header ---------- */
.page-header {
  position: relative;
  margin-bottom: 24px;
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
  box-shadow: 0 0 12px var(--brand-line);
}

.page-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.page-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}

/* ---------- Tabs ---------- */
.user-tabs {
  margin-top: 4px;
}

.user-tabs :deep(.el-tabs__header) {
  margin-bottom: 26px;
}

.user-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px !important;
  background: var(--border-subtle) !important;
}

.user-tabs :deep(.el-tabs__item) {
  height: 46px !important;
  padding: 0 20px !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  letter-spacing: 0.04em;
  color: var(--text-tertiary) !important;
  transition: color var(--duration-fast) ease;
}

.user-tabs :deep(.el-tabs__item:hover) {
  color: var(--text-primary) !important;
}

.user-tabs :deep(.el-tabs__item.is-active) {
  color: var(--text-primary) !important;
}

.user-tabs :deep(.el-tabs__active-bar) {
  height: 2px !important;
  background: linear-gradient(90deg, var(--brand), var(--brand-deep)) !important;
  box-shadow: 0 0 12px var(--brand-line);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tab-idx {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-faint);
  transition: color var(--duration-fast) ease;
}

.user-tabs :deep(.el-tabs__item.is-active) .tab-idx {
  color: var(--brand);
}

/* Settlement Notification Badge */
.settlement-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 17px;
  padding: 0 5px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  color: var(--cream);
  background: var(--brand);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 10px var(--brand-line);
}

/* ---------- Loading skeleton ---------- */
.user-skeleton {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sk-tabs {
  display: flex;
  align-items: center;
  gap: 28px;
  height: 46px;
  margin-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.sk-tab {
  width: 74px;
  height: 12px;
}

.sk-tab:nth-child(1) {
  width: 86px;
}

.sk-tab:nth-child(2) {
  width: 100px;
}

.sk-line {
  border-radius: var(--radius-sm);
}

.sk-identity {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 22px 24px;
  min-height: 132px;
}

.sk-avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sk-identity-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-name {
  width: 180px;
  max-width: 60%;
  height: 24px;
}

.sk-chips {
  display: flex;
  gap: 8px;
}

.sk-chip {
  width: 64px;
  height: 20px;
}

.sk-sub {
  width: 140px;
  max-width: 50%;
  height: 12px;
}

.sk-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.sk-stat {
  padding: 16px 16px 14px;
  min-height: 104px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-kicker {
  width: 40%;
  height: 9px;
}

.sk-value {
  width: 62%;
  height: 26px;
}

.sk-label {
  width: 34%;
  height: 11px;
}

.sk-panel {
  padding: 20px 22px 8px;
}

.sk-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.sk-section-title {
  width: 120px;
  height: 20px;
}

.sk-section-meta {
  width: 70px;
  height: 10px;
}

.sk-row {
  display: flex;
  align-items: center;
  gap: 16px;
  height: 52px;
  border-bottom: 1px solid var(--border-subtle);
}

.sk-row:last-child {
  border-bottom: none;
}

.sk-cell-lg {
  width: 34%;
  height: 13px;
}

.sk-cell-md {
  width: 18%;
  height: 13px;
}

.sk-cell-sm {
  width: 10%;
  height: 13px;
  margin-left: auto;
}

.sk-cell-sm + .sk-cell-sm {
  margin-left: 0;
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .page-title {
    font-size: 28px;
  }

  .sk-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sk-identity {
    min-height: 116px;
    padding: 18px;
  }

  .sk-avatar {
    width: 76px;
    height: 76px;
  }
}

@media (max-width: 480px) {
  .user-tabs :deep(.el-tabs__header) {
    margin-bottom: 20px;
  }

  .user-tabs :deep(.el-tabs__item) {
    padding: 0 11px !important;
    height: 42px !important;
  }

  .tab-idx {
    display: none;
  }

  .tab-label {
    gap: 5px;
  }

  .settlement-badge {
    margin-left: 1px;
  }

  .sk-cell-md {
    display: none;
  }
}
</style>
