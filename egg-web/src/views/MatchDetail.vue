<template>
  <div class="match-detail" v-loading="loading">
    <!-- Skeleton loading screen -->
    <div v-if="loading" class="detail-skeleton">
      <!-- Match Info Skeleton -->
      <div class="skeleton-section">
        <div class="skeleton-line skeleton-line--lg"></div>
        <div class="skeleton-hero">
          <div class="skeleton-line skeleton-line--avatar-lg"></div>
          <div class="skeleton-line skeleton-line--score"></div>
          <div class="skeleton-line skeleton-line--avatar-lg"></div>
        </div>
        <div class="skeleton-line skeleton-line--md"></div>
      </div>
      <!-- Bet Panel Skeleton -->
      <div class="skeleton-section">
        <div class="skeleton-line skeleton-line--title"></div>
        <div class="skeleton-bet-cards">
          <div class="skeleton-line skeleton-line--card"></div>
          <div class="skeleton-line skeleton-line--vs"></div>
          <div class="skeleton-line skeleton-line--card"></div>
        </div>
        <div class="skeleton-line skeleton-line--slider"></div>
        <div class="skeleton-line skeleton-line--btn"></div>
      </div>
      <!-- Comments Skeleton -->
      <div class="skeleton-section">
        <div class="skeleton-line skeleton-line--title"></div>
        <div v-for="i in 3" :key="'sk-comment-'+i" class="skeleton-comment">
          <div class="skeleton-line skeleton-line--avatar-sm"></div>
          <div class="skeleton-comment-body">
            <div class="skeleton-line skeleton-line--md"></div>
            <div class="skeleton-line skeleton-line--sm"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="match" class="detail-content">
      <!-- 锚点导航 -->
      <nav class="detail-nav">
        <a class="detail-nav-link" href="#match-info" @click.prevent="scrollToSection('match-info')">比赛信息</a>
        <a class="detail-nav-link" v-if="match.status === 0 || match.status === 1" href="#bet-panel" @click.prevent="scrollToSection('bet-panel')">押注</a>
        <a class="detail-nav-link" href="#comments" @click.prevent="scrollToSection('comments')">评论</a>
      </nav>

      <!-- 比赛基本信息面板：队伍、logo、比分、时间、状态、联赛、结果 -->
      <div id="match-info"><MatchInfoPanel :match="match" /></div>

      <!-- 押注面板：押注表单、确认弹窗、修改弹窗、押注统计（仅未开始/进行中显示） -->
      <div id="bet-panel">
        <BetPanel
          v-if="match.status === 0 || match.status === 1"
          :match="match"
          :user-points="userPoints"
          :my-bet="myBet"
          :bet-count="betCount"
          @place-bet="onBetPlaced"
          @modify-bet="onBetModified"
          @broke-trigger="showBrokeModal = true"
        />
      </div>

      <!-- BrokeModal - 积分耗尽救济贷款弹窗 -->
      <BrokeModal
        :visible="showBrokeModal"
        @close="showBrokeModal = false"
        @success="onBrokeLoanSuccess"
      />

      <!-- 评论区：评论列表、发表评论、回复、WebSocket 实时推送 -->
      <div id="comments"><CommentSection :match-id="$route.params.id" :current-user="user" /></div>
    </div>

    <el-empty v-if="!loading && !match" description="赛事不存在" />
  </div>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { getToken, getUser } from '@/utils/auth'
import BrokeModal from '@/components/BrokeModal.vue'
import MatchInfoPanel from '@/components/match/MatchInfoPanel.vue'
import BetPanel from '@/components/match/BetPanel.vue'
import CommentSection from '@/components/match/CommentSection.vue'

export default {
  name: 'MatchDetail',
  components: {
    BrokeModal,
    MatchInfoPanel,
    BetPanel,
    CommentSection
  },
  data() {
    return {
      loading: false,
      // 比赛对象
      match: null,
      // 当前用户在该比赛上的押注记录（无则为 null）
      myBet: null,
      // 双方押注人数统计 { teamA, teamB }
      betCount: { teamA: 0, teamB: 0 },
      // 当前用户积分
      userPoints: 0,
      // 轮询定时器
      pollTimer: null,
      // 积分耗尽弹窗显隐
      showBrokeModal: false
    }
  },
  computed: {
    // 当前登录用户（从 localStorage 读取，用于传递给 CommentSection）
    user() {
      return getUser() || null
    }
  },
  created() {
    this.fetchMatch()
    this.fetchBetCount()
    if (getToken()) {
      this.fetchMyBet()
      this.fetchUserPoints()
    }
    this.startPolling()
  },
  beforeUnmount() {
    this.stopPolling()
    // WebSocket 取消订阅由 CommentSection 自行处理
  },
  methods: {
    // 锚点导航：平滑滚动到指定区域
    scrollToSection(id) {
      var el = document.getElementById(id)
      if (el) { el.scrollIntoView({ behavior: 'smooth', block: 'start' }) }
    },
    // 启动定时轮询比赛状态（每 30s 静默刷新）
    startPolling() {
      var self = this
      this.pollTimer = setInterval(function () {
        if (!document.hidden) {
          self.fetchMatch(true)
        }
      }, 30000)
    },
    stopPolling() {
      if (this.pollTimer) {
        clearInterval(this.pollTimer)
        this.pollTimer = null
      }
    },
    // 加载比赛详情（silent=true 时不显示 loading）
    fetchMatch(silent) {
      if (!silent) this.loading = true
      request.get('/match/' + this.$route.params.id).then(function (res) {
        this.match = res.data || res
        // WebSocket 订阅由 CommentSection 自行处理
      }.bind(this)).catch(() => { this.$message.error('加载失败') }).finally(function () {
        if (!silent) this.loading = false
      }.bind(this))
    },
    // 加载双方押注人数
    fetchBetCount() {
      request.get('/bet/count/' + this.$route.params.id).then(function (res) {
        var data = res.data || res
        this.betCount = {
          teamA: data.teamA || data.teamACount || data.sideA || 0,
          teamB: data.teamB || data.teamBCount || data.sideB || 0
        }
      }.bind(this)).catch(() => { this.$message.error('加载失败') })
    },
    // 加载当前用户的所有押注，筛选出当前比赛的
    fetchMyBet() {
      request.get('/bet/my').then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        var bets = extractList(data)
        var matchId = parseInt(this.$route.params.id)
        var found = bets.find(function (b) { return b.matchId === matchId })
        if (found) {
          this.myBet = found
        }
      }.bind(this)).catch(() => { this.$message.error('加载失败') })
    },
    // 加载当前用户积分
    fetchUserPoints() {
      request.get('/user/me').then(function (res) {
        var user = res.data || res
        this.userPoints = user.points || 0
      }.bind(this)).catch(() => { this.$message.error('加载失败') })
    },
    // 救济贷款发放成功回调：更新积分
    onBrokeLoanSuccess(data) {
      this.showBrokeModal = false
      // 刷新用户积分
      var newPoints = (data && data.points) ? data.points : 0
      if (newPoints > 0) {
        this.userPoints = newPoints
        this.$message.success('救济贷款已发放，当前积分：' + newPoints)
      } else {
        this.fetchUserPoints()
      }
    },
    // 押注成功回调：刷新比赛、我的押注、押注计数、用户积分
    onBetPlaced() {
      this.fetchMatch(true)
      this.fetchMyBet()
      this.fetchBetCount()
      this.fetchUserPoints()
    },
    // 修改押注成功回调：刷新我的押注、押注计数、用户积分
    onBetModified() {
      this.fetchMyBet()
      this.fetchBetCount()
      this.fetchUserPoints()
    }
  }
}
</script>

<style scoped>
/* ── Skeleton Loading ── */
.detail-skeleton {
  max-width: 800px;
  margin: 0 auto;
}

.skeleton-section {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 20px;
}

@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 25%, var(--bg-surface-3) 50%, var(--bg-surface-2) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--lg {
  height: 32px;
  width: 80%;
  margin-bottom: 16px;
}

.skeleton-line--md {
  height: 20px;
  width: 60%;
  margin-bottom: 12px;
}

.skeleton-line--sm {
  height: 14px;
  width: 40%;
}

.skeleton-line--title {
  height: 24px;
  width: 120px;
  margin-bottom: 20px;
}

.skeleton-line--btn {
  height: 44px;
  width: 100%;
  border-radius: var(--radius-md);
  margin-top: 12px;
}

.skeleton-line--slider {
  height: 36px;
  width: 100%;
  border-radius: var(--radius-md);
  margin: 16px 0;
}

.skeleton-line--card {
  height: 120px;
  flex: 1;
  border-radius: var(--radius-lg);
}

.skeleton-line--vs {
  width: 32px;
  height: 20px;
  flex-shrink: 0;
}

.skeleton-line--avatar-lg {
  width: 64px;
  height: 64px;
  border-radius: 50%;
}

.skeleton-line--avatar-sm {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-line--score {
  width: 80px;
  height: 40px;
  border-radius: var(--radius-sm);
}

.skeleton-hero {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-bottom: 16px;
}

.skeleton-bet-cards {
  display: flex;
  align-items: stretch;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-comment {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
}

.skeleton-comment-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Anchor Navigation */
.detail-nav {
  position: sticky;
  top: 56px;
  z-index: 50;
  display: flex;
  gap: 4px;
  padding: 8px 0;
  margin-bottom: 16px;
  background: var(--bg-page);
  border-bottom: 1px solid var(--border-subtle);
  overflow-x: auto;
  scrollbar-width: none;
}

.detail-nav::-webkit-scrollbar {
  display: none;
}

.detail-nav-link {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  white-space: nowrap;
  transition: color 0.15s ease, background 0.15s ease;
}

.detail-nav-link:hover {
  color: var(--text-primary);
  background: var(--bg-surface-1);
}

.detail-nav-link:active {
  color: var(--accent-primary);
}
</style>

