<template>
  <div class="match-detail" v-loading="loading">
    <div v-if="match" class="detail-content">
      <!-- 比赛基本信息面板：队伍、logo、比分、时间、状态、联赛、结果 -->
      <MatchInfoPanel :match="match" />

      <!-- 押注面板：押注表单、确认弹窗、修改弹窗、押注统计（仅未开始/进行中显示） -->
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

      <!-- BrokeModal - 积分耗尽救济贷款弹窗 -->
      <BrokeModal
        :visible="showBrokeModal"
        @close="showBrokeModal = false"
        @success="onBrokeLoanSuccess"
      />

      <!-- 评论区：评论列表、发表评论、回复、WebSocket 实时推送 -->
      <CommentSection :match-id="$route.params.id" :current-user="user" />
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
