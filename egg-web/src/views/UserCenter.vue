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
                <div class="points-sub" v-if="loanStatus && loanStatus.rankingScore !== undefined">
                  排名分 {{ loanStatus.rankingScore }}
                </div>
              </div>
              <el-button class="edit-profile-btn" size="small" @click="openEditDialog">
                <i class="el-icon-edit"></i> 编辑资料
              </el-button>
              <el-button class="edit-profile-btn" size="small" @click="passwordDialogVisible = true" style="margin-left: 8px;">
                <i class="el-icon-lock"></i> 修改密码
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

          <!-- Bank Quick Link -->
          <div class="bank-link-card" v-if="loanStatus">
            <router-link to="/bank" class="bank-link">
              <div class="bank-link-left">
                <i class="el-icon-bank-card"></i>
                <span>银行中心</span>
              </div>
              <div class="bank-link-right">
                <span class="bank-link-debt" v-if="loanStatus.loanAmount < 0">
                  贷款 {{ formatLoanAmount(loanStatus.loanAmount) }}
                </span>
                <span class="bank-link-clean" v-else>无贷款</span>
                <i class="el-icon-arrow-right"></i>
              </div>
            </router-link>
          </div>
        </el-tab-pane>

        <el-tab-pane label="数据仪表盘" name="dashboard">
          <div class="dashboard-grid">
            <!-- Points Trend -->
            <div class="dashboard-card dashboard-card--wide">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">最近投注</h4>
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

            <!-- Profit Curve -->
            <div class="dashboard-card dashboard-card--wide">
              <div class="dashboard-card-header">
                <h4 class="dashboard-card-title">收益曲线</h4>
                <span class="dashboard-card-subtitle">累计积分变化 · {{ profitData.length }} 个数据点</span>
              </div>
              <div class="chart-container" v-if="profitData && profitData.length > 0" v-loading="profitLoading">
                <svg viewBox="0 0 800 220" class="profit-chart" preserveAspectRatio="xMidYMid meet">
                  <defs>
                    <linearGradient id="profitGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stop-color="var(--accent-primary)" stop-opacity="0.3" />
                      <stop offset="100%" stop-color="var(--accent-primary)" stop-opacity="0.02" />
                    </linearGradient>
                  </defs>
                  <!-- Grid lines -->
                  <line x1="50" y1="15" x2="50" y2="195" class="chart-axis-line" />
                  <line x1="50" y1="195" x2="790" y2="195" class="chart-axis-line" />
                  <line x1="50" y1="105" x2="790" y2="105" class="chart-grid-line" />
                  <line x1="50" y1="60" x2="790" y2="60" class="chart-grid-line" />
                  <line x1="50" y1="150" x2="790" y2="150" class="chart-grid-line" />
                  <!-- Fill area -->
                  <polygon :points="profitChartFill" fill="url(#profitGradient)" class="chart-fill" />
                  <!-- Line -->
                  <polyline :points="profitChartPoints" fill="none" class="chart-line" />
                  <!-- Y axis labels -->
                  <text x="45" y="198" class="chart-label chart-label--y" text-anchor="end">{{ profitChartMeta ? profitChartMeta.min : '' }}</text>
                  <text x="45" y="18" class="chart-label chart-label--y" text-anchor="end">{{ profitChartMeta ? profitChartMeta.max : '' }}</text>
                  <!-- X axis labels -->
                  <text x="50" y="213" class="chart-label chart-label--x" text-anchor="start">{{ profitChartMeta ? profitChartMeta.first : '' }}</text>
                  <text x="790" y="213" class="chart-label chart-label--x" text-anchor="end">{{ profitChartMeta ? profitChartMeta.last : '' }}</text>
                  <!-- Data dots (interactive) -->
                  <g v-for="(dot, i) in profitDotCoords" :key="'dot-'+i">
                    <circle
                      :cx="dot.x"
                      :cy="dot.y"
                      r="12"
                      fill="transparent"
                      class="chart-hit-area"
                      @mouseenter="chartTooltip = { x: dot.x, y: dot.y, data: profitData[i] }"
                      @mouseleave="chartTooltip = null"
                    />
                    <circle
                      :cx="dot.x"
                      :cy="dot.y"
                      r="3.5"
                      class="chart-dot"
                    />
                  </g>
                  <!-- Tooltip -->
                  <g v-if="chartTooltip" class="chart-tooltip-group">
                    <line
                      :x1="chartTooltip.x"
                      y1="15"
                      :x2="chartTooltip.x"
                      y2="195"
                      class="chart-tooltip-line"
                    />
                    <circle
                      :cx="chartTooltip.x"
                      :cy="chartTooltip.y"
                      r="5"
                      class="chart-tooltip-dot"
                    />
                    <rect
                      :x="chartTooltip.x > 600 ? chartTooltip.x - 130 : chartTooltip.x + 10"
                      :y="chartTooltip.y > 150 ? chartTooltip.y - 48 : chartTooltip.y - 8"
                      width="120"
                      height="40"
                      rx="6"
                      class="chart-tooltip-bg"
                    />
                    <text
                      :x="chartTooltip.x > 600 ? chartTooltip.x - 70 : chartTooltip.x + 70"
                      :y="chartTooltip.y > 150 ? chartTooltip.y - 30 : chartTooltip.y + 10"
                      class="chart-tooltip-text chart-tooltip-text--val"
                      text-anchor="middle"
                    >{{ chartTooltip.data.points }} 分</text>
                    <text
                      :x="chartTooltip.x > 600 ? chartTooltip.x - 70 : chartTooltip.x + 70"
                      :y="chartTooltip.y > 150 ? chartTooltip.y - 14 : chartTooltip.y + 26"
                      class="chart-tooltip-text chart-tooltip-text--time"
                      text-anchor="middle"
                    >{{ chartTooltip.data.time }}</text>
                  </g>
                </svg>
              </div>
              <div class="empty-state chart-empty" v-else>
                <p>暂无结算数据</p>
                <span>结算后积分变化将在这里展示</span>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane name="history">
          <span slot="label">
            投注记录
            <span v-if="settlementCount > 0" class="settlement-badge">{{ settlementCount }}</span>
          </span>
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
              <svg class="empty-svg" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
                <rect x="18" y="10" width="44" height="56" rx="6" stroke="#6b6b7c" stroke-width="2" fill="rgba(107,107,124,0.06)"/>
                <rect x="30" y="6" width="20" height="10" rx="3" stroke="#5b8cff" stroke-width="1.5" fill="rgba(91,140,255,0.08)"/>
                <line x1="28" y1="28" x2="52" y2="28" stroke="#6b6b7c" stroke-width="1.5" stroke-linecap="round" opacity="0.3"/>
                <line x1="28" y1="36" x2="48" y2="36" stroke="#6b6b7c" stroke-width="1.5" stroke-linecap="round" opacity="0.25"/>
                <line x1="28" y1="44" x2="44" y2="44" stroke="#6b6b7c" stroke-width="1.5" stroke-linecap="round" opacity="0.2"/>
                <line x1="28" y1="52" x2="40" y2="52" stroke="#6b6b7c" stroke-width="1.5" stroke-linecap="round" opacity="0.15"/>
                <circle cx="56" cy="56" r="12" stroke="#00e5a0" stroke-width="1.5" fill="rgba(0,229,160,0.06)"/>
                <line x1="52" y1="56" x2="60" y2="56" stroke="#00e5a0" stroke-width="1.5" stroke-linecap="round"/>
                <line x1="56" y1="52" x2="56" y2="60" stroke="#00e5a0" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
              <h3 class="empty-title">还没有押注记录</h3>
              <p class="empty-desc">去赛事列表看看，选一场比赛试试你的判断力</p>
              <el-button type="primary" class="empty-cta" @click="$router.push('/matches')">去赛事列表</el-button>
            </div>

            <template v-else>
              <div class="table-scroll">
              <el-table :data="bets" style="width: 100%" v-loading="betsLoading" :row-class-name="betRowClass">
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
                <el-table-column label="返还" width="80" align="center">
                  <template slot-scope="scope">
                    <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
                    <span v-else class="returned-amount">{{ getReturnedAmount(scope.row) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="净收益" width="100" align="center">
                  <template slot-scope="scope">
                    <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
                    <span v-else :class="netChangeClass(scope.row)">{{ formatNetChange(scope.row) }}</span>
                  </template>
                </el-table-column>
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
              </div>

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

    <!-- Password Change Dialog -->
    <el-dialog title="修改密码" :visible.sync="passwordDialogVisible" width="420px" :close-on-click-modal="false">
      <el-form ref="passwordForm" :model="passwordForm" :rules="passwordRules" label-width="80px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="6-30个字符" show-password />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="passwordDialogVisible = false" :disabled="passwordSaving">取消</el-button>
        <el-button type="primary" @click="submitPassword" :loading="passwordSaving">确认修改</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { getToken, setUser } from '@/utils/auth'

var LAST_VISIT_KEY = 'egg_lastVisitTime'

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
      },
      settlementCount: 0,
      settledBetKeys: [],
      profitData: [],
      profitLoading: false,
      chartTooltip: null,
      passwordDialogVisible: false,
      passwordSaving: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [
          { required: true, message: '请输入原密码', trigger: 'blur' }
        ],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 30, message: '密码长度需在6-30个字符之间', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请再次输入新密码', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (value !== this.passwordForm.newPassword) {
                callback(new Error('两次输入的密码不一致'))
              } else {
                callback()
              }
            },
            trigger: 'blur'
          }
        ]
      },
      loanStatus: null,
      claimingStipend: false,
      loanConfig: {
        dailyStipend: 100
      }
    }
  },
  computed: {
    accuracy() {
      if (!this.user || !this.user.totalBets || this.user.totalBets === 0) return 0
      return Math.min(100, Math.round((this.user.correctBets / this.user.totalBets) * 100))
    },
    profileUrl() {
      if (!this.user || !this.user.id) return ''
      return window.location.origin + '/profile/' + this.user.id
    },
    gaugeOffset() {
      var circumference = 2 * Math.PI * 52
      var offset = circumference - (this.accuracy / 100) * circumference
      return offset
    },
    streakPercent() {
      if (!this.user || !this.user.maxWinStreak || this.user.maxWinStreak === 0) return 0
      return Math.min(100, Math.round(((this.user.winStreak || 0) / this.user.maxWinStreak) * 100))
    },
    teamACount() {
      return this.recentBets.filter(function (b) { return b.betSide === 0 }).length
    },
    teamBCount() {
      return this.recentBets.filter(function (b) { return b.betSide === 1 }).length
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
    },
    profitChartPoints() {
      var data = this.profitData
      if (!data || data.length === 0) return ''
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var pts = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        pts.push(x.toFixed(1) + ',' + y.toFixed(1))
      }
      return pts.join(' ')
    },
    profitChartFill() {
      var data = this.profitData
      if (!data || data.length === 0) return ''
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var pts = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        pts.push(x.toFixed(1) + ',' + y.toFixed(1))
      }
      var bottomY = padT + h
      var firstX = n === 1 ? padL + w / 2 : padL
      var lastX = n === 1 ? padL + w / 2 : padL + (n - 1) * (w / (n - 1))
      pts.push(lastX.toFixed(1) + ',' + bottomY)
      pts.push(firstX.toFixed(1) + ',' + bottomY)
      return pts.join(' ')
    },
    profitChartMeta() {
      var data = this.profitData
      if (!data || data.length === 0) return null
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      return {
        min: minP,
        max: maxP,
        first: data[0].time,
        last: data[data.length - 1].time,
        count: data.length
      }
    },
    profitDotCoords() {
      var data = this.profitData
      if (!data || data.length === 0) return []
      var n = data.length
      var padL = 50, padR = 10, padT = 15, padB = 25
      var w = 800 - padL - padR
      var h = 220 - padT - padB
      var vals = data.map(function (d) { return d.points })
      var minP = Math.min.apply(null, vals)
      var maxP = Math.max.apply(null, vals)
      var range = maxP - minP || 1
      var pad = range * 0.1
      minP = minP - pad
      maxP = maxP + pad
      range = maxP - minP
      var dots = []
      for (var i = 0; i < n; i++) {
        var x = n === 1 ? padL + w / 2 : padL + i * (w / (n - 1))
        var y = padT + h - ((data[i].points - minP) / range) * h
        dots.push({ x: x, y: y, idx: i })
      }
      return dots
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
    this.fetchSettledSince()
    this.fetchProfitData()
    this.fetchLoanStatus()
  },
  methods: {
    fetchUserInfo() {
      this.loading = true
      request.get('/user/me').then(function (res) {
        this.user = res.data || res
        setUser(this.user)
      }.bind(this)).catch(function () {}).finally(function () {
        this.loading = false
      }.bind(this))
    },
    fetchBets() {
      this.betsLoading = true
      var params = {
        page: this.betCurrentPage,
        size: this.betPageSize
      }
      if (this.betStatus !== 'all') {
        params.status = this.betStatus
      }
      request.get('/bet/my', { params }).then(function (res) {
        var data = res.data || res
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
      }.bind(this)).catch(function () {}).finally(function () {
        this.betsLoading = false
      }.bind(this))
    },
    fetchMyRank() {
      request.get('/leaderboard/my-rank').then(function (res) {
        this.myRank = res.data || res
      }.bind(this)).catch(function () {
        this.myRank = { rank: 0, total: 0 }
      }.bind(this))
    },
    fetchRecentBets() {
      request.get('/bet/recent').then(function (res) {
        this.recentBets = res.data || res || []
        if (!Array.isArray(this.recentBets)) {
          this.recentBets = []
        }
      }.bind(this)).catch(function () {
        this.recentBets = []
      }.bind(this))
    },
    fetchSettledSince() {
      var lastVisit = localStorage.getItem(LAST_VISIT_KEY)
      var now = new Date().toISOString()
      if (lastVisit) {
        request.get('/bet/settled-since', { params: { since: lastVisit } }).then(function (res) {
          var data = res.data || res
          var list = Array.isArray(data) ? data : (data.records || data.list || [])
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
    fetchProfitData() {
      this.profitLoading = true
      request.get('/bet/settled-since').then(function (res) {
        var data = res.data || res
        var list = Array.isArray(data) ? data : (data.records || data.list || [])
        // Sort by createTime ascending
        list.sort(function (a, b) {
          return new Date(a.createTime) - new Date(b.createTime)
        })
        // Build cumulative points starting from 1000
        var cumulative = 1000
        var result = []
        for (var i = 0; i < list.length; i++) {
          var net = list[i].netChange
          if (net === null || net === undefined) {
            if (list[i].status === 1) net = list[i].betAmount
            else if (list[i].status === 2) net = -list[i].betAmount
            else net = 0
          }
          cumulative += net
          var d = new Date(list[i].createTime)
          var m = d.getMonth() + 1
          var day = d.getDate()
          var h = d.getHours().toString().padStart(2, '0')
          var min = d.getMinutes().toString().padStart(2, '0')
          result.push({
            time: m + '/' + day + ' ' + h + ':' + min,
            points: cumulative
          })
        }
        this.profitData = result
      }.bind(this)).catch(function () {
        this.profitData = []
      }.bind(this)).finally(function () {
        this.profitLoading = false
      }.bind(this))
    },
    betRowClass: function (_ref) {
      var row = _ref.row
      var key = row.matchId + '_' + row.betSide
      if (this.settledBetKeys.indexOf(key) !== -1 && row.status !== 0) {
        return 'new-settlement'
      }
      return ''
    },
    getReturnedAmount(row) {
      if (row.returnedAmount !== null && row.returnedAmount !== undefined) {
        return row.returnedAmount
      }
      // Fallback computation
      if (row.status === 1) return row.betAmount * 2
      if (row.status === 2) return 0
      // draw (result === 2 scenario) - not directly available in bet status
      return row.betAmount
    },
    netChangeClass(row) {
      var net = this.getNetChange(row)
      if (net > 0) return 'net-change--positive'
      if (net < 0) return 'net-change--negative'
      return 'net-change--neutral'
    },
    getNetChange(row) {
      if (row.netChange !== null && row.netChange !== undefined) {
        return row.netChange
      }
      // Fallback computation
      if (row.status === 1) return row.betAmount
      if (row.status === 2) return -row.betAmount
      return 0
    },
    formatNetChange(row) {
      var net = this.getNetChange(row)
      if (net > 0) return '+' + net
      if (net < 0) return '' + net
      return '0'
    },
    openEditDialog() {
      this.editForm.nickname = this.user.nickname || this.user.username || ''
      this.avatarPreview = this.user.avatar || ''
      this.editDialogVisible = true
      this.$nextTick(function () {
        if (this.$refs.editForm) {
          this.$refs.editForm.clearValidate()
        }
      }.bind(this))
    },
    beforeAvatarUpload(file) {
      var validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
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
      var data = res.data || res
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
      this.$refs.editForm.validate(function (valid) {
        if (!valid) return
        this.editSaving = true
        request.put('/user/profile', { nickname: this.editForm.nickname }).then(function () {
          this.$message.success('资料更新成功')
          this.user.nickname = this.editForm.nickname
          setUser(this.user)
          this.editDialogVisible = false
        }.bind(this)).catch(function () {
          this.$message.error('更新失败，请重试')
        }.bind(this)).finally(function () {
          this.editSaving = false
        }.bind(this))
      }.bind(this))
    },
    submitPassword() {
      this.$refs.passwordForm.validate(valid => {
        if (!valid) return
        this.passwordSaving = true
        request.put('/user/password', {
          oldPassword: this.passwordForm.oldPassword,
          newPassword: this.passwordForm.newPassword
        }).then(() => {
          this.$message.success('密码修改成功')
          this.passwordDialogVisible = false
          this.passwordForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
          this.$refs.passwordForm.resetFields()
        }).catch(() => {})
        .finally(() => { this.passwordSaving = false })
      })
    },
    copyProfileUrl() {
      if (navigator.clipboard) {
        navigator.clipboard.writeText(this.profileUrl).then(function () {
          this.$message.success('链接已复制')
        }.bind(this))
      } else {
        var input = document.createElement('input')
        input.value = this.profileUrl
        document.body.appendChild(input)
        input.select()
        document.execCommand('copy')
        document.body.removeChild(input)
        this.$message.success('链接已复制')
      }
    },
    trendBarHeight(bet) {
      var base = 24
      var max = 80
      if (bet.status === 0) return base
      return Math.min(max, base + bet.betAmount * 2)
    },
    resultDotClass(idx) {
      if (idx >= this.recentBets.length) return 'result-dot--empty'
      var bet = this.recentBets[idx]
      if (bet.status === 1) return 'result-dot--win'
      if (bet.status === 2) return 'result-dot--loss'
      return 'result-dot--pending'
    },
    resultDotIcon(idx) {
      if (idx >= this.recentBets.length) return '·'
      var bet = this.recentBets[idx]
      if (bet.status === 1) return '✓'
      if (bet.status === 2) return '✗'
      return '⏳'
    },
    resultDotTitle(idx) {
      if (idx >= this.recentBets.length) return '暂无数据'
      var bet = this.recentBets[idx]
      var team = bet.betSide === 0 ? 'A队' : 'B队'
      var status = bet.status === 1 ? '猜中' : bet.status === 2 ? '未中' : '待结算'
      return (bet.matchTitle || '赛事') + ' | ' + team + ' | ' + bet.betAmount + '分 | ' + status
    },
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
    formatLoanAmount(amount) {
      if (amount === null || amount === undefined) return '0'
      if (amount >= 0) return '0'
      return amount.toString()
    },
    formatDate(timeStr) {
      if (!timeStr) return '-'
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
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

/* Settlement Notification Badge */
.settlement-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: var(--accent-danger);
  border-radius: var(--radius-full);
  margin-left: 6px;
  line-height: 1;
  vertical-align: middle;
  animation: badge-pulse 2s ease-in-out infinite;
}

@keyframes badge-pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.15); }
}

/* Net Change Colors */
.net-change--positive {
  color: var(--accent-primary);
  font-weight: 600;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--negative {
  color: var(--accent-danger);
  font-weight: 600;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--neutral {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--pending {
  color: var(--text-tertiary);
}

.returned-amount {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

/* New Settlement Row Highlight */
::v-deep .el-table .new-settlement td {
  background: rgba(0, 229, 160, 0.06) !important;
  box-shadow: inset 0 0 20px rgba(0, 229, 160, 0.05);
}

::v-deep .el-table .new-settlement td:first-child {
  position: relative;
}

::v-deep .el-table .new-settlement td:first-child::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--accent-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
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

.points-sub {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 2px;
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
  line-height: 1;
}

.gauge-unit {
  font-size: 14px;
  color: var(--text-tertiary);
  font-weight: 600;
  margin-top: 4px;
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
  max-width: 400px;
  margin: 0 auto;
}

.empty-svg {
  width: 72px;
  height: 72px;
  margin-bottom: 20px;
  opacity: 0.7;
  animation: empty-float 3s ease-in-out infinite;
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
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

.empty-cta {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 28px;
  box-shadow: 0 0 20px rgba(0, 229, 160, 0.2);
  transition: all 0.2s var(--ease-out);
}

.empty-cta:hover {
  transform: translateY(-1px);
  box-shadow: 0 0 30px rgba(0, 229, 160, 0.35);
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
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
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

/* Profit Curve Chart */
.chart-container {
  position: relative;
  width: 100%;
  min-height: 220px;
}

.profit-chart {
  width: 100%;
  height: auto;
  display: block;
  overflow: visible;
}

.chart-axis-line {
  stroke: var(--border-subtle);
  stroke-width: 1;
}

.chart-grid-line {
  stroke: var(--border-subtle);
  stroke-width: 0.5;
  stroke-dasharray: 4 4;
  opacity: 0.5;
}

.chart-fill {
  animation: chart-fade-in 0.8s var(--ease-out) both;
}

.chart-line {
  stroke: var(--accent-primary);
  stroke-width: 2.5;
  stroke-linecap: round;
  stroke-linejoin: round;
  animation: chart-draw 1s var(--ease-out) both;
}

@keyframes chart-draw {
  from {
    stroke-dasharray: 2000;
    stroke-dashoffset: 2000;
  }
  to {
    stroke-dasharray: 2000;
    stroke-dashoffset: 0;
  }
}

@keyframes chart-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.chart-label {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  fill: var(--text-tertiary);
}

.chart-label--y {
  font-size: 11px;
  font-weight: 500;
}

.chart-label--x {
  font-size: 10px;
  font-weight: 400;
}

.chart-dot {
  fill: var(--accent-primary);
  opacity: 0.6;
  transition: opacity 0.15s ease, r 0.15s ease;
}

.chart-hit-area {
  cursor: crosshair;
}

.chart-hit-area:hover ~ .chart-dot {
  opacity: 1;
  r: 5;
}

.chart-tooltip-line {
  stroke: var(--accent-primary);
  stroke-width: 1;
  stroke-dasharray: 3 3;
  opacity: 0.5;
}

.chart-tooltip-dot {
  fill: var(--accent-primary);
  stroke: var(--bg-surface-1);
  stroke-width: 2;
}

.chart-tooltip-bg {
  fill: var(--bg-surface-2);
  stroke: var(--border-subtle);
  stroke-width: 1;
  filter: drop-shadow(0 2px 8px rgba(0, 0, 0, 0.3));
}

.chart-tooltip-text {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.chart-tooltip-text--val {
  font-size: 13px;
  font-weight: 700;
  fill: var(--accent-primary);
}

.chart-tooltip-text--time {
  font-size: 10px;
  font-weight: 400;
  fill: var(--text-tertiary);
}

.chart-empty {
  min-height: 180px;
}

.chart-empty p {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  margin-bottom: 6px;
}

.chart-empty span {
  color: var(--text-tertiary);
  font-size: 13px;
}

/* Bank Quick Link */
.bank-link-card {
  margin-top: 20px;
}

.bank-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  text-decoration: none;
  transition: border-color var(--duration-fast) ease,
              background var(--duration-fast) ease;
}

.bank-link:hover {
  border-color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.04);
}

.bank-link-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.bank-link-right {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-tertiary);
  font-size: 14px;
}

.bank-link-debt {
  font-family: var(--font-mono);
  color: #f56c6c;
}

.bank-link-clean {
  color: var(--accent-primary);
}

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>
