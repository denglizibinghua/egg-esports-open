<template>
  <div class="leaderboard">
    <div class="page-header">
      <h2 class="page-title">排行</h2>
      <div class="page-header-right">
        <div class="leaderboard-tabs">
          <span
            :class="['leaderboard-tab', { active: leaderboardTab === 'points' }]"
            @click="switchTab('points')"
          >🏆 积分榜</span>
          <span
            :class="['leaderboard-tab', { active: leaderboardTab === 'accuracy' }]"
            @click="switchTab('accuracy')"
          >🎯 胜率榜</span>
          <span
            :class="['leaderboard-tab', { active: leaderboardTab === 'worst-profit' }]"
            @click="switchTab('worst-profit')"
          >😈 亏损榜</span>
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
    </div>

    <!-- Loading Skeleton -->
    <template v-if="loading">
      <div class="podium">
        <div class="podium-item silver skeleton">
          <div class="skeleton-line skeleton-line--rank"></div>
          <div class="skeleton-line skeleton-line--avatar"></div>
          <div class="skeleton-line skeleton-line--name"></div>
          <div class="skeleton-line skeleton-line--points"></div>
        </div>
        <div class="podium-item gold skeleton">
          <div class="skeleton-line skeleton-line--crown"></div>
          <div class="skeleton-line skeleton-line--rank"></div>
          <div class="skeleton-line skeleton-line--avatar skeleton-line--avatar-lg"></div>
          <div class="skeleton-line skeleton-line--name"></div>
          <div class="skeleton-line skeleton-line--points"></div>
        </div>
        <div class="podium-item bronze skeleton">
          <div class="skeleton-line skeleton-line--rank"></div>
          <div class="skeleton-line skeleton-line--avatar"></div>
          <div class="skeleton-line skeleton-line--name"></div>
          <div class="skeleton-line skeleton-line--points"></div>
        </div>
      </div>
      <div class="skeleton-table">
        <div v-for="i in 5" :key="'skel-row-'+i" class="skeleton-table-row">
          <div class="skeleton-line skeleton-line--rank-sm"></div>
          <div class="skeleton-line skeleton-line--nickname"></div>
          <div class="skeleton-line skeleton-line--score"></div>
          <div class="skeleton-line skeleton-line--score"></div>
        </div>
      </div>
    </template>

    <!-- Empty State -->
    <div v-else-if="leaderboard.length === 0" class="empty-state">
      <svg class="empty-svg" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M25 18 H55 L50 48 H30 Z" stroke="#d4a843" stroke-width="2" fill="rgba(212,168,67,0.06)"/>
        <path d="M25 18 C14 18, 10 28, 17 35 L25 32" stroke="#d4a843" stroke-width="1.5" fill="none" opacity="0.5"/>
        <path d="M55 18 C66 18, 70 28, 63 35 L55 32" stroke="#d4a843" stroke-width="1.5" fill="none" opacity="0.5"/>
        <rect x="34" y="48" width="12" height="8" rx="1" fill="rgba(212,168,67,0.12)" stroke="#d4a843" stroke-width="1"/>
        <rect x="28" y="56" width="24" height="6" rx="2" fill="rgba(212,168,67,0.08)" stroke="#d4a843" stroke-width="1"/>
        <path d="M36 28 L40 22 L44 28 L40 26 Z" fill="#d4a843" opacity="0.5"/>
        <circle cx="40" cy="12" r="4" fill="none" stroke="#d4a843" stroke-width="1" opacity="0.4"/>
        <line x1="40" y1="8" x2="40" y2="5" stroke="#d4a843" stroke-width="1" opacity="0.3"/>
        <line x1="36" y1="10" x2="34" y2="7" stroke="#d4a843" stroke-width="1" opacity="0.3"/>
        <line x1="44" y1="10" x2="46" y2="7" stroke="#d4a843" stroke-width="1" opacity="0.3"/>
        <circle cx="40" cy="36" r="3" fill="none" stroke="#d4a843" stroke-width="1" opacity="0.3"/>
      </svg>
      <h3 class="empty-title">{{ emptyTitle }}</h3>
      <p class="empty-desc">{{ emptyDesc }}</p>
      <el-button type="primary" class="empty-cta" @click="$router.push('/matches')">去赛事列表押注</el-button>
    </div>

    <!-- Content -->
    <template v-else>
      <!-- Top 3 Podium -->
      <div v-if="top3.length > 0" class="podium">
        <div v-if="top3[1]" class="podium-item silver">
          <div class="podium-rank">2</div>
            <div class="podium-avatar">
              <el-avatar :size="56" :src="top3[1].avatar" :icon="UserFilled" />
            </div>
            <div class="podium-name">{{ top3[1].nickname || top3[1].username }}</div>
          <div class="podium-points">
            <template v-if="leaderboardTab === 'points'">{{ top3[1].points }} 积分</template>
            <template v-else-if="leaderboardTab === 'accuracy'">胜率 {{ getAccuracy(top3[1]) }}%</template>
            <template v-else-if="leaderboardTab === 'worst-profit'">净收益 {{ top3[1].netProfit || 0 }}</template>
          </div>
        </div>

        <div v-if="top3[0]" class="podium-item gold">
          <div class="podium-crown">👑</div>
          <div class="podium-rank">1</div>
            <div class="podium-avatar">
              <el-avatar :size="72" :src="top3[0].avatar" :icon="UserFilled" />
            </div>
            <div class="podium-name">{{ top3[0].nickname || top3[0].username }}</div>
          <div class="podium-points">
            <template v-if="leaderboardTab === 'points'">{{ top3[0].points }} 积分</template>
            <template v-else-if="leaderboardTab === 'accuracy'">胜率 {{ getAccuracy(top3[0]) }}%</template>
            <template v-else-if="leaderboardTab === 'worst-profit'">净收益 {{ top3[0].netProfit || 0 }}</template>
          </div>
        </div>

        <div v-if="top3[2]" class="podium-item bronze">
          <div class="podium-rank">3</div>
            <div class="podium-avatar">
              <el-avatar :size="56" :src="top3[2].avatar" :icon="UserFilled" />
            </div>
            <div class="podium-name">{{ top3[2].nickname || top3[2].username }}</div>
          <div class="podium-points">
            <template v-if="leaderboardTab === 'points'">{{ top3[2].points }} 积分</template>
            <template v-else-if="leaderboardTab === 'accuracy'">胜率 {{ getAccuracy(top3[2]) }}%</template>
            <template v-else-if="leaderboardTab === 'worst-profit'">净收益 {{ top3[2].netProfit || 0 }}</template>
          </div>
        </div>
      </div>

      <!-- 胜率榜门槛提示 -->
      <div v-if="leaderboardTab === 'accuracy'" class="leaderboard-notice">
        💡 需累计押注 <strong>10 场以上</strong> 才能进入胜率榜
      </div>

      <!-- Full Leaderboard Table -->
      <div class="table-scroll">
        <el-table :data="leaderboard" style="width: 100%" class="leaderboard-table">
          <el-table-column label="排名" width="80" align="center">
            <template #default="scope">
              <span :class="getRankClass(scope.$index + 1 + (currentPage - 1) * pageSize)">
                {{ scope.$index + 1 + (currentPage - 1) * pageSize }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="昵称" min-width="150">
            <template #default="scope">
              <span>{{ scope.row.nickname || scope.row.username }}</span>
            </template>
          </el-table-column>
          <!-- Points tab: 积分 + 胜率 + 连胜 -->
          <template v-if="leaderboardTab === 'points'">
            <el-table-column label="积分" width="100" align="center" prop="points" />
            <el-table-column label="胜率" width="100" align="center">
              <template #default="scope">
                {{ getAccuracy(scope.row) }}%
              </template>
            </el-table-column>
            <el-table-column label="连胜" width="80" align="center" prop="winStreak" />
          </template>
          <!-- Accuracy tab: 胜率 + 猜中/总场 + 连胜 -->
          <template v-if="leaderboardTab === 'accuracy'">
            <el-table-column label="胜率" width="100" align="center">
              <template #default="scope">
                {{ getAccuracy(scope.row) }}%
              </template>
            </el-table-column>
            <el-table-column label="猜中/总场" width="120" align="center">
              <template #default="scope">{{ scope.row.correctBets }}/{{ scope.row.totalBets }}</template>
            </el-table-column>
            <el-table-column label="连胜" width="80" align="center" prop="winStreak" />
          </template>
          <!-- Worst-profit tab: 净收益 + 积分 + 胜率 -->
          <template v-if="leaderboardTab === 'worst-profit'">
            <el-table-column label="净收益" width="110" align="center">
              <template #default="scope">
                <span :style="{ color: scope.row.netProfit >= 0 ? '#00e5a0' : '#ff4d4f' }">
                  {{ scope.row.netProfit != null ? (scope.row.netProfit >= 0 ? '+' : '') + scope.row.netProfit : 0 }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="积分" width="100" align="center" prop="points" />
            <el-table-column label="胜率" width="100" align="center">
              <template #default="scope">
                {{ getAccuracy(scope.row) }}%
              </template>
            </el-table-column>
          </template>
        </el-table>
      </div>
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
import { UserFilled } from '@element-plus/icons-vue'

export default {
  name: 'LeaderboardPage',
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
    getRankClass(rank) {
      if (rank === 1) return 'rank-gold'
      if (rank === 2) return 'rank-silver'
      if (rank === 3) return 'rank-bronze'
      return ''
    }
  }
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.game-filter {
  display: flex;
  align-items: center;
}

.page-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.leaderboard-tabs {
  display: flex;
  gap: 4px;
  background: var(--bg-surface-1);
  border-radius: var(--radius-md);
  padding: 4px;
  border: 1px solid var(--border-subtle);
}

.leaderboard-tab {
  padding: 6px 16px;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
  white-space: nowrap;
  user-select: none;
}

.leaderboard-tab:hover {
  color: var(--text-primary);
  background: var(--bg-surface-2);
}

.leaderboard-tab.active {
  color: #0a0a0f;
  background: var(--accent-primary);
  font-weight: 600;
  box-shadow: 0 0 12px rgba(0, 229, 160, 0.2);
}

.game-filter :deep(.el-input__inner) {
  background: var(--bg-surface-1);
  border-color: var(--border-default);
  color: var(--text-primary);
  border-radius: var(--radius-md);
  min-width: 160px;
}

.game-filter :deep(.el-input__inner:focus) {
  border-color: var(--accent-primary);
}

.podium {
  display: flex;
  justify-content: center;
  align-items: flex-end;
  gap: 20px;
  margin-bottom: 40px;
  padding: 32px 0;
}

.podium-item {
  background: var(--bg-surface-1);
  border-radius: var(--radius-lg);
  padding: 24px 28px;
  text-align: center;
  min-width: 160px;
  position: relative;
  transition: transform var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.podium-item:hover {
  transform: translateY(-4px);
}

.podium-item.gold {
  border: 2px solid #d4a843;
  padding-bottom: 32px;
  min-width: 180px;
  box-shadow: 0 0 24px rgba(212,168,67,0.15), 0 8px 24px rgba(0,0,0,0.4);
}

.podium-item.silver {
  border: 2px solid #a0a0a0;
  box-shadow: 0 0 16px rgba(160,160,160,0.10), 0 4px 16px rgba(0,0,0,0.35);
}

.podium-item.bronze {
  border: 2px solid #c47a3a;
  box-shadow: 0 0 16px rgba(196,122,58,0.10), 0 4px 16px rgba(0,0,0,0.35);
}

.podium-crown {
  font-size: 28px;
  margin-bottom: 4px;
}

.podium-rank {
  font-family: var(--font-display);
  font-size: 24px;
  margin-bottom: 8px;
}

.gold .podium-rank {
  color: #d4a843;
}

.silver .podium-rank {
  color: #a0a0a0;
}

.bronze .podium-rank {
  color: #c47a3a;
}

.podium-name {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.podium-points {
  color: var(--accent-primary);
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.rank-gold {
  color: #d4a843;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.rank-silver {
  color: #a0a0a0;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.rank-bronze {
  color: #c47a3a;
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 700;
}

.leaderboard-table {
  margin-top: 8px;
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
  width: 80px;
  height: 80px;
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

.skeleton .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-2) 0%, var(--bg-surface-3) 35%, rgba(0,229,160,0.03) 50%, var(--bg-surface-3) 65%, var(--bg-surface-2) 100%);
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--rank { width: 24px; height: 24px; margin: 0 auto 8px; }
.skeleton-line--crown { width: 28px; height: 20px; margin: 0 auto 4px; }
.skeleton-line--avatar {
  width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 8px;
  animation: shimmer 2s ease-in-out infinite, skeleton-avatar-pulse 2.5s ease-in-out infinite;
}
.skeleton-line--avatar-lg {
  width: 72px; height: 72px;
  animation: shimmer 2s ease-in-out infinite, skeleton-avatar-pulse 2.5s ease-in-out infinite;
}
.skeleton-line--name { width: 80px; height: 14px; margin: 0 auto 6px; }
.skeleton-line--points { width: 60px; height: 14px; margin: 0 auto; }

.skeleton-table {
  margin-top: 8px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 8px 0;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
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
}

.skeleton-line--rank-sm { width: 24px; height: 18px; flex-shrink: 0; }
.skeleton-line--nickname { width: 120px; height: 14px; flex: 1; }
.skeleton-line--score { width: 60px; height: 14px; flex-shrink: 0; }

/* Table scroll wrapper for mobile overflow */
.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

/* ============================
   Mobile: < 768px
   ============================ */
@media (max-width: 767px) {
  .page-header {
    gap: 8px;
  }

  .page-header-right {
    width: 100%;
    justify-content: space-between;
  }

  .leaderboard-tab {
    padding: 4px 10px;
    font-size: 12px;
  }

  .page-title {
    font-size: 24px;
  }

  .podium {
    gap: 8px;
    padding: 16px 0;
    margin-bottom: 24px;
  }

  .podium-item {
    min-width: 0;
    padding: 16px 12px;
    flex: 1;
  }

  .podium-item.gold {
    min-width: 0;
    flex: 1.15;
    padding-bottom: 24px;
  }

  .podium-rank {
    font-size: 20px;
  }

  .podium-crown {
    font-size: 22px;
  }

  .podium-name {
    font-size: 13px;
  }

  .podium-points {
    font-size: 12px;
  }

  .podium-avatar :deep(.el-avatar) {
    width: 44px !important;
    height: 44px !important;
    line-height: 44px !important;
  }

  .gold .podium-avatar :deep(.el-avatar) {
    width: 52px !important;
    height: 52px !important;
    line-height: 52px !important;
  }

  .leaderboard-table :deep(.el-table__header th:first-child),
  .leaderboard-table :deep(.el-table__body td:first-child) {
    width: 50px !important;
    min-width: 50px !important;
  }

  .leaderboard :deep(.el-pagination) {
    padding: 12px 0;
  }
}

/* ============================
   Small mobile: < 480px
   ============================ */
@media (max-width: 479px) {
  .page-title {
    font-size: 20px;
  }

  .podium {
    gap: 6px;
    padding: 12px 0;
  }

  .podium-item {
    padding: 10px 8px;
  }

  .podium-item.gold {
    padding-bottom: 20px;
  }

  .podium-rank {
    font-size: 18px;
    margin-bottom: 4px;
  }

  .podium-crown {
    font-size: 18px;
  }

  .podium-name {
    font-size: 12px;
  }

  .podium-points {
    font-size: 11px;
  }

  .podium-avatar :deep(.el-avatar) {
    width: 36px !important;
    height: 36px !important;
    line-height: 36px !important;
  }

  .gold .podium-avatar :deep(.el-avatar) {
    width: 42px !important;
    height: 42px !important;
    line-height: 42px !important;
  }

  .leaderboard-table :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }
}

/* 胜率榜门槛提示 */
.leaderboard-notice {
  text-align: center;
  padding: 8px 12px;
  margin-bottom: 12px;
  background: rgba(91,140,255,0.06);
  border-radius: var(--radius-sm);
  font-size: 13px;
  color: var(--text-tertiary);
}

.leaderboard-notice strong {
  color: var(--accent-secondary);
}
</style>