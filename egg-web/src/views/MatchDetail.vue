<template>
  <div class="match-detail" v-loading="loading">
    <div v-if="match" class="detail-content">
      <div class="detail-header">
        <el-button type="text" icon="el-icon-arrow-left" class="back-btn" @click="$router.back()">返回</el-button>
        <el-tag v-if="match.status === 0" type="warning">即将开始</el-tag>
        <el-tag v-else-if="match.status === 1" type="danger">
          <span class="live-dot"></span> LIVE
        </el-tag>
        <el-tag v-else-if="match.status === 2" type="success">已结束</el-tag>
      </div>

      <div class="match-info-card">
        <div class="tournament-name">{{ match.tournament || '常规赛事' }}</div>
        <div class="match-time">{{ formatTime(match.matchTime) }}</div>

        <div class="teams-display">
          <div class="team-display team-a-display">
            <div class="team-logo-large">
              <img v-if="match.teamALogo" :src="match.teamALogo" alt="">
              <span v-else class="team-logo-placeholder-large">{{ (match.teamA || '').charAt(0) }}</span>
            </div>
            <div class="team-name-large">{{ match.teamA }}</div>
          </div>

          <div class="vs-section">
            <div class="vs-text">VS</div>
          </div>

          <div class="team-display team-b-display">
            <div class="team-logo-large">
              <img v-if="match.teamBLogo" :src="match.teamBLogo" alt="">
              <span v-else class="team-logo-placeholder-large">{{ (match.teamB || '').charAt(0) }}</span>
            </div>
            <div class="team-name-large">{{ match.teamB }}</div>
          </div>
        </div>

        <div v-if="match.status === 2 && match.result !== null && match.result !== undefined" class="match-result-display">
          <template v-if="match.result === 0">
            <span class="winner-name">{{ match.teamA }}</span> 获胜!
          </template>
          <template v-else-if="match.result === 1">
            <span class="winner-name">{{ match.teamB }}</span> 获胜!
          </template>
          <template v-else-if="match.result === 2">
            平局
          </template>
        </div>
      </div>

      <!-- Betting Panel -->
      <div v-if="match.status === 0 || match.status === 1" class="bet-panel">
        <h3 class="panel-title">押注</h3>

        <div v-if="myBet" class="already-bet">
          <el-alert
            :title="'你已押注 ' + (myBet.betSide === 0 ? match.teamA : match.teamB) + ' ' + myBet.betAmount + ' 积分'"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>

        <div v-else class="bet-form">
          <div class="bet-side-select">
            <div class="bet-side-label">选择押注队伍</div>
            <el-radio-group v-model="betForm.betSide" class="side-radio-group">
              <el-radio-button :label="0">{{ match.teamA }}</el-radio-button>
              <el-radio-button :label="1">{{ match.teamB }}</el-radio-button>
            </el-radio-group>
          </div>

          <div class="bet-amount-section">
            <div class="bet-amount-label">
              押注积分
              <span class="current-points">当前积分: {{ userPoints }}</span>
            </div>
            <el-slider
              v-model="betForm.betAmount"
              :min="10"
              :max="Math.max(10, userPoints)"
              :step="10"
              show-input
              :show-input-controls="false"
            />
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

        <div class="bet-stats">
          <div class="bet-stat">
            <span class="stat-label">{{ match.teamA }}</span>
            <span class="stat-value">{{ betCount.teamA }} 人押注</span>
          </div>
          <div class="bet-stat">
            <span class="stat-label">{{ match.teamB }}</span>
            <span class="stat-value">{{ betCount.teamB }} 人押注</span>
          </div>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && !match" description="赛事不存在" />
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken } from '@/utils/auth'

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
        betAmount: 10
      },
      betLoading: false,
      userPoints: 0
    }
  },
  created() {
    this.fetchMatch()
    this.fetchBetCount()
    if (getToken()) {
      this.fetchMyBet()
      this.fetchUserPoints()
    }
  },
  methods: {
    fetchMatch() {
      this.loading = true
      request.get('/match/' + this.$route.params.id).then(res => {
        this.match = res.data || res
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    fetchBetCount() {
      request.get('/bet/count/' + this.$route.params.id).then(res => {
        const data = res.data || res
        this.betCount = {
          teamA: data.teamA || data.teamACount || data.sideA || 0,
          teamB: data.teamB || data.teamBCount || data.sideB || 0
        }
      }).catch(() => {})
    },
    fetchMyBet() {
      request.get('/bet/my').then(res => {
        const data = res.data || res
        const bets = Array.isArray(data) ? data : (data.records || data.list || [])
        const matchId = parseInt(this.$route.params.id)
        const found = bets.find(b => b.matchId === matchId)
        if (found) {
          this.myBet = found
        }
      }).catch(() => {})
    },
    fetchUserPoints() {
      request.get('/user/me').then(res => {
        const user = res.data || res
        this.userPoints = user.points || 0
      }).catch(() => {})
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
      }).then(() => {
        this.$message.success('押注成功!')
        this.fetchMyBet()
        this.fetchBetCount()
        this.fetchUserPoints()
      }).catch(() => {}).finally(() => {
        this.betLoading = false
      })
    },
    formatTime(timeStr) {
      if (!timeStr) return ''
      const d = new Date(timeStr)
      const y = d.getFullYear()
      const month = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const hour = d.getHours().toString().padStart(2, '0')
      const min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + month + '-' + day + ' ' + hour + ':' + min
    }
  }
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.back-btn {
  color: #a0a0b0 !important;
  font-size: 14px;
}

.back-btn:hover {
  color: #FF4655 !important;
}

.match-info-card {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 32px;
  text-align: center;
}

.tournament-name {
  color: #a0a0b0;
  font-size: 14px;
  margin-bottom: 4px;
}

.match-time {
  color: #a0a0b0;
  font-size: 13px;
  margin-bottom: 24px;
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
  border-radius: 50%;
  overflow: hidden;
  background-color: #2a2a4a;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #3a3a5a;
}

.team-logo-large img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.team-logo-placeholder-large {
  font-size: 28px;
  font-weight: 700;
  color: #FF4655;
}

.team-name-large {
  font-size: 22px;
  font-weight: 700;
  color: #e0e0e0;
}

.vs-section {
  flex-shrink: 0;
}

.vs-text {
  font-size: 24px;
  font-weight: 800;
  color: #FF4655;
  letter-spacing: 3px;
}

.match-result-display {
  font-size: 20px;
  font-weight: 600;
  color: #67C23A;
  margin-top: 16px;
}

.winner-name {
  color: #FF4655;
}

.bet-panel {
  background-color: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  padding: 24px;
  margin-top: 24px;
}

.panel-title {
  color: #e0e0e0;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 20px;
  border-bottom: 1px solid #2a2a4a;
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
  color: #a0a0b0;
  font-size: 14px;
  margin-bottom: 8px;
}

.side-radio-group {
  width: 100%;
}

.side-radio-group .el-radio-button__inner {
  background-color: #1a1a2e !important;
  border-color: #2a2a4a !important;
  color: #e0e0e0 !important;
  width: 50%;
}

.side-radio-group .el-radio-button__orig-radio:checked + .el-radio-button__inner {
  background-color: #FF4655 !important;
  border-color: #FF4655 !important;
  color: #fff !important;
}

.bet-amount-section {
  margin-bottom: 24px;
}

.bet-amount-label {
  color: #a0a0b0;
  font-size: 14px;
  margin-bottom: 12px;
}

.current-points {
  color: #FF4655;
  font-weight: 600;
  margin-left: 8px;
}

.bet-btn {
  width: 100%;
  background-color: #FF4655;
  border-color: #FF4655;
  font-size: 16px;
  font-weight: 600;
}

.bet-btn:hover {
  background-color: #e63e4d;
  border-color: #e63e4d;
}

.bet-stats {
  display: flex;
  justify-content: space-around;
  padding-top: 16px;
  border-top: 1px solid #2a2a4a;
}

.bet-stat {
  text-align: center;
}

.stat-label {
  display: block;
  color: #e0e0e0;
  font-weight: 600;
  font-size: 15px;
  margin-bottom: 4px;
}

.stat-value {
  color: #a0a0b0;
  font-size: 13px;
}

.live-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #FF4655;
  margin-right: 4px;
  animation: dotBlink 1s ease-in-out infinite;
}

@keyframes dotBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>