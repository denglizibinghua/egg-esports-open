<template>
  <div class="leaderboard">
    <!-- ── Page header ─────────────────────────────── -->
    <header class="page-header">
      <div class="page-header-main">
        <div class="page-eyebrow u-eyebrow">// LEADERBOARD · SEASON 03</div>
        <h1 class="page-title">排行榜</h1>
        <p class="page-sub">赛季预言家排名 · 每场结算后实时刷新</p>
      </div>
      <div class="page-actions">
        <div class="dim-switch" role="group" aria-label="排行榜维度">
          <button
            type="button"
            class="dim-btn"
            :class="{ active: leaderboardTab === 'points' }"
            :aria-pressed="leaderboardTab === 'points'"
            @click="switchTab('points')"
          >
            <span class="dim-idx">01</span>
            <span class="dim-name">积分榜</span>
          </button>
          <button
            type="button"
            class="dim-btn"
            :class="{ active: leaderboardTab === 'accuracy' }"
            :aria-pressed="leaderboardTab === 'accuracy'"
            @click="switchTab('accuracy')"
          >
            <span class="dim-idx">02</span>
            <span class="dim-name">胜率榜</span>
          </button>
          <button
            type="button"
            class="dim-btn"
            :class="{ active: leaderboardTab === 'worst-profit' }"
            :aria-pressed="leaderboardTab === 'worst-profit'"
            @click="switchTab('worst-profit')"
          >
            <span class="dim-idx">03</span>
            <span class="dim-name">亏损榜</span>
          </button>
        </div>
        <!-- 瓦聚焦：多游戏入口已隐藏，保留Adapter，可?gameId=2调试 -->
        <div class="game-filter" v-if="false">
          <el-select
            v-model="selectedGameId"
            placeholder="瓦洛兰特"
            clearable
            size="small"
            @change="handleGameChange"
          >
            <el-option :value="''" label="瓦洛兰特" />
            <el-option
              v-for="game in games"
              :key="game.id"
              :label="game.name"
              :value="game.id"
            />
          </el-select>
        </div>
      </div>
    </header>

    <!-- ── Loading skeleton ─────────────────────────── -->
    <template v-if="loading">
      <div class="podium podium--skeleton">
        <div class="podium-item silver skeleton">
          <div class="podium-body">
            <div class="skeleton-line skeleton-line--avatar"></div>
            <div class="skeleton-line skeleton-line--name"></div>
            <div class="skeleton-line skeleton-line--badge"></div>
            <div class="skeleton-line skeleton-line--points"></div>
          </div>
          <div class="podium-plinth skeleton-plinth"></div>
        </div>
        <div class="podium-item gold skeleton">
          <div class="podium-body">
            <div class="skeleton-line skeleton-line--crown"></div>
            <div class="skeleton-line skeleton-line--avatar skeleton-line--avatar-lg"></div>
            <div class="skeleton-line skeleton-line--name"></div>
            <div class="skeleton-line skeleton-line--badge"></div>
            <div class="skeleton-line skeleton-line--points"></div>
          </div>
          <div class="podium-plinth skeleton-plinth"></div>
        </div>
        <div class="podium-item bronze skeleton">
          <div class="podium-body">
            <div class="skeleton-line skeleton-line--avatar"></div>
            <div class="skeleton-line skeleton-line--name"></div>
            <div class="skeleton-line skeleton-line--badge"></div>
            <div class="skeleton-line skeleton-line--points"></div>
          </div>
          <div class="podium-plinth skeleton-plinth"></div>
        </div>
      </div>

      <div class="skeleton-table">
        <div class="skeleton-table-head">
          <div class="skeleton-line skeleton-line--head"></div>
          <div class="skeleton-line skeleton-line--head-sm"></div>
        </div>
        <div v-for="i in 6" :key="'skel-row-' + i" class="skeleton-table-row">
          <div class="skeleton-line skeleton-line--rank-sm"></div>
          <div class="skeleton-line skeleton-line--avatar-sm"></div>
          <div class="skeleton-line skeleton-line--nickname"></div>
          <div class="skeleton-line skeleton-line--score"></div>
          <div class="skeleton-line skeleton-line--score"></div>
          <div class="skeleton-line skeleton-line--score-sm"></div>
        </div>
      </div>
    </template>

    <!-- ── Empty state ──────────────────────────────── -->
    <div v-else-if="leaderboard.length === 0" class="empty-state">
      <span class="empty-mark" aria-hidden="true">//</span>
      <h3 class="empty-title">{{ emptyTitle }}</h3>
      <p class="empty-desc">{{ emptyDesc }}</p>
      <el-button type="primary" class="empty-cta" @click="$router.push('/matches')">去赛事列表押注</el-button>
    </div>

    <!-- ── Content ──────────────────────────────────── -->
    <template v-else>
      <!-- Top 3 podium -->
      <section v-if="top3.length > 0" class="podium-block">
        <div class="podium-topline">
          <span class="podium-topline-label">// TOP 03</span>
          <span class="podium-topline-meta">{{ dimensionLabel }} · 赛季领跑</span>
        </div>

        <div class="podium">
          <!-- 2nd -->
          <div v-if="top3[1]" class="podium-item silver">
            <div class="podium-body">
              <div class="podium-avatar">
                <el-avatar :size="58" :src="top3[1].avatar" :icon="UserFilled" />
                <span class="podium-rank-plate">02</span>
              </div>
              <div class="podium-name" :title="displayName(top3[1])">{{ displayName(top3[1]) }}</div>
              <LevelBadge class="podium-level" :count="getBetCount(top3[1])" />
              <div class="podium-points">
                <span class="podium-points-label">{{ podiumMetricLabel }}</span>
                <span
                  :class="['podium-points-value', leaderboardTab === 'worst-profit' ? profitClass(top3[1].netProfit) : '']"
                >
                  <template v-if="leaderboardTab === 'points'">{{ top3[1].points }}</template>
                  <template v-else-if="leaderboardTab === 'accuracy'">{{ getAccuracy(top3[1]) }}%</template>
                  <template v-else-if="leaderboardTab === 'worst-profit'">{{ top3[1].netProfit || 0 }}</template>
                </span>
              </div>
            </div>
            <div class="podium-plinth">
              <span class="podium-plinth-rank">2</span>
              <span class="podium-plinth-label">RUNNER-UP</span>
            </div>
          </div>

          <!-- 1st -->
          <div v-if="top3[0]" class="podium-item gold">
            <div class="podium-crown" aria-hidden="true">
              <svg viewBox="0 0 48 30" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path
                  d="M5 24 L3 8 L14 15 L24 3 L34 15 L45 8 L43 24 Z"
                  fill="rgba(216,178,92,0.12)"
                  stroke="currentColor"
                  stroke-width="1.5"
                  stroke-linejoin="round"
                />
                <path d="M5 24 H43" stroke="currentColor" stroke-width="3" stroke-linecap="square" />
                <circle cx="24" cy="3" r="2" fill="currentColor" />
              </svg>
            </div>
            <div class="podium-body">
              <div class="podium-avatar">
                <el-avatar :size="78" :src="top3[0].avatar" :icon="UserFilled" />
                <span class="podium-rank-plate">01</span>
              </div>
              <div class="podium-name" :title="displayName(top3[0])">{{ displayName(top3[0]) }}</div>
              <LevelBadge class="podium-level" :count="getBetCount(top3[0])" />
              <div class="podium-points">
                <span class="podium-points-label">{{ podiumMetricLabel }}</span>
                <span
                  :class="['podium-points-value', leaderboardTab === 'worst-profit' ? profitClass(top3[0].netProfit) : '']"
                >
                  <template v-if="leaderboardTab === 'points'">{{ top3[0].points }}</template>
                  <template v-else-if="leaderboardTab === 'accuracy'">{{ getAccuracy(top3[0]) }}%</template>
                  <template v-else-if="leaderboardTab === 'worst-profit'">{{ top3[0].netProfit || 0 }}</template>
                </span>
              </div>
            </div>
            <div class="podium-plinth">
              <span class="podium-plinth-rank">1</span>
              <span class="podium-plinth-label">CHAMPION</span>
            </div>
          </div>

          <!-- 3rd -->
          <div v-if="top3[2]" class="podium-item bronze">
            <div class="podium-body">
              <div class="podium-avatar">
                <el-avatar :size="58" :src="top3[2].avatar" :icon="UserFilled" />
                <span class="podium-rank-plate">03</span>
              </div>
              <div class="podium-name" :title="displayName(top3[2])">{{ displayName(top3[2]) }}</div>
              <LevelBadge class="podium-level" :count="getBetCount(top3[2])" />
              <div class="podium-points">
                <span class="podium-points-label">{{ podiumMetricLabel }}</span>
                <span
                  :class="['podium-points-value', leaderboardTab === 'worst-profit' ? profitClass(top3[2].netProfit) : '']"
                >
                  <template v-if="leaderboardTab === 'points'">{{ top3[2].points }}</template>
                  <template v-else-if="leaderboardTab === 'accuracy'">{{ getAccuracy(top3[2]) }}%</template>
                  <template v-else-if="leaderboardTab === 'worst-profit'">{{ top3[2].netProfit || 0 }}</template>
                </span>
              </div>
            </div>
            <div class="podium-plinth">
              <span class="podium-plinth-rank">3</span>
              <span class="podium-plinth-label">THIRD</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 胜率榜门槛提示 -->
      <div v-if="leaderboardTab === 'accuracy'" class="leaderboard-notice">
        <span class="notice-tag">INFO</span>
        <span>需累计押注 <strong>10 场以上</strong> 才能进入胜率榜</span>
      </div>

      <!-- Full leaderboard table -->
      <section class="table-panel">
        <div class="table-head">
          <span class="table-head-title">全部选手</span>
          <span class="table-head-meta">PAGE {{ currentPage }} · TOTAL {{ total }}</span>
        </div>

        <div class="table-scroll">
          <el-table
            :data="leaderboard"
            style="width: 100%"
            class="leaderboard-table"
            :row-class-name="rowClassName"
          >
            <el-table-column label="排名" width="72" align="center">
              <template #default="scope">
                <span
                  v-if="absRank(scope.$index) <= 3"
                  :class="['rank-medal', getRankClass(absRank(scope.$index))]"
                >
                  {{ absRank(scope.$index) }}
                </span>
                <span v-else class="rank-plain">{{ absRank(scope.$index) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="昵称" min-width="200">
              <template #default="scope">
                <div class="player-cell">
                  <el-avatar
                    :size="30"
                    :src="scope.row.avatar"
                    :icon="UserFilled"
                    class="player-avatar"
                  />
                  <div class="player-copy">
                    <span class="player-name" :title="displayName(scope.row)">{{ displayName(scope.row) }}</span>
                    <span class="player-sub">
                      <LevelBadge :count="getBetCount(scope.row)" :show-progress="false" />
                      <span v-if="isCurrentUser(scope.row)" class="you-chip">YOU</span>
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- Points tab: 积分 + 胜率 + 连胜 -->
            <template v-if="leaderboardTab === 'points'">
              <el-table-column label="积分" width="118" align="center">
                <template #default="scope">
                  <div class="points-cell">
                    <span class="points-value">{{ scope.row.points }}</span>
                    <span class="level-rail" aria-hidden="true">
                      <span
                        class="level-rail-fill"
                        :style="{
                          width: getLevelProgress(getBetCount(scope.row)) + '%',
                          background: getLevelColor(getLevel(getBetCount(scope.row)))
                        }"
                      ></span>
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="胜率" width="112" align="center">
                <template #default="scope">
                  <div class="acc-cell">
                    <span class="acc-bar"><span class="acc-bar-fill" :style="{ width: getAccuracy(scope.row) + '%' }"></span></span>
                    <span class="acc-num">{{ getAccuracy(scope.row) }}%</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="连胜" width="84" align="center">
                <template #default="scope">
                  <span v-if="scope.row.winStreak > 0" :class="['streak-val', { 'streak-val--hot': scope.row.winStreak >= 3 }]">
                    <span class="streak-x">×</span>{{ scope.row.winStreak }}
                  </span>
                  <span v-else class="streak-zero">—</span>
                </template>
              </el-table-column>
            </template>

            <!-- Accuracy tab: 胜率 + 猜中/总场 + 连胜 -->
            <template v-if="leaderboardTab === 'accuracy'">
              <el-table-column label="胜率" width="112" align="center">
                <template #default="scope">
                  <div class="acc-cell">
                    <span class="acc-bar"><span class="acc-bar-fill" :style="{ width: getAccuracy(scope.row) + '%' }"></span></span>
                    <span class="acc-num">{{ getAccuracy(scope.row) }}%</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="猜中/总场" width="110" align="center">
                <template #default="scope">
                  <span class="hit-ratio">{{ scope.row.correctBets }}<em>/{{ scope.row.totalBets }}</em></span>
                </template>
              </el-table-column>
              <el-table-column label="连胜" width="84" align="center">
                <template #default="scope">
                  <span v-if="scope.row.winStreak > 0" :class="['streak-val', { 'streak-val--hot': scope.row.winStreak >= 3 }]">
                    <span class="streak-x">×</span>{{ scope.row.winStreak }}
                  </span>
                  <span v-else class="streak-zero">—</span>
                </template>
              </el-table-column>
            </template>

            <!-- Worst-profit tab: 净收益 + 积分 + 胜率 -->
            <template v-if="leaderboardTab === 'worst-profit'">
              <el-table-column label="净收益" width="110" align="center">
                <template #default="scope">
                  <span :class="['money-val', profitClass(scope.row.netProfit)]">
                    {{ scope.row.netProfit != null ? (scope.row.netProfit >= 0 ? '+' : '') + scope.row.netProfit : 0 }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column label="积分" width="118" align="center">
                <template #default="scope">
                  <div class="points-cell">
                    <span class="points-value">{{ scope.row.points }}</span>
                    <span class="level-rail" aria-hidden="true">
                      <span
                        class="level-rail-fill"
                        :style="{
                          width: getLevelProgress(getBetCount(scope.row)) + '%',
                          background: getLevelColor(getLevel(getBetCount(scope.row)))
                        }"
                      ></span>
                    </span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column label="胜率" width="112" align="center">
                <template #default="scope">
                  <div class="acc-cell">
                    <span class="acc-bar"><span class="acc-bar-fill" :style="{ width: getAccuracy(scope.row) + '%' }"></span></span>
                    <span class="acc-num">{{ getAccuracy(scope.row) }}%</span>
                  </div>
                </template>
              </el-table-column>
            </template>
          </el-table>
        </div>

        <!-- 移动端：表格折叠为紧凑名单，避免横向裁切 -->
        <div class="roster-mobile">
          <div
            v-for="(row, index) in leaderboard"
            :key="'mob-' + (row.userId || row.id || index) + '-' + index"
            class="roster-row"
            :class="{ 'is-self': isCurrentUser(row) }"
          >
            <span
              :class="absRank(index) <= 3 ? 'rank-medal ' + getRankClass(absRank(index)) : 'rank-plain'"
              class="roster-rank"
            >{{ absRank(index) }}</span>
            <el-avatar :size="32" :src="row.avatar" :icon="UserFilled" class="roster-avatar" />
            <div class="roster-main">
              <div class="roster-name" :title="displayName(row)">
                <span class="roster-name-text">{{ displayName(row) }}</span>
                <span v-if="isCurrentUser(row)" class="you-chip">YOU</span>
              </div>
              <div class="roster-sub">
                <LevelBadge :count="getBetCount(row)" :show-progress="false" />
                <template v-if="leaderboardTab === 'points'">
                  <span>胜率 {{ getAccuracy(row) }}%</span>
                  <span v-if="row.winStreak > 0">连胜 {{ row.winStreak }}</span>
                </template>
                <template v-else-if="leaderboardTab === 'accuracy'">
                  <span>猜中 {{ row.correctBets }}/{{ row.totalBets }}</span>
                  <span v-if="row.winStreak > 0">连胜 {{ row.winStreak }}</span>
                </template>
                <template v-else>
                  <span>胜率 {{ getAccuracy(row) }}%</span>
                  <span>积分 {{ row.points }}</span>
                </template>
              </div>
            </div>
            <div class="roster-value">
              <template v-if="leaderboardTab === 'points'">
                <span>{{ row.points }}</span><em>PTS</em>
              </template>
              <template v-else-if="leaderboardTab === 'accuracy'">
                <span>{{ getAccuracy(row) }}</span><em>PCT</em>
              </template>
              <template v-else>
                <span :class="profitClass(row.netProfit)">{{ row.netProfit != null ? (row.netProfit >= 0 ? '+' : '') + row.netProfit : 0 }}</span><em>P/L</em>
              </template>
            </div>
          </div>
        </div>
      </section>
    </template>

    <el-pagination
      v-if="total > 0"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="fetchLeaderboard"
    />
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { getLevel, getLevelColor, getLevelProgress, getBetCount } from '@/utils/level'
import { getUser } from '@/utils/auth'
import { UserFilled } from '@element-plus/icons-vue'
import LevelBadge from '@/components/user/LevelBadge.vue'

export default {
  name: 'LeaderboardPage',
  components: { LevelBadge },
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      UserFilled: markRaw(UserFilled),
      leaderboard: [],
      top3: [],
      loading: false,
      currentPage: 1,
      pageSize: 20,
      total: 0,
      games: [],
      selectedGameId: '1',
      leaderboardTab: 'points'
    }
  },
  created() {
    var q = this.$route && this.$route.query && this.$route.query.gameId
    if (q !== undefined && q !== null && q !== '') {
      this.selectedGameId = String(q)
    }
    this.fetchGames()
    this.fetchTop3()
    this.fetchLeaderboard()
  },
  computed: {
    emptyTitle() {
      if (this.leaderboardTab === 'accuracy') return '暂无上榜玩家'
      if (this.leaderboardTab === 'worst-profit') return '暂无亏损数据'
      return '还没有人上榜'
    },
    emptyDesc() {
      if (this.leaderboardTab === 'accuracy') return '参与 10 场以上才能进入胜率榜'
      if (this.leaderboardTab === 'worst-profit') return '暂无投注记录，快去押注吧！'
      return '成为第一个预言家！'
    },
    dimensionLabel() {
      if (this.leaderboardTab === 'accuracy') return '胜率榜'
      if (this.leaderboardTab === 'worst-profit') return '亏损榜'
      return '积分榜'
    },
    podiumMetricLabel() {
      if (this.leaderboardTab === 'accuracy') return 'WIN RATE'
      if (this.leaderboardTab === 'worst-profit') return 'NET PROFIT'
      return 'POINTS'
    },
    currentUserId() {
      const user = getUser()
      return user && user.id != null ? Number(user.id) : null
    }
  },
  methods: {
    fetchGames() {
      request.get('/game/list').then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.games = extractList(data)
      }).catch(() => {})
    },
    handleGameChange() {
      this.currentPage = 1
      this.fetchTop3()
      this.fetchLeaderboard()
    },
    switchTab(tab) {
      if (this.leaderboardTab === tab) return
      this.leaderboardTab = tab
      this.currentPage = 1
      this.fetchTop3()
      this.fetchLeaderboard()
    },
    fetchTop3() {
      const params = {}
      if (this.selectedGameId != '') {
        params.gameId = this.selectedGameId
      }
      const endpoints = {
        'points': '/leaderboard/top/3',
        'accuracy': '/leaderboard/accuracy/top/3',
        'worst-profit': '/leaderboard/worst-profit/top/3'
      }
      const url = endpoints[this.leaderboardTab] || '/leaderboard/top/3'
      request.get(url, { params }).then(res => {
        const data = res.data || res
        this.top3 = Array.isArray(data) ? data : []
      }).catch(() => { this.$message.error('加载失败，请刷新重试') })
    },
    fetchLeaderboard() {
      this.loading = true
      const params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.selectedGameId != '') {
        params.gameId = this.selectedGameId
      }
      const endpoints = {
        'points': '/leaderboard/points',
        'accuracy': '/leaderboard/accuracy',
        'worst-profit': '/leaderboard/worst-profit'
      }
      const url = endpoints[this.leaderboardTab] || '/leaderboard/points'
      request.get(url, { params }).then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.leaderboard = extractList(data)
        if (Array.isArray(data)) {
          this.total = data.length
        } else if (data.records) {
          this.total = data.total || data.records.length
        } else {
          this.total = data.total || data.totalElements || 0
        }
      }).catch(() => { this.$message.error('加载失败，请刷新重试') }).finally(() => {
        this.loading = false
      })
    },
    getAccuracy(row) {
      if (!row.totalBets || row.totalBets === 0) return 0
      return Math.min(100, Math.round((row.correctBets / row.totalBets) * 100))
    },
    getBetCount(row) { return getBetCount(row) },
    getLevel(count) { return getLevel(count) },
    getLevelColor(lv) { return getLevelColor(lv) || 'var(--brand)' },
    getLevelProgress(count) { return getLevelProgress(count) },
    getRankClass(rank) {
      if (rank === 1) return 'rank-gold'
      if (rank === 2) return 'rank-silver'
      if (rank === 3) return 'rank-bronze'
      return ''
    },
    absRank(index) {
      return index + 1 + (this.currentPage - 1) * this.pageSize
    },
    displayName(row) {
      if (!row) return '匿名玩家'
      return row.nickname || row.username || '匿名玩家'
    },
    profitClass(value) {
      return value != null && Number(value) < 0 ? 'is-neg' : 'is-pos'
    },
    isCurrentUser(row) {
      if (!row || this.currentUserId == null || row.userId == null) return false
      return Number(row.userId) === this.currentUserId
    },
    rowClassName({ row }) {
      return this.isCurrentUser(row) ? 'is-current-user' : ''
    }
  }
}
</script>

<style scoped>
.leaderboard {
  min-width: 0;
}

/* ── Shared page header ─────────────────────────── */
.page-header {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  padding-bottom: 20px;
  margin-bottom: 28px;
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
}

.page-eyebrow {
  margin-bottom: 10px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
}

.page-sub {
  margin-top: 8px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.page-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  min-width: 0;
}

.game-filter {
  display: flex;
  align-items: center;
}

.game-filter :deep(.el-input__inner) {
  min-width: 160px;
}

/* ── Dimension segmented control ────────────────── */
.dim-switch {
  display: flex;
  align-items: stretch;
  gap: 2px;
  padding: 3px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.dim-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  height: 34px;
  padding: 0 12px;
  background: transparent;
  border: 0;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  font-family: var(--font-body);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out),
    background var(--duration-fast) var(--ease-out),
    box-shadow var(--duration-fast) var(--ease-out);
}

.dim-btn:hover {
  color: var(--text-primary);
  background: rgba(255, 255, 255, 0.03);
}

.dim-btn.active {
  color: var(--text-primary);
  background: rgba(255, 70, 85, 0.10);
  box-shadow: inset 0 -2px 0 var(--brand);
}

.dim-idx {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-faint);
}

.dim-btn.active .dim-idx {
  color: var(--brand);
}

/* ── Podium ─────────────────────────────────────── */
.podium-block {
  margin-bottom: 30px;
}

.podium-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.podium-topline-label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--brand);
}

.podium-topline-meta {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.podium {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1.18fr 1fr;
  align-items: end;
  gap: 18px;
  padding: 30px 0 0;
}

.podium::before {
  content: '';
  position: absolute;
  left: 8%;
  right: 8%;
  top: 0;
  bottom: 20%;
  background: radial-gradient(ellipse 55% 60% at 50% 10%, rgba(216, 178, 92, 0.06), transparent 72%);
  pointer-events: none;
}

.podium-item {
  --metal: var(--silver);
  --metal-soft: rgba(169, 176, 190, 0.12);
  --plinth-h: 64px;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-width: 0;
  text-align: center;
  transition: transform var(--duration-normal) var(--ease-out);
}

.podium-item.silver {
  grid-column: 1;
  grid-row: 1;
}

.podium-item:hover {
  transform: translateY(-3px);
}

.podium-item.gold {
  --metal: var(--gold);
  --metal-soft: rgba(216, 178, 92, 0.14);
  --plinth-h: 92px;
  grid-column: 2;
  grid-row: 1;
}

.podium-item.bronze {
  --metal: var(--bronze);
  --metal-soft: rgba(192, 122, 69, 0.12);
  --plinth-h: 52px;
  grid-column: 3;
  grid-row: 1;
}

.podium-crown {
  height: 28px;
  margin-bottom: 6px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  color: var(--gold);
  filter: drop-shadow(0 0 10px rgba(216, 178, 92, 0.38));
}

.podium-crown svg {
  display: block;
  height: 100%;
  width: auto;
}

.podium-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 0;
  padding: 0 6px 4px;
}

.podium-avatar {
  position: relative;
  display: inline-flex;
  padding: 4px;
  margin-bottom: 12px;
  border: 1px solid var(--metal);
  border-radius: 50%;
  background: radial-gradient(circle at 50% 32%, var(--metal-soft), var(--bg-surface-1) 72%);
  box-shadow: 0 0 0 4px rgba(255, 255, 255, 0.018), 0 10px 24px rgba(0, 0, 0, 0.45);
}

.podium-item.gold .podium-avatar {
  border-width: 2px;
  box-shadow: 0 0 0 4px rgba(216, 178, 92, 0.07), 0 0 26px rgba(216, 178, 92, 0.18),
    0 10px 26px rgba(0, 0, 0, 0.5);
}

.podium-avatar :deep(.el-avatar) {
  display: block;
  border-radius: 50%;
  background: var(--bg-surface-3);
}

.podium-rank-plate {
  position: absolute;
  right: -7px;
  bottom: -4px;
  min-width: 24px;
  height: 18px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface-2);
  border: 1px solid var(--metal);
  border-radius: var(--radius-sm);
  color: var(--metal);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.04em;
}

.podium-name {
  width: 100%;
  max-width: 100%;
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.podium-item.gold .podium-name {
  font-size: 17px;
}

.podium-item .podium-level {
  margin: 8px 0 6px;
  align-items: center;
}

.podium-points {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  min-width: 0;
}

.podium-points-label {
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
}

.podium-points-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 15px;
  font-weight: 700;
  line-height: 1.15;
  color: var(--accent-primary);
}

.podium-item.gold .podium-points-value {
  font-size: 20px;
}

.podium-points-value.is-neg {
  color: var(--accent-danger);
}

.podium-points-value.is-pos {
  color: var(--accent-primary);
}

.podium-plinth {
  position: relative;
  height: var(--plinth-h);
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
  border-top: 2px solid var(--metal);
  background: linear-gradient(180deg, var(--metal-soft), rgba(255, 255, 255, 0.012) 55%, transparent 100%);
  overflow: hidden;
  transition: box-shadow var(--duration-normal) var(--ease-out);
}

.podium-plinth::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image: repeating-linear-gradient(
    -45deg,
    rgba(255, 255, 255, 0.04) 0px,
    rgba(255, 255, 255, 0.04) 1px,
    transparent 1px,
    transparent 6px
  );
  opacity: 0.5;
  pointer-events: none;
}

.podium-item:hover .podium-plinth {
  box-shadow: 0 -10px 26px -14px var(--metal);
}

.podium-plinth-rank {
  position: relative;
  font-family: var(--font-display);
  font-size: 40px;
  line-height: 1;
  letter-spacing: 0.04em;
  color: var(--metal);
  opacity: 0.88;
}

.podium-item.gold .podium-plinth-rank {
  font-size: 54px;
}

.podium-plinth-label {
  position: relative;
  text-align: center;
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.20em;
  text-transform: uppercase;
  color: var(--text-faint);
  white-space: nowrap;
}

/* ── Accuracy notice ────────────────────────────── */
.leaderboard-notice {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  margin-bottom: 16px;
  background: rgba(91, 140, 255, 0.06);
  border: 1px solid rgba(91, 140, 255, 0.18);
  border-left: 2px solid var(--accent-secondary);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 12.5px;
}

.notice-tag {
  flex-shrink: 0;
  padding: 2px 6px;
  background: rgba(91, 140, 255, 0.10);
  border: 1px solid rgba(91, 140, 255, 0.26);
  border-radius: 2px;
  color: var(--accent-secondary);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.14em;
  line-height: 1.3;
}

.leaderboard-notice strong {
  color: var(--accent-secondary);
  font-weight: 700;
}

/* ── Table panel ────────────────────────────────── */
.table-panel {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.table-head-title {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.table-head-meta {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.10em;
  white-space: nowrap;
}

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.leaderboard-table {
  width: 100%;
  min-width: 640px;
}

/* Mobile roster */
.roster-mobile {
  display: none;
}

@media (max-width: 700px) {
  .table-scroll {
    display: none;
  }

  .roster-mobile {
    display: flex;
    flex-direction: column;
  }

  .roster-row {
    display: grid;
    grid-template-columns: 28px 32px minmax(0, 1fr) auto;
    align-items: center;
    gap: 10px;
    padding: 11px 6px;
    border-bottom: 1px solid var(--border-subtle);
  }

  .roster-row:last-child {
    border-bottom: 0;
  }

  .roster-row.is-self {
    background: linear-gradient(90deg, var(--brand-soft), transparent 78%);
    box-shadow: inset 2px 0 0 var(--brand);
    border-radius: 2px;
  }

  .roster-rank {
    justify-self: center;
    text-align: center;
  }

  .roster-avatar {
    border: 1px solid var(--border-default);
    background: var(--bg-surface-2);
  }

  .roster-main {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  .roster-name {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
    font-size: 14px;
    font-weight: 700;
    color: var(--text-primary);
  }

  .roster-name-text {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .roster-sub {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
    font-family: var(--font-mono);
    font-size: 10px;
    letter-spacing: 0.03em;
    color: var(--text-tertiary);
  }

  .roster-value {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 1px;
    font-family: var(--font-mono);
    font-size: 15px;
    font-weight: 700;
    color: var(--accent-primary);
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .roster-value em {
    font-style: normal;
    font-size: 8px;
    letter-spacing: 0.14em;
    color: var(--text-faint);
  }
}

/* Rank medal */
.rank-medal {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid currentColor;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.02);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.rank-medal.rank-gold {
  color: var(--gold);
  background: rgba(216, 178, 92, 0.10);
  box-shadow: 0 0 14px rgba(216, 178, 92, 0.14);
}

.rank-medal.rank-silver {
  color: var(--silver);
  background: rgba(169, 176, 190, 0.09);
  box-shadow: 0 0 14px rgba(169, 176, 190, 0.10);
}

.rank-medal.rank-bronze {
  color: var(--bronze);
  background: rgba(192, 122, 69, 0.10);
  box-shadow: 0 0 14px rgba(192, 122, 69, 0.10);
}

.rank-plain {
  font-family: var(--font-mono);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  color: var(--text-tertiary);
}

/* Player cell */
.player-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.player-avatar {
  flex-shrink: 0;
  border: 1px solid var(--border-default);
}

.player-copy {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.player-name {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.player-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.you-chip {
  flex-shrink: 0;
  padding: 1px 4px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: 2px;
  color: var(--brand);
  font-family: var(--font-mono);
  font-size: 8px;
  font-weight: 700;
  letter-spacing: 0.10em;
  line-height: 1.3;
}

/* Points / accuracy / streak cells */
.points-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.points-value {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.level-rail {
  display: block;
  width: 64px;
  height: 3px;
  background: var(--bg-surface-3);
  overflow: hidden;
}

.level-rail-fill {
  display: block;
  height: 100%;
  transition: width 0.4s var(--ease-out);
}

.acc-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.acc-bar {
  display: block;
  width: 64px;
  height: 3px;
  background: var(--bg-surface-3);
  overflow: hidden;
}

.acc-bar-fill {
  display: block;
  height: 100%;
  background: var(--accent-primary);
  opacity: 0.8;
}

.acc-num {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
  line-height: 1;
}

.streak-val {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.streak-val--hot {
  color: var(--brand);
}

.streak-x {
  margin-right: 1px;
  color: var(--text-faint);
  font-size: 10px;
}

.streak-zero {
  color: var(--text-faint);
  font-family: var(--font-mono);
  font-size: 12px;
}

.hit-ratio {
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.hit-ratio em {
  color: var(--text-tertiary);
  font-style: normal;
}

.money-val {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.money-val.is-pos {
  color: var(--accent-primary);
}

.money-val.is-neg {
  color: var(--accent-danger);
}

/* Current user row */
.leaderboard-table :deep(.is-current-user > td.el-table__cell) {
  background: rgba(255, 70, 85, 0.055) !important;
}

.leaderboard-table :deep(.is-current-user > td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 0 var(--brand);
}

.leaderboard-table :deep(.is-current-user:hover > td.el-table__cell) {
  background: rgba(255, 70, 85, 0.09) !important;
}

/* ── Empty state ────────────────────────────────── */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 54px 24px;
  margin: 0 auto;
  max-width: 460px;
  text-align: center;
  background: var(--bg-surface-1);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
  color: var(--brand);
  font-family: var(--font-display);
  font-size: 22px;
  letter-spacing: 0.10em;
  line-height: 1;
}

.empty-title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}

.empty-desc {
  max-width: 320px;
  margin-top: -6px;
  color: var(--text-tertiary);
  font-size: 13px;
}

.empty-cta {
  margin-top: 6px;
}

/* ── Skeletons ──────────────────────────────────── */
@keyframes lb-shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

@keyframes lb-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

.skeleton {
  pointer-events: none;
  animation: lb-fade-in 0.3s ease both;
}

.skeleton-line {
  background: linear-gradient(
    90deg,
    var(--bg-surface-2) 0%,
    var(--bg-surface-3) 45%,
    var(--bg-surface-2) 100%
  );
  background-size: 200% 100%;
  animation: lb-shimmer 1.8s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.podium--skeleton .podium-body {
  width: 100%;
  padding: 16px 12px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}

.skeleton-plinth {
  border-top: 2px solid var(--bg-surface-3);
  background: linear-gradient(180deg, var(--bg-surface-2), transparent 70%);
  opacity: 0.5;
}

.skeleton-line--crown { width: 28px; height: 18px; margin: 0 auto 8px; }
.skeleton-line--avatar {
  width: 56px;
  height: 56px;
  margin: 0 auto 10px;
  border-radius: 50%;
}
.skeleton-line--avatar-lg {
  width: 72px;
  height: 72px;
}
.skeleton-line--name { width: 80px; height: 13px; margin: 0 auto 8px; }
.skeleton-line--badge { width: 48px; height: 16px; margin: 0 auto 8px; }
.skeleton-line--points { width: 64px; height: 16px; margin: 0 auto; }

.skeleton-table {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.skeleton-table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-table-row:last-child {
  border-bottom: 0;
}

.skeleton-line--head { width: 72px; height: 13px; }
.skeleton-line--head-sm { width: 104px; height: 10px; }
.skeleton-line--rank-sm { width: 22px; height: 18px; flex-shrink: 0; }
.skeleton-line--avatar-sm { width: 30px; height: 30px; border-radius: 50%; flex-shrink: 0; }
.skeleton-line--nickname { width: 140px; height: 14px; flex: 1; }
.skeleton-line--score { width: 64px; height: 14px; flex-shrink: 0; }
.skeleton-line--score-sm { width: 42px; height: 14px; flex-shrink: 0; }

/* ── Responsive ─────────────────────────────────── */
@media (max-width: 900px) {
  .page-header {
    align-items: flex-start;
  }

  .page-actions {
    width: 100%;
  }

  .dim-switch {
    width: 100%;
  }

  .dim-btn {
    flex: 1;
  }
}

@media (max-width: 767px) {
  .page-header {
    gap: 14px;
    margin-bottom: 22px;
  }

  .page-title {
    font-size: 28px;
  }

  .podium {
    gap: 8px;
    padding-top: 20px;
  }

  .podium-body {
    padding: 0 2px 2px;
  }

  .podium-crown {
    height: 20px;
    margin-bottom: 4px;
  }

  .podium-avatar {
    padding: 3px;
    margin-bottom: 8px;
  }

  .podium-avatar :deep(.el-avatar) {
    width: 44px !important;
    height: 44px !important;
    line-height: 44px !important;
  }

  .podium-item.gold .podium-avatar :deep(.el-avatar) {
    width: 54px !important;
    height: 54px !important;
    line-height: 54px !important;
  }

  .podium-name {
    font-size: 12.5px;
  }

  .podium-item.gold .podium-name {
    font-size: 13.5px;
  }

  .podium-level :deep(.level-rail) {
    width: 42px;
  }

  .podium-points-value {
    font-size: 13px;
  }

  .podium-item.gold .podium-points-value {
    font-size: 16px;
  }

  .podium-item {
    --plinth-h: 48px;
  }

  .podium-item.gold {
    --plinth-h: 64px;
  }

  .podium-item.bronze {
    --plinth-h: 40px;
  }

  .podium-plinth-rank {
    font-size: 28px;
  }

  .podium-item.gold .podium-plinth-rank {
    font-size: 38px;
  }

  .podium-plinth-label {
    font-size: 7px;
    letter-spacing: 0.14em;
  }

  .table-head {
    padding: 12px 14px;
  }

  .table-head-meta {
    font-size: 9px;
  }
}

@media (max-width: 430px) {
  .page-title {
    font-size: 24px;
  }

  .podium {
    gap: 6px;
  }

  .dim-btn {
    gap: 5px;
    padding: 0 8px;
    font-size: 12px;
  }

  .dim-idx {
    display: none;
  }

  .podium-name,
  .podium-item.gold .podium-name {
    font-size: 11.5px;
  }

  .podium-avatar :deep(.el-avatar) {
    width: 38px !important;
    height: 38px !important;
    line-height: 38px !important;
  }

  .podium-item.gold .podium-avatar :deep(.el-avatar) {
    width: 46px !important;
    height: 46px !important;
    line-height: 46px !important;
  }

  .podium-points-label {
    font-size: 7px;
    letter-spacing: 0.10em;
  }

  .podium-level :deep(.level-badge) {
    height: 16px;
    padding: 0 4px;
  }

  .podium-level :deep(.lv-num) {
    font-size: 10px;
  }

  .podium-level :deep(.level-rail) {
    width: 36px;
  }

  .podium-rank-plate {
    right: -4px;
    min-width: 20px;
    height: 16px;
    font-size: 9px;
  }
}
</style>
