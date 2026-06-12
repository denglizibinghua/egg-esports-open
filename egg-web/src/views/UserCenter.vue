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
          <!-- User Info Card -->
          <div class="user-info-card">
            <div class="user-avatar-section">
              <el-avatar :size="80" icon="el-icon-user-solid" />
              <div class="user-meta">
                <div class="user-nickname-row">
                  <span class="user-nickname">{{ user.nickname || user.username }}</span>
                  <!-- Rank Badge -->
                  <span v-if="myRank && myRank.rank === 1" class="rank-badge rank-badge--gold">
                    <span class="rank-crown">👑</span> #1
                  </span>
                  <span v-else-if="myRank && myRank.rank === 2" class="rank-badge rank-badge--silver">
                    🥈 #2
                  </span>
                  <span v-else-if="myRank && myRank.rank === 3" class="rank-badge rank-badge--bronze">
                    🥉 #3
                  </span>
                  <span v-else-if="myRank && myRank.rank > 0" class="rank-badge rank-badge--normal">
                    #{{ myRank.rank }}<span class="rank-total"> / {{ myRank.total }}</span>
                  </span>
                  <span v-else-if="myRank && myRank.rank === 0" class="rank-badge rank-badge--none">
                    暂无排名
                  </span>
                </div>
                <div class="user-join-date">加入于 {{ formatDate(user.createTime) }}</div>
              </div>
            </div>
            <div class="user-info-right">
              <div class="user-points">
                <div class="points-value">{{ user.points }}</div>
                <div class="points-label">积分</div>
              </div>
              <el-button class="edit-profile-btn" size="small" @click="openEditDialog">
                <i class="el-icon-edit"></i> 编辑资料
              </el-button>
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
            <div class="stat-card">
              <div class="stat-value">{{ user.points || 0 }}</div>
              <div class="stat-label">总积分</div>
            </div>
          </div>

          <!-- Public Profile Link -->
          <div class="public-profile-hint" v-if="user.id">
            <i class="el-icon-link"></i>
            <span>公开主页链接：</span>
            <code class="profile-url">{{ profileUrl }}</code>
            <el-button type="text" class="copy-url-btn" @click="copyProfileUrl">复制</el-button>
          </div>
        </el-tab-pane>

        <el-tab-pane label="数据仪表盘" name="dashboard">
          <div class="dashboard-grid">
            <!-- Points Trend -->
            <div class="dashboard-card dashboard-card--wide">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">积分走势</h4>
                <span class="dashboard-card-subtitle">最近 {{ recentBets.length }} 场</span>
              </div>
              <div class="points-trend" v-if="recentBets.length > 0">
                <div
                  v-for="(bet, idx) in recentBets"
                  :key="'trend-'+idx"
                  class="trend-bar"
                  :class="{
                    'trend-bar--win': bet.status === 1,
                    'trend-bar--loss': bet.status === 2,
                    'trend-bar--pending': bet.status === 0
                  }"
                  :style="{ height: trendBarHeight(bet) + 'px', animationDelay: (idx * 60) + 'ms' }"
                  :title="(bet.matchTitle || '赛事') + ' | ' + (bet.status === 1 ? '+' + bet.betAmount : bet.status === 2 ? '-' + bet.betAmount : '待结算')"
                >
                  <span class="trend-label">
                    {{ bet.status === 1 ? '+' + bet.betAmount : bet.status === 2 ? '-' + bet.betAmount : '—' }}
                  </span>
                </div>
              </div>
              <div v-else class="dashboard-empty">
                <span>暂无数据</span>
              </div>
            </div>

            <!-- Win Rate Gauge -->
            <div class="dashboard-card">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">胜率</h4>
              </div>
              <div class="win-rate-gauge">
                <div class="gauge-ring">
                  <svg viewBox="0 0 120 120" class="gauge-svg">
                    <circle cx="60" cy="60" r="52" class="gauge-bg" />
                    <circle
                      cx="60" cy="60" r="52"
                      class="gauge-fill"
                      :style="{ strokeDashoffset: gaugeOffset }"
                    />
                  </svg>
                  <div class="gauge-center">
                    <span class="gauge-value">{{ accuracy }}</span>
                    <span class="gauge-unit">%</span>
                  </div>
                </div>
                <div class="gauge-stats">
                  <div class="gauge-stat">
                    <span class="gauge-stat-val" style="color: var(--accent-primary)">{{ user.correctBets || 0 }}</span>
                    <span class="gauge-stat-lbl">猜中</span>
                  </div>
                  <div class="gauge-stat">
                    <span class="gauge-stat-val" style="color: var(--accent-danger)">{{ (user.totalBets || 0) - (user.correctBets || 0) }}</span>
                    <span class="gauge-stat-lbl">未中</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Streak Meter -->
            <div class="dashboard-card">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">连胜记录</h4>
              </div>
              <div class="streak-meter">
                <div class="streak-current">
                  <span class="streak-number">{{ user.winStreak || 0 }}</span>
                  <span class="streak-label">当前连胜</span>
                </div>
                <div class="streak-bar-track">
                  <div
                    class="streak-bar-fill"
                    :style="{ width: streakPercent + '%' }"
                  ></div>
                </div>
                <div class="streak-max">
                  <span class="streak-max-label">最高连胜</span>
                  <span class="streak-max-val">{{ user.maxWinStreak || 0 }}</span>
                </div>
              </div>
            </div>

            <!-- Bet Distribution -->
            <div class="dashboard-card">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">押注分布</h4>
              </div>
              <div class="bet-distribution" v-if="recentBets.length > 0">
                <div class="dist-row">
                  <span class="dist-label dist-label--a">A队</span>
                  <div class="dist-bar-track">
                    <div class="dist-bar dist-bar--a" :style="{ width: teamAPercent + '%' }"></div>
                  </div>
                  <span class="dist-count">{{ teamACount }}</span>
                </div>
                <div class="dist-row">
                  <span class="dist-label dist-label--b">B队</span>
                  <div class="dist-bar-track">
                    <div class="dist-bar dist-bar--b" :style="{ width: teamBPercent + '%' }"></div>
                  </div>
                  <span class="dist-count">{{ teamBCount }}</span>
                </div>
                <div class="dist-favorite" v-if="teamACount !== teamBCount">
                  最常押注：<strong>{{ teamACount >= teamBCount ? 'A队' : 'B队' }}</strong>
                </div>
              </div>
              <div v-else class="dashboard-empty">
                <span>暂无数据</span>
              </div>
            </div>

            <!-- Recent Results Chart -->
            <div class="dashboard-card dashboard-card--wide">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">战绩图</h4>
                <span class="dashboard-card-subtitle">最近 10 场</span>
              </div>
              <div class="recent-results">
                <div
                  v-for="i in 10"
                  :key="'result-'+i"
                  class="result-dot"
                  :class="resultDotClass(i - 1)"
                  :title="resultDotTitle(i - 1)"
                >
                  <span class="result-icon">{{ resultDotIcon(i - 1) }}</span>
                  <span class="result-team" v-if="recentBets[i - 1]">
                    {{ recentBets[i - 1].betSide === 0 ? 'A' : 'B' }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="投注记录" name="history">
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
                    <span class="bet-match-title">{{ scope.row.matchTitle || ('赛事 #' + scope.row.matchId) }}</span>
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
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Edit Profile Dialog -->
    <el-dialog
      title="编辑个人资料"
      :visible.sync="editDialogVisible"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="editForm" :rules="editRules" ref="editForm" label-width="80px">
        <el-form-item label="头像">
          <div class="avatar-upload">
            <el-avatar :size="64" :src="avatarPreview" icon="el-icon-user-solid" />
            <el-upload
              class="avatar-uploader"
              action="/api/user/avatar"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeAvatarUpload"
              :on-success="handleAvatarSuccess"
              :on-error="handleAvatarError"
            >
              <el-button size="small" type="text">更换头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入新昵称"
            maxlength="20"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="saveProfile">保存</el-button>
      </span>
    </el-dialog>
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
      betTotal: 0,
      activeTab: 'profile',
      myRank: null,
      recentBets: [],
      editDialogVisible: false,
      editSaving: false,
      editForm: {
        nickname: ''
      },
      avatarPreview: '',
      editRules: {
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.round((this.user.correctBets / this.user.totalBets) * 100)
    },
    profileUrl() {
      if (!this.user || !this.user.id) return ''
      return window.location.origin + '/profile/' + this.user.id
    },
    gaugeOffset() {
      const circumference = 2 * Math.PI * 52
      const offset = circumference - (this.accuracy / 100) * circumference
      return offset
    },
    streakPercent() {
      if (!this.user || !this.user.maxWinStreak || this.user.maxWinStreak === 0) return 0
      return Math.min(100, Math.round(((this.user.winStreak || 0) / this.user.maxWinStreak) * 100))
    },
    teamACount() {
      return this.recentBets.filter(b => b.betSide === 0).length
    },
    teamBCount() {
      return this.recentBets.filter(b => b.betSide === 1).length
    },
    teamAPercent() {
      if (this.recentBets.length === 0) return 50
      return Math.round((this.teamACount / this.recentBets.length) * 100)
    },
    teamBPercent() {
      if (this.recentBets.length === 0) return 50
      return Math.round((this.teamBCount / this.recentBets.length) * 100)
    },
    uploadHeaders() {
      return { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    if (!getToken()) {
      this.$router.push('/login')
      return
    }
    this.fetchUserInfo()
    this.fetchBets()
    this.fetchMyRank()
    this.fetchRecentBets()
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
    fetchMyRank() {
      request.get('/leaderboard/my-rank').then(res => {
        this.myRank = res.data || res
      }).catch(() => {
        this.myRank = { rank: 0, total: 0 }
      })
    },
    fetchRecentBets() {
      request.get('/bet/recent').then(res => {
        this.recentBets = res.data || res || []
        if (!Array.isArray(this.recentBets)) {
          this.recentBets = []
        }
      }).catch(() => {
        this.recentBets = []
      })
    },
    openEditDialog() {
      this.editForm.nickname = this.user.nickname || this.user.username || ''
      this.avatarPreview = this.user.avatar || ''
      this.editDialogVisible = true
      this.$nextTick(() => {
        if (this.$refs.editForm) {
          this.$refs.editForm.clearValidate()
        }
      })
    },
    beforeAvatarUpload(file) {
      const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
      if (!validTypes.includes(file.type)) {
        this.$message.error('仅支持 jpg/png/gif/webp 格式')
        return false
      }
      if (file.size > 2 * 1024 * 1024) {
        this.$message.error('文件大小不能超过 2MB')
        return false
      }
      return true
    },
    handleAvatarSuccess(res) {
      const data = res.data || res
      if (data.avatar) {
        this.avatarPreview = data.avatar
        this.user.avatar = data.avatar
        setUser(this.user)
        this.$message.success('头像更新成功')
      }
    },
    handleAvatarError() {
      this.$message.error('头像上传失败')
    },
    saveProfile() {
      this.$refs.editForm.validate(valid => {
        if (!valid) return
        this.editSaving = true
        request.put('/user/profile', { nickname: this.editForm.nickname }).then(() => {
          this.$message.success('资料更新成功')
          this.user.nickname = this.editForm.nickname
          setUser(this.user)
          this.editDialogVisible = false
        }).catch(() => {
          this.$message.error('更新失败，请重试')
        }).finally(() => {
          this.editSaving = false
        })
      })
    },
    copyProfileUrl() {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(this.profileUrl).then(() => {
          this.$message.success('链接已复制')
        })
      } else {
        const input = document.createElement('input')
        input.value = this.profileUrl
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('链接已复制')
      }
    },
    trendBarHeight(bet) {
      const base = 24
      const max = 80
      if (bet.status === 0) return base
      return Math.min(max, base + bet.betAmount * 2)
    },
    resultDotClass(idx) {
      if (idx >= this.recentBets.length) return 'result-dot--empty'
      const bet = this.recentBets[idx]
      if (bet.status === 1) return 'result-dot--win'
      if (bet.status === 2) return 'result-dot--loss'
      return 'result-dot--pending'
    },
    resultDotIcon(idx) {
      if (idx >= this.recentBets.length) return '·'
      const bet = this.recentBets[idx]
      if (bet.status === 1) return '✓'
      if (bet.status === 2) return '✗'
      return '⏳'
    },
    resultDotTitle(idx) {
      if (idx >= this.recentBets.length) return '暂无数据'
      const bet = this.recentBets[idx]
      const team = bet.betSide === 0 ? 'A队' : 'B队'
      const status = bet.status === 1 ? '猜中' : bet.status === 2 ? '未中' : '待结算'
      return (bet.matchTitle || '赛事') + ' | ' + team + ' | ' + bet.betAmount + '分 | ' + status
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

/* Tabs */
.user-tabs {
  margin-top: 4px;
}

/* User Info Card */
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

.user-nickname-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
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

.user-info-right {
  display: flex;
  align-items: center;
  gap: 20px;
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

/* Rank Badge */
.rank-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  border-radius: var(--radius-full);
  font-family: var(--font-mono);
  letter-spacing: 0.04em;
}

.rank-badge--gold {
  color: #1a1200;
  background: linear-gradient(135deg, #ffd700, #ffb300);
  box-shadow: 0 0 12px rgba(255, 215, 0, 0.4);
}

.rank-badge--silver {
  color: #1a1a1a;
  background: linear-gradient(135deg, #e0e0e0, #b0b0b0);
  box-shadow: 0 0 8px rgba(192, 192, 192, 0.3);
}

.rank-badge--bronze {
  color: #1a0e00;
  background: linear-gradient(135deg, #cd7f32, #a0522d);
  box-shadow: 0 0 8px rgba(205, 127, 50, 0.3);
}

.rank-badge--normal {
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
}

.rank-badge--none {
  color: var(--text-tertiary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  font-weight: 500;
  font-family: var(--font-body);
}

.rank-crown {
  font-size: 14px;
}

.rank-total {
  color: var(--text-tertiary);
  font-weight: 400;
}

/* Edit Profile Button */
.edit-profile-btn {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-md) !important;
  font-weight: 500;
  transition: all var(--duration-fast) ease;
}

.edit-profile-btn:hover {
  border-color: var(--accent-primary) !important;
  color: var(--accent-primary) !important;
  background: rgba(0, 229, 160, 0.06) !important;
}

/* Stats Grid */
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

/* Public Profile Hint */
.public-profile-hint {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.public-profile-hint i {
  color: var(--accent-secondary);
  font-size: 16px;
}

.profile-url {
  color: var(--accent-secondary);
  background: var(--bg-surface-2);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 12px;
  word-break: break-all;
}

.copy-url-btn {
  color: var(--accent-primary) !important;
  font-size: 12px !important;
  padding: 2px 8px !important;
  flex-shrink: 0;
}

/* Dashboard Grid */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.dashboard-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px;
  box-shadow: var(--shadow-md);
}

.dashboard-card--wide {
  grid-column: 1 / -1;
}

.dashboard-card-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 20px;
}

.dashboard-card-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 18px;
  letter-spacing: 0.5px;
}

.dashboard-card-subtitle {
  color: var(--text-tertiary);
  font-size: 12px;
}

.dashboard-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 80px;
  color: var(--text-tertiary);
  font-size: 13px;
}

/* Points Trend */
.points-trend {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  height: 100px;
  padding: 0 4px;
}

.trend-bar {
  flex: 1;
  min-width: 20px;
  border-radius: var(--radius-sm) var(--radius-sm) 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  transition: transform var(--duration-fast) ease;
  cursor: default;
  animation: trend-grow 0.5s var(--ease-out) both;
}

@keyframes trend-grow {
  from {
    transform: scaleY(0);
    transform-origin: bottom;
  }
  to {
    transform: scaleY(1);
    transform-origin: bottom;
  }
}

.trend-bar:hover {
  transform: scaleY(1.05);
  transform-origin: bottom;
}

.trend-bar--win {
  background: linear-gradient(to top, rgba(0, 229, 160, 0.3), rgba(0, 229, 160, 0.7));
}

.trend-bar--loss {
  background: linear-gradient(to top, rgba(255, 77, 109, 0.3), rgba(255, 77, 109, 0.7));
}

.trend-bar--pending {
  background: linear-gradient(to top, rgba(255, 255, 255, 0.06), rgba(255, 255, 255, 0.12));
}

.trend-label {
  font-size: 10px;
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
}

/* Win Rate Gauge */
.win-rate-gauge {
  display: flex;
  align-items: center;
  gap: 24px;
}

.gauge-ring {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.gauge-svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.gauge-bg {
  fill: none;
  stroke: var(--bg-surface-3);
  stroke-width: 8;
}

.gauge-fill {
  fill: none;
  stroke: var(--accent-primary);
  stroke-width: 8;
  stroke-linecap: round;
  stroke-dasharray: 326.73;
  transition: stroke-dashoffset 1s var(--ease-out);
}

.gauge-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.gauge-value {
  font-family: var(--font-mono);
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  font-variant-numeric: tabular-nums;
}

.gauge-unit {
  font-size: 14px;
  color: var(--text-tertiary);
  font-weight: 600;
  align-self: flex-start;
  margin-top: 8px;
}

.gauge-stats {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.gauge-stat {
  display: flex;
  flex-direction: column;
}

.gauge-stat-val {
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.gauge-stat-lbl {
  font-size: 11px;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* Streak Meter */
.streak-meter {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.streak-current {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.streak-number {
  font-family: var(--font-mono);
  font-size: 36px;
  font-weight: 800;
  color: var(--accent-primary);
  font-variant-numeric: tabular-nums;
}

.streak-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

.streak-bar-track {
  height: 8px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.streak-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent-primary), var(--accent-secondary));
  border-radius: var(--radius-full);
  transition: width 0.8s var(--ease-out);
}

.streak-max {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.streak-max-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

.streak-max-val {
  font-family: var(--font-mono);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-secondary);
}

/* Bet Distribution */
.bet-distribution {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.dist-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dist-label {
  width: 32px;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  flex-shrink: 0;
}

.dist-label--a {
  color: var(--team-a);
}

.dist-label--b {
  color: var(--team-b);
}

.dist-bar-track {
  flex: 1;
  height: 10px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.dist-bar {
  height: 100%;
  border-radius: var(--radius-full);
  transition: width 0.6s var(--ease-out);
}

.dist-bar--a {
  background: var(--team-a);
}

.dist-bar--b {
  background: var(--team-b);
}

.dist-count {
  width: 24px;
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  text-align: right;
  flex-shrink: 0;
}

.dist-favorite {
  font-size: 12px;
  color: var(--text-tertiary);
  text-align: center;
  padding-top: 4px;
}

.dist-favorite strong {
  color: var(--text-secondary);
}

/* Recent Results */
.recent-results {
  display: flex;
  gap: 8px;
  justify-content: center;
  flex-wrap: wrap;
}

.result-dot {
  width: 48px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  cursor: default;
  transition: transform var(--duration-fast) ease, box-shadow var(--duration-fast) ease;
  animation: dot-pop 0.3s var(--ease-out) both;
}

.result-dot:nth-child(1) { animation-delay: 0ms; }
.result-dot:nth-child(2) { animation-delay: 50ms; }
.result-dot:nth-child(3) { animation-delay: 100ms; }
.result-dot:nth-child(4) { animation-delay: 150ms; }
.result-dot:nth-child(5) { animation-delay: 200ms; }
.result-dot:nth-child(6) { animation-delay: 250ms; }
.result-dot:nth-child(7) { animation-delay: 300ms; }
.result-dot:nth-child(8) { animation-delay: 350ms; }
.result-dot:nth-child(9) { animation-delay: 400ms; }
.result-dot:nth-child(10) { animation-delay: 450ms; }

@keyframes dot-pop {
  from {
    opacity: 0;
    transform: scale(0.7);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.result-dot:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.result-dot--win {
  background: rgba(0, 229, 160, 0.12);
  border: 1px solid rgba(0, 229, 160, 0.3);
}

.result-dot--loss {
  background: rgba(255, 77, 109, 0.12);
  border: 1px solid rgba(255, 77, 109, 0.3);
}

.result-dot--pending {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid var(--border-default);
}

.result-dot--empty {
  background: var(--bg-surface-2);
  border: 1px dashed var(--border-subtle);
}

.result-icon {
  font-size: 16px;
  font-weight: 700;
}

.result-dot--win .result-icon {
  color: var(--accent-primary);
}

.result-dot--loss .result-icon {
  color: var(--accent-danger);
}

.result-dot--pending .result-icon {
  color: var(--text-tertiary);
  font-size: 12px;
}

.result-dot--empty .result-icon {
  color: var(--text-tertiary);
  opacity: 0.4;
}

.result-team {
  font-size: 10px;
  font-weight: 600;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* Bet History */
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

.bet-match-title {
  color: var(--text-primary);
  font-weight: 500;
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

/* Dialog Footer */
.dialog-footer .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
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

/* Responsive */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(3, 1fr);
  }
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  .dashboard-card--wide {
    grid-column: 1;
  }
  .user-info-card {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
  .user-avatar-section {
    flex-direction: column;
  }
  .user-info-right {
    flex-direction: column;
    gap: 12px;
  }
  .win-rate-gauge {
    flex-direction: column;
  }
  .recent-results {
    gap: 6px;
  }
  .result-dot {
    width: 40px;
    height: 48px;
  }
}

.avatar-upload {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar-uploader {
  display: inline-block;
}
</style>
