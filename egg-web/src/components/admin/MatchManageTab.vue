<template>
  <!-- 赛事列表模式：状态筛选 + 赛事表格 + 结算弹窗 -->
  <div v-if="mode === 'list'" class="match-manage-tab">
    <div class="admin-toolbar">
      <div class="toolbar-block">
        <span class="toolbar-kicker">FILTER / 状态</span>
        <el-radio-group v-model="matchStatus" size="small" class="status-filter" @change="fetchMatches">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="0">未开始</el-radio-button>
          <el-radio-button value="1">进行中</el-radio-button>
          <el-radio-button value="2">已结束</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-block toolbar-block--end">
        <el-input
          v-model="searchText"
          size="small"
          clearable
          placeholder="搜索 标题 / 战队 / 联赛"
          class="toolbar-search"
        />
        <span class="toolbar-count">TOTAL <b>{{ total }}</b></span>
        <el-button type="primary" size="small" :icon="PlusIcon" @click="$router.push('/admin/match/create')">添加赛事</el-button>
      </div>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="skeleton-table u-panel">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
      <div v-for="i in 5" :key="'admin-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="matches.length === 0" class="empty-state u-panel">
      <div class="empty-mark">//</div>
      <h3 class="empty-title">还没有赛事</h3>
      <p class="empty-desc">创建第一场比赛，开始你的竞猜平台</p>
      <el-button type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
    </div>

    <!-- Match Table -->
    <template v-else>
      <div class="table-shell">
        <el-table :data="filteredMatches" style="width: 100%" empty-text="没有匹配的赛事">
          <el-table-column label="ID" width="70">
            <template #default="scope">
              <span class="cell-id">#{{ scope.row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="对阵 / FIXTURE" min-width="240">
            <template #default="scope">
              <div class="fixture-cell">
                <div class="fixture-line">
                  <span class="fixture-team fixture-team--a" :title="scope.row.teamA">{{ scope.row.teamA || '待定' }}</span>
                  <span class="fixture-vs">VS</span>
                  <span class="fixture-team fixture-team--b" :title="scope.row.teamB">{{ scope.row.teamB || '待定' }}</span>
                </div>
                <div class="fixture-title" :title="scope.row.title">{{ scope.row.title || '—' }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="联赛" width="140">
            <template #default="scope">
              <span class="tournament-chip" :title="scope.row.tournament">{{ scope.row.tournament || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="时间 / TIME" width="150">
            <template #default="scope">
              <span class="cell-time">{{ formatDate(scope.row.matchTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="104" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 0" class="status-badge status-badge--upcoming">
                <span class="status-dot"></span>未开始
              </span>
              <span v-else-if="scope.row.status === 1" class="status-badge status-badge--live">
                <span class="status-dot status-dot--pulse"></span>进行中
              </span>
              <span v-else-if="scope.row.status === 2" class="status-badge status-badge--finished">已结束</span>
            </template>
          </el-table-column>
          <el-table-column label="结果" width="104" align="center">
            <template #default="scope">
              <template v-if="scope.row.status === 2">
                <span v-if="scope.row.result === 0" class="result-tag result-tag--a">A 胜</span>
                <span v-else-if="scope.row.result === 1" class="result-tag result-tag--b">B 胜</span>
                <span v-else-if="scope.row.result === 2" class="result-tag result-tag--draw">平局</span>
                <span v-else class="result-tag result-tag--pending">未结算</span>
              </template>
              <span v-else class="cell-dash">—</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" align="right">
            <template #default="scope">
              <div class="row-actions">
                <el-button link size="small" class="row-btn" @click="$router.push('/admin/match/' + scope.row.id)">编辑</el-button>
                <el-button
                  v-if="scope.row.status === 2 && (scope.row.result === null || scope.row.result === undefined)"
                  link
                  size="small"
                  class="row-btn row-btn--settle"
                  @click="openSettleDialog(scope.row)"
                >
                  结算
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
        v-if="total > 0"
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        v-model:current-page="currentPage"
        @current-change="fetchMatches"
      />
    </template>

    <!-- Settle Dialog -->
    <el-dialog title="赛事结算" v-model="settleDialogVisible" width="420px" custom-class="settle-dialog">
      <div class="settle-content">
        <div class="settle-kicker">SETTLE / 结果裁定</div>
        <p class="settle-match-name">{{ settleMatch ? settleMatch.teamA + ' VS ' + settleMatch.teamB : '' }}</p>
        <p class="settle-label">选择比赛结果</p>
        <el-radio-group v-model="settleResult" class="settle-options">
          <el-radio :value="0" class="settle-option">{{ settleMatch ? settleMatch.teamA : 'A队' }} 获胜</el-radio>
          <el-radio :value="1" class="settle-option">{{ settleMatch ? settleMatch.teamB : 'B队' }} 获胜</el-radio>
          <el-radio :value="2" class="settle-option">平局</el-radio>
        </el-radio-group>
        <p class="settle-warn">结算后将按结果派奖，且不可撤销。</p>
      </div>
      <template #footer>
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="settleLoading" @click="handleSettle">确认结算</el-button>
      </template>
    </el-dialog>
  </div>

  <!-- 批量导入模式：JSON 解析 + 预览 + 提交 -->
  <div v-else-if="mode === 'import'" class="import-panel">
    <div class="import-instructions">
      <div class="import-kicker">IMPORT / JSON SCHEMA</div>
      <h4 class="import-section-title">JSON 数据格式</h4>
      <p class="import-hint">粘贴 JSON 数组，每个对象需包含：title, teamA, teamB, matchTime。可选字段：gameId, teamALogo, teamBLogo, tournament, status</p>
    </div>

    <el-input
      v-model="importJsonText"
      type="textarea"
      :rows="8"
      placeholder='[{"title":"LPL春季赛决赛","teamA":"TES","teamB":"JDG","matchTime":"2026-06-15 18:00:00","tournament":"LPL","status":0}]'
      class="import-textarea"
    />

    <div class="import-actions">
      <el-button type="primary" size="small" :disabled="!importJsonText.trim()" @click="parseImportJson">解析预览</el-button>
      <el-button size="small" @click="clearImport">清空</el-button>
    </div>

    <div v-if="importParseError" class="import-parse-error">
      <el-alert :title="importParseError" type="error" :closable="false" show-icon />
    </div>

    <template v-if="importParsedList.length > 0">
      <div class="import-preview-header">
        <span class="import-preview-count">已解析 {{ importParsedList.length }} 条赛事</span>
        <span class="import-preview-hint">校验通过 · 待提交</span>
      </div>

      <div class="table-shell">
        <el-table :data="importParsedList" style="width: 100%" max-height="360" size="small">
          <el-table-column label="序号" width="60" align="center">
            <template #default="scope">{{ scope.$index + 1 }}</template>
          </el-table-column>
          <el-table-column label="标题" min-width="150" prop="title" />
          <el-table-column label="A队" width="120" prop="teamA" />
          <el-table-column label="B队" width="120" prop="teamB" />
          <el-table-column label="联赛" width="120" prop="tournament">
            <template #default="scope">{{ scope.row.tournament || '-' }}</template>
          </el-table-column>
          <el-table-column label="比赛时间" width="160">
            <template #default="scope"><span class="cell-time">{{ formatDate(scope.row.matchTime) }}</span></template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <span :class="['status-badge', statusBadgeClass(scope.row.status)]">
                <span class="status-dot"></span>{{ statusLabel(scope.row.status) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="import-submit-row">
        <el-button type="primary" :loading="importSubmitting" @click="submitBatchImport">确认导入 {{ importParsedList.length }} 条</el-button>
      </div>
    </template>

    <div v-if="importResult" class="import-result">
      <el-alert
        :title="'成功导入 ' + importResult.success + ' 条，失败 ' + importResult.fail + ' 条'"
        :type="importResult.fail > 0 ? 'warning' : 'success'"
        :closable="false"
        show-icon
      />
      <div v-if="importResult.errors && importResult.errors.length > 0" class="import-errors">
        <div class="import-errors-toggle" @click="importErrorsExpanded = !importErrorsExpanded">
          <el-icon><component :is="importErrorsExpanded ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
          <span>查看失败详情 ({{ importResult.errors.length }} 条)</span>
        </div>
        <div v-if="importErrorsExpanded" class="import-errors-list">
          <div v-for="(err, idx) in importResult.errors" :key="'err-'+idx" class="import-error-item">
            <span class="import-error-index">#{{ err.index + 1 }}</span>
            <span class="import-error-title">{{ err.title || '未命名' }}</span>
            <span class="import-error-msg">{{ err.error }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'MatchManageTab',
  props: {
    // 模式：'list' 赛事列表 / 'import' 批量导入
    mode: {
      type: String,
      default: 'list'
    }
  },
  emits: ['import-success'],
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      PlusIcon: markRaw(Plus),
      // ===== list 模式状态 =====
      matches: [],
      loading: false,
      matchStatus: 'all',
      // 前端搜索关键字：仅过滤当前页已加载数据，不改变请求参数
      searchText: '',
      currentPage: 1,
      pageSize: 20,
      total: 0,
      settleDialogVisible: false,
      settleMatch: null,
      settleResult: null,
      settleLoading: false,
      // ===== import 模式状态 =====
      importJsonText: '',
      importParsedList: [],
      importParseError: '',
      importSubmitting: false,
      importResult: null,
      importErrorsExpanded: false
    }
  },
  computed: {
    // 本地过滤：标题 / A队 / B队 / 联赛
    filteredMatches() {
      var q = (this.searchText || '').trim().toLowerCase()
      if (!q) return this.matches
      return this.matches.filter(function (m) {
        return [m.title, m.teamA, m.teamB, m.tournament].some(function (v) {
          return v && String(v).toLowerCase().indexOf(q) !== -1
        })
      })
    }
  },
  created() {
    // 仅 list 模式挂载时拉取赛事列表（import 模式无需初始化数据）
    if (this.mode === 'list') {
      this.fetchMatches()
    }
  },
  methods: {
    // 暴露给父组件：导入成功后刷新赛事列表
    refresh() {
      if (this.mode === 'list') {
        this.fetchMatches()
      }
    },
    fetchMatches() {
      this.loading = true
      const params = {
        page: this.currentPage,
        size: this.pageSize
      }
      if (this.matchStatus !== 'all') {
        params.status = this.matchStatus
      }
      request.get('/match/list', { params }).then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.matches = extractList(data)
        if (Array.isArray(data)) {
          this.total = data.length
        } else if (data.records) {
          this.total = data.total || data.records.length
        } else {
          this.total = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.loading = false
      })
    },
    openSettleDialog(match) {
      this.settleMatch = match
      this.settleResult = null
      this.settleDialogVisible = true
    },
    handleSettle() {
      if (this.settleResult === null) {
        this.$message.warning('请选择比赛结果')
        return
      }
      this.settleLoading = true
      request.put('/match/settle', {
        matchId: this.settleMatch.id,
        result: this.settleResult
      }).then(() => {
        this.$message.success('结算成功')
        this.settleDialogVisible = false
        this.fetchMatches()
      }).catch(() => {}).finally(() => {
        this.settleLoading = false
      })
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
    },
    parseImportJson() {
      this.importParseError = ''
      this.importParsedList = []
      this.importResult = null
      this.importErrorsExpanded = false
      var text = this.importJsonText.trim()
      if (!text) {
        this.importParseError = '请输入 JSON 数据'
        return
      }
      var parsed
      try {
        parsed = JSON.parse(text)
      } catch (e) {
        this.importParseError = 'JSON 格式错误：' + e.message
        return
      }
      if (!Array.isArray(parsed)) {
        this.importParseError = 'JSON 必须是一个数组'
        return
      }
      if (parsed.length === 0) {
        this.importParseError = '数组不能为空'
        return
      }
      var required = ['title', 'teamA', 'teamB', 'matchTime']
      var errors = []
      for (var i = 0; i < parsed.length; i++) {
        var item = parsed[i]
        for (var j = 0; j < required.length; j++) {
          if (!item[required[j]]) {
            errors.push('第 ' + (i + 1) + ' 条缺少必填字段：' + required[j])
          }
        }
      }
      if (errors.length > 0) {
        this.importParseError = errors.join('；')
        return
      }
      this.importParsedList = parsed
    },
    clearImport() {
      this.importJsonText = ''
      this.importParsedList = []
      this.importParseError = ''
      this.importResult = null
      this.importErrorsExpanded = false
    },
    submitBatchImport() {
      if (this.importParsedList.length === 0) return
      this.importSubmitting = true
      this.importResult = null
      this.importErrorsExpanded = false
      request.post('/match/batch-import', this.importParsedList).then(res => {
        var data = res.data || res
        this.importResult = data
        // 导入有成功项时通知父组件刷新赛事列表（list 模式实例）
        if (data.success > 0) {
          this.$emit('import-success')
        }
      }).catch(err => {
        var msg = (err && err.response && err.response.data && err.response.data.message) || '导入请求失败'
        this.importResult = { success: 0, fail: this.importParsedList.length, total: this.importParsedList.length, errors: [{ index: 0, title: '-', error: msg }] }
      }).finally(() => {
        this.importSubmitting = false
      })
    },
    statusLabel(status) {
      if (status === 0) return '未开始'
      if (status === 1) return '进行中'
      if (status === 2) return '已结束'
      return '未开始'
    },
    statusBadgeClass(status) {
      if (status === 0) return 'status-badge--upcoming'
      if (status === 1) return 'status-badge--live'
      if (status === 2) return 'status-badge--finished'
      return 'status-badge--upcoming'
    }
  }
}
</script>

<style scoped>
.match-manage-tab,
.import-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- Toolbar ---------- */
.admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 14px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.018) 0%, transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.toolbar-block {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.toolbar-block--end {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.toolbar-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.status-filter {
  flex-wrap: wrap;
}

.status-filter :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 30px;
  padding: 0 12px;
  background: var(--bg-inset);
  border-color: var(--border-default);
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  box-shadow: none;
  transition: all var(--duration-fast) ease;
}

.status-filter :deep(.el-radio-button__inner:hover) {
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.status-filter :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: var(--brand);
  border-color: var(--brand);
  color: var(--text-primary);
  box-shadow: 0 0 0 1px var(--brand-line);
}

.toolbar-search {
  width: 220px;
}

.toolbar-search :deep(.el-input__wrapper) {
  min-height: 30px;
  height: 30px;
}

.toolbar-search :deep(.el-input__inner) {
  font-family: var(--font-mono);
  font-size: 11.5px;
  letter-spacing: 0.02em;
}

.toolbar-count {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.toolbar-count b {
  color: var(--text-primary);
  font-weight: 700;
}

.match-manage-tab :deep(.el-pagination) {
  margin-top: 0;
}

/* ---------- Table shell ---------- */
.table-shell {
  max-width: 100%;
  min-width: 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  background: var(--bg-surface-1);
}

.table-shell :deep(.el-table) {
  min-width: 900px;
  background: transparent;
}

.table-shell :deep(.el-table th.el-table__cell:first-child .cell) {
  padding-left: 18px;
}

.table-shell :deep(.el-table td.el-table__cell:first-child .cell) {
  padding-left: 18px;
}

.cell-id {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: var(--text-faint);
}

.cell-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11.5px;
  letter-spacing: 0.01em;
  color: var(--text-secondary);
  white-space: nowrap;
}

.cell-dash {
  font-family: var(--font-mono);
  color: var(--text-faint);
}

/* Matchup cell */
.fixture-cell {
  min-width: 0;
}

.fixture-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.fixture-team {
  max-width: 42%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.01em;
}

.fixture-team--a {
  color: var(--text-primary);
}

.fixture-team--b {
  color: var(--text-primary);
}

.fixture-vs {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--text-faint);
}

.fixture-title {
  margin-top: 2px;
  font-size: 11px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Result tags */
.result-tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 7px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  white-space: nowrap;
}

.result-tag--a {
  color: var(--brand);
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.result-tag--b {
  color: var(--accent-secondary);
  background: var(--bg-surface-2);
}

.result-tag--draw {
  color: var(--accent-warning);
  background: var(--bg-surface-2);
}

.result-tag--pending {
  color: var(--accent-warning);
  border-color: var(--border-default);
  background: var(--bg-surface-2);
}

/* Row actions */
.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.row-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.row-actions :deep(.el-button.row-btn) {
  height: 26px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) ease;
}

.row-actions :deep(.el-button.row-btn:hover) {
  color: var(--text-primary) !important;
  background: var(--bg-surface-2) !important;
}

.row-actions :deep(.el-button.row-btn--settle) {
  color: var(--accent-primary);
  border: 1px solid var(--border-default) !important;
}

.row-actions :deep(.el-button.row-btn--settle:hover) {
  color: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  background: var(--accent-primary-soft) !important;
}

/* ---------- Settle dialog ---------- */
.settle-content {
  text-align: left;
}

.settle-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.settle-match-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.35;
  margin-bottom: 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--border-subtle);
}

.settle-label {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  margin-bottom: 10px;
}

.settle-options {
  display: grid;
  gap: 8px;
  width: 100%;
}

.settle-option {
  display: flex;
  align-items: center;
  width: 100%;
  height: auto;
  margin: 0;
  padding: 9px 12px;
  white-space: normal;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: var(--bg-inset);
  transition: border-color var(--duration-fast) ease, background var(--duration-fast) ease;
}

.settle-option:hover {
  border-color: var(--border-strong);
}

.settle-option.is-checked {
  border-color: var(--brand-line);
  background: var(--brand-soft);
}

.settle-option :deep(.el-radio__label) {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
  padding-left: 8px;
  white-space: normal;
  word-break: break-word;
}

.settle-option :deep(.el-radio__inner) {
  border-color: var(--border-strong);
  background: transparent;
}

.settle-option :deep(.el-radio__input.is-checked .el-radio__inner) {
  border-color: var(--brand);
  background: var(--brand);
}

.settle-warn {
  margin-top: 14px;
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

/* ---------- Empty state ---------- */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 24px;
  text-align: center;
  border-style: dashed;
  background: transparent;
}

.empty-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
  font-family: var(--font-mono);
  font-size: 20px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
}

.empty-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-bottom: 20px;
  max-width: 300px;
}

/* ---------- Skeleton ---------- */

.skeleton-table-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--border-default);
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.skeleton-table-row:last-child {
  border-bottom: none;
}

.skeleton-line {
  height: 12px;
  border-radius: var(--radius-sm);
}

.skeleton-line--cell-sm { width: 50px; flex-shrink: 0; }
.skeleton-line--cell-md { width: 90px; flex-shrink: 0; }
.skeleton-line--cell-lg { flex: 1; min-width: 100px; }

/* ---------- Import panel ---------- */
.import-instructions {
  padding: 16px 18px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--brand);
  border-radius: var(--radius-md);
}

.import-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.import-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 0 0 6px 0;
}

.import-hint {
  font-size: 12.5px;
  color: var(--text-tertiary);
  margin: 0;
  line-height: 1.65;
}

.import-textarea :deep(.el-textarea__inner) {
  font-family: var(--font-mono);
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-primary);
  background: var(--bg-inset);
  border-radius: var(--radius-sm);
  box-shadow: 0 0 0 1px var(--border-default) inset;
  resize: vertical;
}

.import-textarea :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--brand) inset, 0 0 0 3px var(--brand-soft);
}

.import-actions {
  display: flex;
  gap: 8px;
  margin-top: -4px;
}

.import-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding: 10px 14px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.import-preview-count {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--accent-primary);
}

.import-preview-hint {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.import-submit-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
}

.import-result {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 4px;
}

.import-errors {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.import-errors-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 12.5px;
  transition: color var(--duration-fast) ease;
  user-select: none;
}

.import-errors-toggle:hover {
  color: var(--text-primary);
}

.import-errors-list {
  border-top: 1px solid var(--border-subtle);
}

.import-error-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 14px;
  font-size: 12.5px;
  border-bottom: 1px solid var(--border-subtle);
}

.import-error-item:last-child {
  border-bottom: none;
}

.import-error-index {
  min-width: 32px;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-faint);
}

.import-error-title {
  min-width: 100px;
  color: var(--text-primary);
}

.import-error-msg {
  flex: 1;
  color: var(--accent-danger);
  font-family: var(--font-mono);
  font-size: 11.5px;
  word-break: break-word;
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .admin-toolbar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .toolbar-block {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .toolbar-block--end {
    justify-content: flex-start;
  }

  .toolbar-search {
    flex: 1;
    min-width: 160px;
    width: auto;
  }
}

@media (max-width: 768px) {
  .toolbar-block {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .toolbar-block--end {
    align-items: stretch;
  }

  .toolbar-search {
    width: 100%;
  }

  .toolbar-block--end :deep(.el-button) {
    width: 100%;
  }

  .table-shell :deep(.el-table) {
    min-width: 860px;
  }

  .row-actions :deep(.el-button.row-btn) {
    min-height: 30px;
  }

  .import-error-item {
    align-items: flex-start;
    flex-direction: column;
    gap: 3px;
  }

  .import-error-title {
    min-width: 0;
  }

  .import-submit-row :deep(.el-button) {
    width: 100%;
    min-height: 42px;
  }
}

@media (max-width: 480px) {
  .toolbar-count {
    display: none;
  }

  .import-preview-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 4px;
  }
}
</style>
