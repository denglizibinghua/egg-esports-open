<template>
  <!-- Bet History -->
  <div class="bet-history u-panel card-entrance">
    <div class="section-header">
      <div class="section-head-main">
        <span class="section-index">01</span>
        <div class="section-head-text">
          <h3 class="section-title">押注记录</h3>
          <span class="section-meta">共 {{ betTotal }} 条记录</span>
        </div>
      </div>
      <el-radio-group v-model="betStatus" size="small" @change="fetchBets" class="status-filter">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="0">待结算</el-radio-button>
        <el-radio-button value="1">猜中</el-radio-button>
        <el-radio-button value="2">未中</el-radio-button>
      </el-radio-group>
    </div>

    <!-- Bet History Empty State -->
    <div v-if="!betsLoading && bets.length === 0" class="empty-state">
      <span class="empty-mark">//</span>
      <h3 class="empty-title">还没有押注记录</h3>
      <p class="empty-desc">去赛事列表看看，选一场比赛试试你的判断力</p>
      <el-button type="primary" class="empty-cta" @click="$router.push('/matches')">去赛事列表</el-button>
    </div>

    <template v-else>
      <div class="table-scroll">
        <el-table class="bet-table" :data="bets" style="width: 100%" v-loading="betsLoading" :row-class-name="betRowClass">
          <el-table-column label="赛事" min-width="200">
            <template #default="scope">
              <span class="bet-match-title" :title="scope.row.matchTitle || ('赛事 #' + scope.row.matchId)">
                {{ scope.row.matchTitle || ('赛事 #' + scope.row.matchId) }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="押注方" width="140" align="center">
            <template #default="scope">
              <span class="side-chip" :class="scope.row.betSide === 0 ? 'side-chip--a' : 'side-chip--b'">
                <span class="side-letter">{{ scope.row.betSide === 0 ? 'A' : 'B' }}</span>
                <span class="side-name">{{ getBetTeamName(scope.row) }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column label="积分" width="80" align="center">
            <template #default="scope">
              <span class="cell-num cell-stake">{{ scope.row.betAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="返还" width="80" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
              <span v-else class="returned-amount cell-num">{{ getReturnedAmount(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="净收益" width="96" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 0" class="net-change--pending">-</span>
              <span v-else :class="netChangeClass(scope.row)">{{ formatNetChange(scope.row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="96" align="center">
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
          <el-table-column label="时间" width="150" align="center">
            <template #default="scope">
              <span class="bet-time">{{ formatDate(scope.row.createTime) }}</span>
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
/* ---------- Panel ---------- */
.bet-history {
  padding: 22px 22px 6px;
}

/* ---------- Section header ---------- */
.section-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 6px;
}

.section-head-main {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
}

.section-index {
  padding-top: 3px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--brand);
}

.section-head-text {
  min-width: 0;
}

.section-title {
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.3;
  color: var(--text-primary);
}

.section-meta {
  display: block;
  margin-top: 2px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.1em;
  color: var(--text-faint);
}

/* ---------- Segmented filter ---------- */
.status-filter {
  flex-shrink: 0;
}

.status-filter :deep(.el-radio-button__inner) {
  height: 30px;
  padding: 0 13px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border-color: var(--border-default);
  box-shadow: none;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  transition: all var(--duration-fast) ease;
}

.status-filter :deep(.el-radio-button:first-child .el-radio-button__inner) {
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
}

.status-filter :deep(.el-radio-button:last-child .el-radio-button__inner) {
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.status-filter :deep(.el-radio-button__inner:hover) {
  color: var(--text-primary);
  background: var(--bg-surface-2);
}

.status-filter :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--brand-soft);
  border-color: var(--brand-line);
  color: var(--brand);
  box-shadow: none;
}

/* ---------- Table ---------- */
.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.bet-table {
  min-width: 860px;
}

.bet-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.bet-match-title {
  display: block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}

.side-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 100%;
  height: 22px;
  padding: 0 7px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-surface-2);
  font-family: var(--font-mono);
  font-size: 10px;
  line-height: 1;
}

.side-chip--a {
  color: var(--brand);
  background: var(--brand-soft);
  border-color: var(--brand-line);
}

.side-chip--b {
  color: var(--accent-secondary);
}

.side-letter {
  font-size: 10px;
  font-weight: 800;
}

.side-name {
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-body);
  font-size: 11px;
  font-weight: 600;
  color: currentColor;
}

.cell-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 12.5px;
}

.cell-stake {
  color: var(--text-primary);
  font-weight: 700;
}

.bet-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11.5px;
  letter-spacing: 0.02em;
  color: var(--text-tertiary);
}

/* Net Change Colors */
.net-change--positive {
  color: var(--accent-primary);
  font-weight: 700;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--negative {
  color: var(--accent-danger);
  font-weight: 700;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--neutral {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

.net-change--pending {
  color: var(--text-faint);
  font-family: var(--font-mono);
}

.returned-amount {
  color: var(--text-secondary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
}

/* New Settlement Row Highlight */
.bet-table :deep(.el-table__body tr.new-settlement > td.el-table__cell) {
  background: var(--accent-primary-soft) !important;
}

.bet-table :deep(.el-table__body tr.new-settlement:hover > td.el-table__cell) {
  background: var(--accent-primary-soft) !important;
}

.bet-table :deep(.el-table__body tr.new-settlement > td.el-table__cell:first-child) {
  box-shadow: inset 2px 0 0 var(--accent-primary);
}

/* ---------- Empty State ---------- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin: 18px 0 20px;
  padding: 52px 24px;
  text-align: center;
  background: var(--bg-inset);
  border: 1px dashed var(--border-default);
  border-radius: var(--radius-lg);
}

.empty-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-md);
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  max-width: 320px;
  margin-bottom: 22px;
  font-size: 13px;
  color: var(--text-tertiary);
}

.empty-cta {
  font-weight: 700;
  letter-spacing: 0.04em;
}

/* ---------- Pagination ---------- */
.bet-history :deep(.el-pagination) {
  margin: 18px 0 14px;
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .bet-history {
    padding: 16px 14px 4px;
  }

  .section-header {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }

  .status-filter {
    display: flex;
    width: 100%;
  }

  .status-filter :deep(.el-radio-button) {
    flex: 1;
  }

  .status-filter :deep(.el-radio-button__inner) {
    width: 100%;
    padding: 0 6px;
  }

  .empty-state {
    padding: 44px 18px;
    margin: 14px 0 16px;
  }
}

@media (max-width: 420px) {
  .section-title {
    font-size: 17px;
  }

  .bet-history :deep(.el-pagination) {
    margin: 14px 0 12px;
  }
}
</style>
