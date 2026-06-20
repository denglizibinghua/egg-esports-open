<template>
  <div class="match-detail" v-loading="loading">
    <div v-if="match" class="detail-content">
      <div class="detail-header">
        <el-button type="text" icon="el-icon-arrow-left" class="back-btn" @click="$router.back()">返回</el-button>
        <span v-if="match.status === 0" class="status-badge status-badge--upcoming">
          <span class="status-dot"></span>未开始
        </span>
        <span v-else-if="match.status === 1" class="status-badge status-badge--live">
          <span class="status-dot status-dot--pulse"></span>进行中
        </span>
        <span v-else-if="match.status === 2" class="status-badge status-badge--finished">已结束</span>
      </div>

      <div class="match-info-card">
        <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
        <div class="match-time">{{ formatTime(match.matchTime) }}</div>

        <div class="teams-display">
          <div class="team-display team-a-display">
            <div class="team-logo-large">
              <img v-if="match.teamALogo" :src="match.teamALogo" alt="">
              <span v-else class="team-logo-placeholder-large">{{ match.teamA === 'TBD' ? '?' : (match.teamA || '').charAt(0) }}</span>
            </div>
            <div class="team-name-large" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
          </div>

          <div class="vs-section">
            <div class="vs-text">VS</div>
          </div>

          <div class="team-display team-b-display">
            <div class="team-logo-large">
              <img v-if="match.teamBLogo" :src="match.teamBLogo" alt="">
              <span v-else class="team-logo-placeholder-large">{{ match.teamB === 'TBD' ? '?' : (match.teamB || '').charAt(0) }}</span>
            </div>
            <div class="team-name-large" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
          </div>
        </div>

        <!-- Score Display -->
        <div v-if="hasScore(match)" class="score-display-large" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
          <span class="score-large-a">{{ match.scoreA }}</span>
          <span class="score-large-sep">-</span>
          <span class="score-large-b">{{ match.scoreB }}</span>
        </div>

        <div v-if="match.status === 2 && match.result !== null && match.result !== undefined" class="match-result-display">
          <template v-if="match.result === 0">
            <span class="winner-name">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span> 获胜!
          </template>
          <template v-else-if="match.result === 1">
            <span class="winner-name">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span> 获胜!
          </template>
          <template v-else-if="match.result === 2">
            平局
          </template>
        </div>
      </div>

      <!-- Betting Panel -->
      <div v-if="match.status === 0 || match.status === 1" class="bet-panel">
        <h3 class="panel-title">押注</h3>

        <!-- TBD Notice -->
        <div v-if="match.teamA === 'TBD' || match.teamB === 'TBD'" class="bet-tbd-notice">
          <div class="tbd-icon">⏳</div>
          <p class="tbd-text">参赛队伍尚未确定，暂不支持押注</p>
        </div>

        <!-- Not Logged In Prompt -->
        <div v-else-if="!getToken()" class="bet-login-prompt">
          <div class="prompt-icon">🔒</div>
          <p class="prompt-text">登录后即可参与竞猜</p>
          <el-button type="primary" class="prompt-btn" @click="$router.push('/login')">去登录</el-button>
        </div>

        <template v-else>
          <div v-if="myBet" class="already-bet">
            <el-alert
              :title="'你已押注 ' + (myBet.betSide === 0 ? match.teamA : match.teamB) + ' ' + myBet.betAmount + ' 积分'"
              type="warning"
              :closable="false"
              show-icon
            />
            <el-button
              v-if="match.status === 0"
              type="warning"
              size="small"
              class="modify-bet-btn"
              @click="openModifyDialog"
            >修改押注</el-button>
          </div>

          <div v-else class="bet-form">
            <div class="bet-side-select">
              <div class="bet-side-label">选择押注队伍</div>
              <div class="bet-side-cards">
                <div
                  class="team-card"
                  :class="{ 'team-card--selected': betForm.betSide === 0 }"
                  @click="betForm.betSide = 0"
                >
                  <div class="team-card-logo">
                    <img v-if="match.teamALogo" :src="match.teamALogo" alt="">
                    <span v-else class="team-card-logo-placeholder">{{ (match.teamA || '').charAt(0) }}</span>
                  </div>
                  <div class="team-card-name">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
                  <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">{{ match.oddsA.toFixed(2) }}</div>
                  <div v-if="betForm.betSide === 0" class="team-card-check">&#10003;</div>
                </div>
                <div class="team-card-vs">VS</div>
                <div
                  class="team-card"
                  :class="{ 'team-card--selected': betForm.betSide === 1 }"
                  @click="betForm.betSide = 1"
                >
                  <div class="team-card-logo">
                    <img v-if="match.teamBLogo" :src="match.teamBLogo" alt="">
                    <span v-else class="team-card-logo-placeholder">{{ (match.teamB || '').charAt(0) }}</span>
                  </div>
                  <div class="team-card-name">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
                  <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">{{ match.oddsB.toFixed(2) }}</div>
                  <div v-if="betForm.betSide === 1" class="team-card-check">&#10003;</div>
                </div>
              </div>
            </div>

            <div class="bet-amount-section">
              <div class="bet-amount-label">
                押注积分
                <span class="current-points">当前积分: {{ userPoints }}</span>
              </div>
              <el-slider
                v-model="betForm.betAmount"
                :min="1"
                :max="Math.max(userPoints, 1)"
                :step="1"
                show-input
                :show-input-controls="false"
              />
              <div class="estimated-return" v-if="betForm.betSide !== null && betForm.betAmount > 0 && match">
                <span class="est-label">预估返还</span>
                <span class="est-amount">{{ estimatedReturn }} 积分</span>
                <span class="est-odds">(预估赔率 {{ currentSelectedOdds }})</span>
              </div>
            </div>

            <el-button
              type="primary"
              class="bet-btn"
              :loading="betLoading"
              :disabled="betForm.betSide === null || betForm.betAmount <= 0"
              @click="placeBet"
            >
              确认押注
            </el-button>
          </div>
        </template>

        <div class="bet-stats">
          <div class="bet-stat">
            <span class="stat-label" :class="{ 'stat-label-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span>
            <span class="stat-value">{{ betCount.teamA }} 人押注</span>
          </div>
          <div class="bet-stat">
            <span class="stat-label" :class="{ 'stat-label-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span>
            <span class="stat-value">{{ betCount.teamB }} 人押注</span>
          </div>
        </div>
      </div>

      <!-- Comments Panel -->
      <div class="comments-panel">
        <h3 class="panel-title">
          <i class="el-icon-chat-dot-square"></i>
          <span>评论</span>
          <span class="comment-count">{{ commentTotal }}</span>
        </h3>

        <!-- Not Logged In -->
        <div v-if="!getToken()" class="bet-login-prompt">
          <div class="prompt-icon">🔒</div>
          <p class="prompt-text">登录后参与评论</p>
          <el-button type="primary" class="prompt-btn" @click="$router.push('/login')">去登录</el-button>
        </div>

        <template v-else>
          <!-- Comment Input -->
          <div class="comment-input-wrap">
            <div v-if="replyTarget" class="reply-hint">
              <span>回复 @{{ replyTarget.userNickname }}</span>
              <el-button type="text" size="mini" @click="cancelReply">取消</el-button>
            </div>
            <el-input
              v-model="commentInput"
              type="textarea"
              :rows="3"
              :placeholder="replyTarget ? '回复 @' + replyTarget.userNickname + '...' : '发表你的看法（支持 @昵称 提及）...'"
              maxlength="500"
              show-word-limit
            />
            <div class="comment-input-actions">
              <el-button
                type="primary"
                size="small"
                :loading="commentPosting"
                :disabled="!commentInput.trim()"
                @click="postComment"
              >发送</el-button>
            </div>
          </div>

          <!-- Comment List -->
          <div v-if="comments.length === 0 && !commentsLoading" class="comment-empty">
            <el-empty description="还没有评论，快来抢沙发" />
          </div>

          <div v-else class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-avatar">
                <img v-if="comment.userAvatar" :src="comment.userAvatar" alt="">
                <span v-else class="avatar-placeholder">{{ (comment.userNickname || '?').charAt(0) }}</span>
              </div>
              <div class="comment-body">
                <div class="comment-header">
                  <span class="comment-nickname">{{ comment.userNickname || '匿名用户' }}</span>
                  <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
                </div>
                <div class="comment-content" v-html="renderContent(comment.content)"></div>
                <div class="comment-actions">
                  <el-button type="text" size="mini" @click="startReply(comment)">回复</el-button>
                  <el-button
                    v-if="user && comment.userId === user.id"
                    type="text"
                    size="mini"
                    class="delete-btn"
                    @click="deleteComment(comment)"
                  >删除</el-button>
                </div>

                <!-- Nested Replies -->
                <div v-if="comment.replies && comment.replies.length > 0" class="reply-list">
                  <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
                    <div class="comment-avatar comment-avatar--sm">
                      <img v-if="reply.userAvatar" :src="reply.userAvatar" alt="">
                      <span v-else class="avatar-placeholder">{{ (reply.userNickname || '?').charAt(0) }}</span>
                    </div>
                    <div class="comment-body">
                      <div class="comment-header">
                        <span class="comment-nickname">{{ reply.userNickname || '匿名用户' }}</span>
                        <span v-if="reply.replyToNickname" class="reply-to">回复 @{{ reply.replyToNickname }}</span>
                        <span class="comment-time">{{ formatTime(reply.createTime) }}</span>
                      </div>
                      <div class="comment-content" v-html="renderContent(reply.content)"></div>
                      <div class="comment-actions">
                        <el-button type="text" size="mini" @click="startReply(reply, comment)">回复</el-button>
                        <el-button
                          v-if="user && reply.userId === user.id"
                          type="text"
                          size="mini"
                          class="delete-btn"
                          @click="deleteComment(reply)"
                        >删除</el-button>
                      </div>
                    </div>
                  </div>
                  <el-button
                    v-if="comment.replies.length < comment.replyTotal"
                    type="text"
                    size="mini"
                    class="load-more-replies"
                    @click="loadMoreReplies(comment)"
                  >展开更多回复 ({{ comment.replyTotal - comment.replies.length }})</el-button>
                </div>
              </div>
            </div>
          </div>

          <el-pagination
            v-if="commentTotal > commentPageSize"
            background
            layout="prev, pager, next"
            :total="commentTotal"
            :page-size="commentPageSize"
            :current-page.sync="commentPage"
            @current-change="fetchComments"
            class="comment-pagination"
          />
        </template>
      </div>
    </div>

    <!-- Modify Bet Dialog -->
    <el-dialog
      title="修改押注"
      :visible.sync="showModifyBetDialog"
      width="400px"
      :close-on-click-modal="false"
      custom-class="dark-dialog"
    >
      <div class="modify-bet-form">
        <div class="bet-side-select">
          <div class="bet-side-label">选择押注队伍</div>
          <div class="bet-side-cards">
            <div
              class="team-card"
              :class="{ 'team-card--selected': modifyForm.betSide === 0 }"
              @click="modifyForm.betSide = 0"
            >
              <div class="team-card-logo">
                <img v-if="match && match.teamALogo" :src="match.teamALogo" alt="">
                <span v-else class="team-card-logo-placeholder">{{ (match ? match.teamA : '').charAt(0) }}</span>
              </div>
              <div class="team-card-name">{{ match && match.teamA === 'TBD' ? '待定' : (match ? match.teamA : '') }}</div>
              <div class="team-card-odds" v-if="match && match.oddsA !== null && match.oddsA !== undefined">{{ match.oddsA.toFixed(2) }}</div>
              <div v-if="modifyForm.betSide === 0" class="team-card-check">&#10003;</div>
            </div>
            <div class="team-card-vs">VS</div>
            <div
              class="team-card"
              :class="{ 'team-card--selected': modifyForm.betSide === 1 }"
              @click="modifyForm.betSide = 1"
            >
              <div class="team-card-logo">
                <img v-if="match && match.teamBLogo" :src="match.teamBLogo" alt="">
                <span v-else class="team-card-logo-placeholder">{{ (match ? match.teamB : '').charAt(0) }}</span>
              </div>
              <div class="team-card-name">{{ match && match.teamB === 'TBD' ? '待定' : (match ? match.teamB : '') }}</div>
              <div class="team-card-odds" v-if="match && match.oddsB !== null && match.oddsB !== undefined">{{ match.oddsB.toFixed(2) }}</div>
              <div v-if="modifyForm.betSide === 1" class="team-card-check">&#10003;</div>
            </div>
          </div>
        </div>

        <div class="bet-amount-section">
          <div class="bet-amount-label">
            押注积分
            <span class="current-points">可用积分: {{ modifyMaxAmount }}</span>
          </div>
          <el-slider
            v-model="modifyForm.betAmount"
            :min="1"
            :max="Math.max(modifyMaxAmount, 1)"
            :step="1"
            show-input
            :show-input-controls="false"
          />
          <div class="estimated-return" v-if="modifyForm.betSide !== null && modifyForm.betAmount > 0 && match">
            <span class="est-label">预估返还</span>
            <span class="est-amount">{{ modifyEstimatedReturn }} 积分</span>
            <span class="est-odds">(预估赔率 {{ modifyCurrentOdds }})</span>
          </div>
        </div>
      </div>

      <span slot="footer">
        <el-button @click="showModifyBetDialog = false">取消</el-button>
        <el-button type="primary" :loading="modifying" @click="submitModifyBet">确认修改</el-button>
      </span>
    </el-dialog>

    <el-empty v-if="!loading && !match" description="赛事不存在" />
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken, getUser } from '@/utils/auth'
import { subscribe as wsSubscribe, unsubscribe as wsUnsubscribe } from '@/utils/websocket'
import DOMPurify from 'dompurify'

export default {
  name: 'MatchDetail',
  data() {
    return {
      loading: false,
      match: null,
      myBet: null,
      betCount: { teamA: 0, teamB: 0 },
      betForm: {
        betSide: null,
        betAmount: 50
      },
      betLoading: false,
      userPoints: 0,
      pollTimer: null,
      showModifyBetDialog: false,
      modifying: false,
      modifyForm: {
        betSide: 0,
        betAmount: 0
      },
      // 评论
      comments: [],
      commentsLoading: false,
      commentTotal: 0,
      commentPage: 1,
      commentPageSize: 20,
      commentInput: '',
      commentPosting: false,
      replyTarget: null,
      replyParent: null
    }
  },
  computed: {
    user() {
      return getUser() || null
    },
    modifyMaxAmount() {
      return this.userPoints + (this.myBet ? this.myBet.betAmount : 0)
    },
    currentSelectedOdds() {
      if (this.betForm.betSide === 0) return this.match ? (this.match.oddsA || 0).toFixed(2) : '0.00'
      if (this.betForm.betSide === 1) return this.match ? (this.match.oddsB || 0).toFixed(2) : '0.00'
      return '0.00'
    },
    estimatedReturn() {
      if (this.betForm.betSide === null || !this.match) return 0
      var odds = this.betForm.betSide === 0 ? (this.match.oddsA || 0) : (this.match.oddsB || 0)
      return Math.floor(this.betForm.betAmount * Number(odds))
    },
    modifyCurrentOdds() {
      if (this.modifyForm.betSide === 0) return this.match ? (this.match.oddsA || 0).toFixed(2) : '0.00'
      if (this.modifyForm.betSide === 1) return this.match ? (this.match.oddsB || 0).toFixed(2) : '0.00'
      return '0.00'
    },
    modifyEstimatedReturn() {
      if (this.modifyForm.betSide === null || !this.match) return 0
      var odds = this.modifyForm.betSide === 0 ? (this.match.oddsA || 0) : (this.match.oddsB || 0)
      return Math.floor(this.modifyForm.betAmount * Number(odds))
    }
  },
  created() {
    this.fetchMatch()
    this.fetchBetCount()
    if (getToken()) {
      this.fetchMyBet()
      this.fetchUserPoints()
    }
    this.fetchComments()
    this.startPolling()
  },
  beforeDestroy() {
    this.stopPolling()
    if (this.match) {
      wsUnsubscribe('/topic/match/' + this.match.id)
    }
  },
  watch: {
    showModifyBetDialog(val) {
      if (val && this.myBet) {
        this.modifyForm.betSide = this.myBet.betSide
        this.modifyForm.betAmount = this.myBet.betAmount
      }
    }
  },
  methods: {
    getToken() {
      return getToken()
    },
    hasScore(match) {
      return (match.status === 1 || match.status === 2) &&
        match.scoreA !== null && match.scoreA !== undefined &&
        match.scoreB !== null && match.scoreB !== undefined
    },
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
    fetchMatch(silent) {
      if (!silent) this.loading = true
      request.get('/match/' + this.$route.params.id).then(function (res) {
        this.match = res.data || res
        // 订阅评论 WebSocket
        if (this.match && this.match.id) {
          wsSubscribe('/topic/match/' + this.match.id, this.onWsComment)
        }
      }.bind(this)).catch(function () {}).finally(function () {
        if (!silent) this.loading = false
      }.bind(this))
    },
    fetchBetCount() {
      request.get('/bet/count/' + this.$route.params.id).then(function (res) {
        var data = res.data || res
        this.betCount = {
          teamA: data.teamA || data.teamACount || data.sideA || 0,
          teamB: data.teamB || data.teamBCount || data.sideB || 0
        }
      }.bind(this)).catch(function () {})
    },
    fetchMyBet() {
      request.get('/bet/my').then(function (res) {
        var data = res.data || res
        var bets = Array.isArray(data) ? data : (data.records || data.list || [])
        var matchId = parseInt(this.$route.params.id)
        var found = bets.find(function (b) { return b.matchId === matchId })
        if (found) {
          this.myBet = found
        }
      }.bind(this)).catch(function () {})
    },
    fetchUserPoints() {
      request.get('/user/me').then(function (res) {
        var user = res.data || res
        this.userPoints = user.points || 0
      }.bind(this)).catch(function () {})
    },
    placeBet() {
      if (this.betForm.betSide === null) {
        this.$message.warning('请选择押注队伍')
        return
      }
      if (this.betForm.betAmount <= 0) {
        this.$message.warning('请输入押注积分')
        return
      }
      this.betLoading = true
      request.post('/bet/place', {
        matchId: parseInt(this.$route.params.id),
        betSide: this.betForm.betSide,
        betAmount: this.betForm.betAmount
      }).then(function () {
        this.$message.success('押注成功! 赔率以结算时最终奖池为准')
        this.fetchMatch(true)
        this.fetchMyBet()
        this.fetchBetCount()
        this.fetchUserPoints()
      }.bind(this)).catch(function () {}).finally(function () {
        this.betLoading = false
      }.bind(this))
    },
    openModifyDialog() {
      if (this.myBet) {
        this.modifyForm.betSide = this.myBet.betSide
        this.modifyForm.betAmount = this.myBet.betAmount
      }
      this.showModifyBetDialog = true
    },
    submitModifyBet() {
      if (this.modifyForm.betSide === null || this.modifyForm.betSide === undefined) {
        this.$message.warning('请选择押注队伍')
        return
      }
      if (this.modifyForm.betAmount <= 0) {
        this.$message.warning('请输入押注积分')
        return
      }
      if (this.modifyForm.betSide === this.myBet.betSide && this.modifyForm.betAmount === this.myBet.betAmount) {
        this.$message.info('未修改任何内容')
        return
      }
      this.modifying = true
      request.put('/bet/' + this.myBet.id, {
        betSide: this.modifyForm.betSide,
        betAmount: this.modifyForm.betAmount
      }).then(function () {
        this.$message.success('修改成功')
        this.showModifyBetDialog = false
        this.fetchMyBet()
        this.fetchBetCount()
        this.fetchUserPoints()
      }.bind(this)).catch(function (err) {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '修改失败，请重试'
        this.$message.error(msg)
      }.bind(this)).finally(function () {
        this.modifying = false
      }.bind(this))
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var month = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var hour = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + month + '-' + day + ' ' + hour + ':' + min
    },
    // ===== 评论相关 =====
    fetchComments(silent) {
      if (!silent) this.commentsLoading = true
      request.get('/comment/list', {
        params: {
          matchId: this.$route.params.id,
          page: this.commentPage,
          size: this.commentPageSize
        }
      }).then(function (res) {
        var data = res.data || res
        var records = data.records || data.list || []
        // 填充 replyTotal（后端未返回时默认为 replies 长度）
        records.forEach(function (c) {
          if (c.replyTotal == null) c.replyTotal = (c.replies || []).length
        })
        this.comments = records
        this.commentTotal = data.total || 0
      }.bind(this)).catch(function () {}).finally(function () {
        this.commentsLoading = false
      }.bind(this))
    },
    postComment() {
      if (!this.commentInput.trim()) return
      this.commentPosting = true
      var body = {
        matchId: parseInt(this.$route.params.id),
        content: this.commentInput.trim()
      }
      if (this.replyTarget) {
        body.parentId = (this.replyParent || this.replyTarget).id
        body.replyToId = this.replyTarget.id
        body.replyToUid = this.replyTarget.userId
      }
      request.post('/comment/post', body).then(function () {
        this.$message.success('评论成功')
        this.commentInput = ''
        this.cancelReply()
        this.fetchComments(true)
      }.bind(this)).catch(function () {}).finally(function () {
        this.commentPosting = false
      }.bind(this))
    },
    startReply(comment, parent) {
      this.replyTarget = comment
      this.replyParent = parent || null
    },
    cancelReply() {
      this.replyTarget = null
      this.replyParent = null
    },
    deleteComment(comment) {
      this.$confirm('确定删除这条评论？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(function () {
        request.delete('/comment/' + comment.id).then(function () {
          this.$message.success('删除成功')
          this.fetchComments(true)
        }.bind(this)).catch(function () {})
      }.bind(this)).catch(function () {})
    },
    loadMoreReplies(comment) {
      request.get('/comment/replies', {
        params: { parentId: comment.id, page: 1, size: 100 }
      }).then(function (res) {
        var data = res.data || res
        var records = data.records || data.list || []
        comment.replies = records
        // 保持 replyTotal 为后端返回的总数，不覆盖
      }.bind(this)).catch(function () {})
    },
    renderContent(content) {
      if (!content) return ''
      // 先用 DOMPurify 剥掉所有 HTML 标签（XSS 防御）
      var cleaned = DOMPurify.sanitize(content, { ALLOWED_TAGS: [] })
      // @提及高亮
      return cleaned.replace(/@(\S+)/g, '<span class="at-mention">@$1</span>')
    },
    onWsComment(newComment) {
      if (!newComment) return
      if (newComment.parentId) {
        // 回复：找到父评论 push 到 replies
        var parent = this.comments.find(function (c) { return c.id === newComment.parentId })
        if (parent) {
          if (!parent.replies) parent.replies = []
          parent.replies.push(newComment)
          parent.replyTotal = (parent.replyTotal || 0) + 1
        }
      } else {
        // 顶级评论：unshift 到列表顶
        if (!newComment.replies) newComment.replies = []
        if (newComment.replyTotal == null) newComment.replyTotal = 0
        this.comments.unshift(newComment)
        this.commentTotal++
      }
    }
  }
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  color: var(--text-tertiary) !important;
  font-size: 14px;
  transition: color var(--duration-fast) ease;
}

.back-btn:hover {
  color: var(--accent-primary) !important;
}

.match-info-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 40px 32px;
  text-align: center;
  box-shadow: var(--shadow-md);
}

.match-time {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
  margin-bottom: 32px;
}

.teams-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  margin-bottom: 24px;
}

.team-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.team-logo-large {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-default);
  transition: border-color var(--duration-normal) ease;
}

.team-logo-large img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-logo-placeholder-large {
  font-size: 28px;
  font-weight: 700;
  color: var(--accent-primary);
}

.team-name-large {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.vs-section {
  flex-shrink: 0;
}

.vs-text {
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--accent-primary);
  letter-spacing: 3px;
}

/* Large Score Display */
.score-display-large {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  font-family: var(--font-mono);
  font-size: 40px;
  font-weight: 800;
  margin-top: 8px;
  margin-bottom: 16px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;
  animation: score-reveal 0.5s var(--ease-out) both;
}

@keyframes score-reveal {
  from {
    opacity: 0;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.score--live {
  color: var(--accent-primary);
  text-shadow: 0 0 20px rgba(0, 229, 160, 0.35);
}

.score--finished {
  color: var(--text-secondary);
}

.score-large-a,
.score-large-b {
  font-variant-numeric: tabular-nums;
  min-width: 40px;
  text-align: center;
}

.score-large-sep {
  opacity: 0.4;
  font-weight: 400;
  font-size: 32px;
}

.match-result-display {
  font-size: 20px;
  font-weight: 600;
  color: var(--accent-primary);
  margin-top: 16px;
}

.winner-name {
  color: var(--accent-primary);
}

/* Betting Panel - Glass Effect */
.bet-panel {
  background: rgba(18,18,26,0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  padding: 28px;
  margin-top: 24px;
  position: relative;
  overflow: hidden;
}

.panel-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 1px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border-subtle);
  padding-bottom: 12px;
}

.already-bet {
  margin-bottom: 16px;
}

.bet-form {
  margin-bottom: 20px;
}

.bet-side-select {
  margin-bottom: 24px;
}

.bet-side-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 14px;
}

.bet-side-cards {
  display: flex;
  align-items: stretch;
  gap: 12px;
}

.team-card {
  flex: 1;
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-height: 120px;
  padding: 20px 16px;
  background: #1a1a24;
  border: 1px solid #2a2a3a;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-normal) ease;
  user-select: none;
}

.team-card:hover {
  background: #1f1f2c;
  border-color: #3a3a4a;
}

.team-card--selected {
  background: #151a17;
  border-color: #3d5a40;
  box-shadow: 0 0 24px rgba(60,100,65,0.20);
}

.team-card--selected:hover {
  background: #151a17;
  border-color: #3d5a40;
}

.team-card-logo {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: #252533;
  border: 1px solid #2a2a3a;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  flex-shrink: 0;
}

.team-card-logo img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-card-logo-placeholder {
  font-size: 20px;
  font-weight: 700;
  color: #5b8c5a;
}

.team-card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  text-align: center;
  line-height: 1.3;
}

.team-card-check {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: #5b8c5a;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.team-card-vs {
  width: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 12px;
  color: #4a4a5a;
  font-weight: 600;
  letter-spacing: 1px;
}

.bet-amount-section {
  margin-bottom: 24px;
}

.bet-amount-label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 12px;
}

.current-points {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-weight: 600;
  margin-left: 8px;
}

.bet-btn {
  width: 100%;
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-size: 15px;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  box-shadow: 0 0 20px rgba(0,229,160,0.20);
  transition: transform var(--duration-fast) var(--ease-out),
              box-shadow var(--duration-fast) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.bet-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.15), transparent);
  transition: left 0.5s ease;
}

.bet-btn:hover {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0,229,160,0.35);
}

.bet-btn:hover::after {
  left: 100%;
}

.bet-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 20px;
  border-top: 1px solid var(--border-subtle);
}

.bet-stat {
  text-align: center;
}

.stat-label {
  display: block;
  color: var(--text-primary);
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}

.stat-value {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
}

/* Bet Login Prompt */
.bet-login-prompt {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
  text-align: center;
}

.prompt-icon {
  font-size: 36px;
  margin-bottom: 12px;
  opacity: 0.7;
}

.prompt-text {
  font-size: 16px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 16px;
}

.prompt-btn {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 32px;
}

/* TBD Notice */
.bet-tbd-notice {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 24px;
  text-align: center;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.tbd-icon {
  font-size: 32px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.tbd-text {
  font-size: 15px;
  color: var(--text-tertiary);
  margin: 0;
}

/* TBD Team Name */
.team-name-tbd {
  color: var(--text-tertiary) !important;
  font-style: italic;
}

.stat-label-tbd {
  color: var(--text-tertiary) !important;
  font-style: italic;
}

/* Modify Bet Button */
.already-bet {
  display: flex;
  align-items: center;
  gap: 12px;
}

.already-bet .el-alert {
  flex: 1;
}

.modify-bet-btn {
  flex-shrink: 0;
  background: transparent !important;
  border: 1px solid #e6a23c !important;
  color: #e6a23c !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  transition: all var(--duration-fast) ease;
}

.modify-bet-btn:hover {
  background: rgba(230, 162, 60, 0.12) !important;
  border-color: #e6a23c !important;
  color: #e6a23c !important;
}

/* Dark Dialog */
::v-deep .dark-dialog {
  background: #12121a;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  box-shadow: 0 24px 64px rgba(0, 0, 0, 0.6);
}

::v-deep .dark-dialog .el-dialog__header {
  border-bottom: 1px solid var(--border-subtle);
  padding: 16px 20px;
}

::v-deep .dark-dialog .el-dialog__title {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 16px;
}

::v-deep .dark-dialog .el-dialog__headerbtn .el-dialog__close {
  color: var(--text-tertiary);
}

::v-deep .dark-dialog .el-dialog__body {
  padding: 20px;
}

::v-deep .dark-dialog .el-dialog__footer {
  border-top: 1px solid var(--border-subtle);
  padding: 12px 20px;
}

.modify-bet-form .bet-side-select {
  margin-bottom: 20px;
}

.modify-bet-form .bet-amount-section {
  margin-bottom: 0;
}

/* Team Card Odds */
.team-card-odds {
  font-family: var(--font-mono);
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-primary);
  margin-top: 4px;
  letter-spacing: 0.5px;
}

/* Estimated Return */
.estimated-return {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(0, 229, 160, 0.06);
  border: 1px solid rgba(0, 229, 160, 0.15);
  border-radius: var(--radius-md);
  margin-top: 12px;
}

.est-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.est-amount {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--accent-primary);
}

.est-odds {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* Comments Panel */
.comments-panel {
  background: rgba(18,18,26,0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  padding: 28px;
  margin-top: 24px;
}

.comments-panel .panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.comments-panel .panel-title i {
  color: var(--accent-primary);
  font-size: 20px;
}

.comment-count {
  color: var(--text-tertiary);
  font-size: 14px;
  font-family: var(--font-mono);
  font-weight: 400;
  margin-left: 4px;
}

.comment-input-wrap {
  margin-bottom: 24px;
}

.reply-hint {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  margin-bottom: 8px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-secondary);
}

.comment-input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.comment-input-actions .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
}

.comment-empty {
  padding: 24px 0;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  display: flex;
  gap: 12px;
}

.comment-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.comment-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.comment-avatar--sm {
  width: 28px;
  height: 28px;
}

.avatar-placeholder {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 600;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-nickname {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 13px;
}

.reply-to {
  color: var(--text-tertiary);
  font-size: 12px;
}

.comment-time {
  color: var(--text-tertiary);
  font-size: 12px;
  margin-left: auto;
}

.comment-content {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.comment-content >>> .at-mention {
  color: var(--accent-primary);
  font-weight: 500;
}

.comment-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}

.comment-actions .el-button--text {
  color: var(--text-tertiary) !important;
  font-size: 12px;
  padding: 2px 0;
}

.comment-actions .el-button--text:hover {
  color: var(--accent-primary) !important;
}

.comment-actions .delete-btn:hover {
  color: var(--accent-danger) !important;
}

.reply-list {
  margin-top: 12px;
  padding: 12px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-item {
  display: flex;
  gap: 8px;
}

.load-more-replies {
  color: var(--accent-secondary) !important;
  font-size: 12px !important;
  padding: 4px 0 !important;
}

.load-more-replies:hover {
  color: var(--accent-primary) !important;
}

.comment-pagination {
  margin-top: 20px;
  text-align: center;
}

/* ============================================================
   Responsive: Tablet & Mobile (<768px)
   - 赛事详情页移动端适配
   - 队伍展示改为竖排，面板缩减内边距，押注卡片竖排
   ============================================================ */
@media (max-width: 767px) {
  /* 顶部导航栏：允许换行，缩小间距 */
  .detail-header {
    flex-wrap: wrap;
    gap: 8px;
    margin-bottom: 16px;
  }

  /* 赛事信息卡片：缩减内边距 */
  .match-info-card {
    padding: 24px 16px;
  }

  /* 比赛时间：减小下边距 */
  .match-time {
    margin-bottom: 20px;
  }

  /* 队伍展示区：改为竖排（team-a 上，VS 中，team-b 下） */
  .teams-display {
    flex-direction: column;
    gap: 24px;
    margin-bottom: 16px;
  }

  /* 队伍 logo：72px → 56px */
  .team-logo-large {
    width: 56px;
    height: 56px;
  }

  /* 队伍 logo 占位符文字：28px → 22px */
  .team-logo-placeholder-large {
    font-size: 22px;
  }

  /* 队伍名称：20px → 16px */
  .team-name-large {
    font-size: 16px;
  }

  /* VS 文字：28px → 22px */
  .vs-text {
    font-size: 22px;
    letter-spacing: 2px;
  }

  /* 比分显示：40px → 28px */
  .score-display-large {
    font-size: 28px;
    gap: 12px;
    margin-top: 4px;
    margin-bottom: 12px;
  }

  /* 比分分隔符：32px → 22px */
  .score-large-sep {
    font-size: 22px;
  }

  /* 比分数字最小宽度缩减 */
  .score-large-a,
  .score-large-b {
    min-width: 28px;
  }

  /* 比赛结果文字：20px → 16px */
  .match-result-display {
    font-size: 16px;
    margin-top: 12px;
  }

  /* 押注面板：28px → 16px */
  .bet-panel {
    padding: 16px;
    margin-top: 16px;
  }

  /* 面板标题：22px → 18px */
  .panel-title {
    font-size: 18px;
    margin-bottom: 16px;
    padding-bottom: 10px;
  }

  /* 押注队伍卡片：横排改竖排 */
  .bet-side-cards {
    flex-direction: column;
    gap: 8px;
  }

  /* 竖排时 VS 分隔改为水平线样式 */
  .team-card-vs {
    width: auto;
    height: 20px;
    font-size: 11px;
  }

  /* 押注统计 */
  .bet-stats {
    padding-top: 16px;
  }

  /* 已押注提示：允许换行 */
  .already-bet {
    flex-wrap: wrap;
  }

  /* 预估返还：允许换行 */
  .estimated-return {
    flex-wrap: wrap;
    padding: 10px 12px;
  }

  .est-amount {
    font-size: 18px;
  }

  /* 登录/TBD 提示：缩减内边距 */
  .bet-login-prompt {
    padding: 24px 16px;
  }

  .bet-tbd-notice {
    padding: 24px 16px;
  }

  /* 评论面板：28px → 16px */
  .comments-panel {
    padding: 16px;
    margin-top: 16px;
  }

  /* 评论项：缩小间距 */
  .comment-list {
    gap: 16px;
  }

  .comment-item {
    gap: 8px;
  }

  /* 回复列表：缩减内边距 */
  .reply-list {
    padding: 8px;
    gap: 8px;
  }

  /* 修改押注弹窗中的卡片也竖排 */
  .modify-bet-form .bet-side-cards {
    flex-direction: column;
    gap: 8px;
  }
}

/* ============================================================
   Responsive: Small Mobile (<480px)
   - 进一步紧凑布局，适配小屏手机
   - 元素尺寸再缩一档，间距最小化
   ============================================================ */
@media (max-width: 479px) {
  /* 赛事信息卡片：进一步紧凑 */
  .match-info-card {
    padding: 16px 12px;
  }

  /* 比赛时间 */
  .match-time {
    font-size: 12px;
    margin-bottom: 16px;
  }

  /* 队伍展示区：间距进一步缩小 */
  .teams-display {
    gap: 16px;
    margin-bottom: 12px;
  }

  /* 队伍 logo：56px → 48px */
  .team-logo-large {
    width: 48px;
    height: 48px;
  }

  /* 队伍 logo 占位符：22px → 18px */
  .team-logo-placeholder-large {
    font-size: 18px;
  }

  /* 队伍名称：16px → 14px */
  .team-name-large {
    font-size: 14px;
  }

  /* VS 文字：22px → 18px */
  .vs-text {
    font-size: 18px;
    letter-spacing: 1px;
  }

  /* 比分显示：28px → 24px */
  .score-display-large {
    font-size: 24px;
    gap: 8px;
  }

  /* 比分分隔符 */
  .score-large-sep {
    font-size: 18px;
  }

  /* 比分数字最小宽度 */
  .score-large-a,
  .score-large-b {
    min-width: 22px;
  }

  /* 比赛结果文字 */
  .match-result-display {
    font-size: 14px;
    margin-top: 8px;
  }

  /* 押注面板 */
  .bet-panel {
    padding: 12px;
  }

  /* 面板标题 */
  .panel-title {
    font-size: 16px;
    margin-bottom: 12px;
    padding-bottom: 8px;
  }

  /* 押注队伍卡片：缩减内边距 */
  .team-card {
    padding: 14px 12px;
    min-height: 96px;
    gap: 6px;
  }

  /* 卡片内 logo 缩小 */
  .team-card-logo {
    width: 40px;
    height: 40px;
  }

  /* 卡片内队名缩小 */
  .team-card-name {
    font-size: 13px;
  }

  /* 卡片内赔率缩小 */
  .team-card-odds {
    font-size: 15px;
  }

  /* 押注金额标签 */
  .bet-amount-label {
    font-size: 12px;
  }

  /* 押注按钮 */
  .bet-btn {
    font-size: 14px;
  }

  /* 押注统计 */
  .stat-label {
    font-size: 13px;
  }

  .stat-value {
    font-size: 12px;
  }

  /* 预估返还 */
  .estimated-return {
    padding: 8px 10px;
    gap: 6px;
  }

  .est-amount {
    font-size: 16px;
  }

  .est-odds {
    font-size: 11px;
  }

  /* 登录/TBD 提示 */
  .bet-login-prompt {
    padding: 20px 12px;
  }

  .prompt-text {
    font-size: 14px;
  }

  .bet-tbd-notice {
    padding: 20px 12px;
  }

  .tbd-text {
    font-size: 13px;
  }

  /* 评论面板 */
  .comments-panel {
    padding: 12px;
  }

  /* 评论头像缩小 */
  .comment-avatar {
    width: 30px;
    height: 30px;
  }

  .comment-avatar--sm {
    width: 24px;
    height: 24px;
  }

  /* 评论内容 */
  .comment-content {
    font-size: 13px;
  }

  .comment-nickname {
    font-size: 12px;
  }

  /* 回复列表 */
  .reply-list {
    padding: 6px;
    gap: 6px;
  }

  .reply-item {
    gap: 6px;
  }

  /* 评论输入区 */
  .comment-input-wrap {
    margin-bottom: 16px;
  }
}
</style>
