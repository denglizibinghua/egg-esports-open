<template>
  <!-- Bet History -->
  <div class="bet-history">
    <div class="section-header">
      <h3 class="section-title">押注记录</h3>
      <el-radio-group v-model="betStatus" size="small" @change="fetchBets">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="0">待结算</el-radio-button>
        <el-radio-button value="1">猜中</el-radio-button>
        <el-radio-button value="2">未中</el-radio-button>
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
          <template #default="scope">
            <span class="bet-match-title">{{ scope.row.matchTitle || ('赛事 #' + scope.row.matchId) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="押注方" width="100" align="center">
          <template #default="scope">
            <el-tag size="small" :type="scope.row.betSide === 0 ? 'danger' : 'primary'">
              {{ getBetTeamName(scope.row) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="积分" width="80" align="center" prop="betAmount" />
        <el-table-column label="返还" width="80" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
            <span v-else class="returned-amount">{{ getReturnedAmount(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净收益" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
            <span v-else :class="netChangeClass(scope.row)">{{ formatNetChange(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
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
          <template #default="scope">
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
        v-model:current-page="betCurrentPage"
        @current-change="fetchBets"
      />
    </template>
  </div>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'

export default {
  name: 'BetHistoryTab',
  props: {
    // 已结算投注的 key 列表（matchId + '_' + betSide），用于高亮新结算行，由父组件传入
    settledBetKeys: { type: Array, default: () => [] }
  },
  data() {
    return {
      betsLoading: false,
      bets: [],
      betStatus: 'all',
      betCurrentPage: 1,
      betPageSize: 10,
      betTotal: 0
    }
  },
  created() {
    // 子组件自洽：挂载后自动加载第一页投注记录
    this.fetchBets()
  },
  methods: {
    // 拉取当前用户的投注记录（/bet/my 基于 token，无需 userId）
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
        // 兼容 records/list/content 多种后端返回格式
        this.bets = extractList(data)
        if (Array.isArray(data)) {
          this.betTotal = data.length
        } else if (data.records) {
          this.betTotal = data.total || data.records.length
        } else {
          this.betTotal = data.total || data.totalElements || 0
        }
      }.bind(this)).catch(function () {}).finally(function () {
        this.betsLoading = false
      }.bind(this))
    },
    // 行样式：命中"新结算"列表的行高亮
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
    getBetTeamName(bet) {
      // 优先用下注时快照队名，兜底用 A队/B队
      if (bet.teamASnapshot || bet.teamBSnapshot) {
        return bet.betSide === 0
          ? (bet.teamASnapshot || 'A队')
          : (bet.teamBSnapshot || 'B队')
      }
      // 无快照时用当前 match 队名
      if (bet.teamA && bet.teamB) {
        return bet.betSide === 0 ? bet.teamA : bet.teamB
      }
      return bet.betSide === 0 ? 'A队' : 'B队'
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
:deep(.el-table .new-settlement td) {
  background: rgba(0, 229, 160, 0.06) !important;
  box-shadow: inset 0 0 20px rgba(0, 229, 160, 0.05);
}

:deep(.el-table .new-settlement td:first-child) {
  position: relative;
}

:deep(.el-table .new-settlement td:first-child::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  background: var(--accent-primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
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

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}
</style>
