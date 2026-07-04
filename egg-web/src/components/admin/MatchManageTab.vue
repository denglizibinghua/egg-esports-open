<template>
  <!-- 赛事列表模式：状态筛选 + 赛事表格 + 结算弹窗 -->
  <div v-if="mode === 'list'" class="match-manage-tab">
    <div class="tab-header">
      <el-radio-group v-model="matchStatus" size="small" @change="fetchMatches">
        <el-radio-button value="all">全部</el-radio-button>
        <el-radio-button value="0">未开始</el-radio-button>
        <el-radio-button value="1">进行中</el-radio-button>
        <el-radio-button value="2">已结束</el-radio-button>
      </el-radio-group>
      <el-button type="primary" size="small" :icon="PlusIcon" @click="$router.push('/admin/match/create')">添加赛事</el-button>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="loading" class="skeleton-table">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
      <div v-for="i in 5" :key="'admin-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="matches.length === 0" class="empty-state">
      <div class="empty-icon">📝</div>
      <h3 class="empty-title">还没有赛事</h3>
      <p class="empty-desc">创建第一场比赛，开始你的竞猜平台</p>
      <el-button type="primary" class="empty-action" @click="$router.push('/admin/match/create')">创建赛事</el-button>
    </div>

    <!-- Match Table -->
    <template v-else>
      <el-table :data="matches" style="width: 100%">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="标题" min-width="150" prop="title" />
        <el-table-column label="A队" width="120" prop="teamA" />
        <el-table-column label="B队" width="120" prop="teamB" />
        <el-table-column label="联赛" width="120" prop="tournament" />
        <el-table-column label="时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.matchTime) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
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
        <el-table-column label="结果" width="100" align="center">
          <template #default="scope">
            <template v-if="scope.row.status === 2">
              <template v-if="scope.row.result === 0">A胜</template>
              <template v-else-if="scope.row.result === 1">B胜</template>
              <template v-else-if="scope.row.result === 2">平局</template>
              <template v-else>未结算</template>
            </template>
            <template v-else>-</template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="scope">
            <el-button link size="small" @click="$router.push('/admin/match/' + scope.row.id)">编辑</el-button>
            <el-button
              v-if="scope.row.status === 2 && (scope.row.result === null || scope.row.result === undefined)"
              link
              size="small"
              class="settle-btn"
              @click="openSettleDialog(scope.row)"
            >
              结算
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <el-pagination
      v-if="total > 0"
      background
      layout="prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="currentPage"
      @current-change="fetchMatches"
    />

    <!-- Settle Dialog -->
    <el-dialog title="赛事结算" v-model="settleDialogVisible" width="400px" custom-class="settle-dialog">
      <div class="settle-content">
        <p class="settle-match-name">{{ settleMatch ? settleMatch.teamA + ' VS ' + settleMatch.teamB : '' }}</p>
        <p class="settle-label">选择比赛结果：</p>
        <el-radio-group v-model="settleResult">
          <el-radio :value="0">{{ settleMatch ? settleMatch.teamA : 'A队' }} 获胜</el-radio>
          <el-radio :value="1">{{ settleMatch ? settleMatch.teamB : 'B队' }} 获胜</el-radio>
          <el-radio :value="2">平局</el-radio>
        </el-radio-group>
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
      </div>

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
          <template #default="scope">{{ formatDate(scope.row.matchTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <span :class="['status-badge', statusBadgeClass(scope.row.status)]">
              <span class="status-dot"></span>{{ statusLabel(scope.row.status) }}
            </span>
          </template>
        </el-table-column>
      </el-table>

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
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.settle-btn {
  color: var(--accent-primary) !important;
  font-weight: 600;
}

.settle-content {
  text-align: center;
}

.settle-match-name {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 16px;
}

.settle-label {
  color: var(--text-secondary);
  margin-bottom: 12px;
}

.settle-content .el-radio {
  display: block;
  margin-bottom: 12px;
  color: var(--text-primary);
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

/* Skeleton Loading */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.skeleton-table {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.skeleton-table-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 2px solid var(--border-subtle);
}

.skeleton-table-header .skeleton-line {
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
  height: 14px;
}

.skeleton-table-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
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

.skeleton-line--cell-sm { width: 50px; flex-shrink: 0; }
.skeleton-line--cell-md { width: 90px; flex-shrink: 0; }
.skeleton-line--cell-lg { flex: 1; min-width: 100px; }

/* Import Panel */
.import-panel {
  padding: 4px 0;
}

.import-instructions {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.import-section-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.import-hint {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 0;
  line-height: 1.6;
}

.import-textarea {
  margin-bottom: 12px;
}

.import-textarea :deep(.el-textarea__inner) {
  background: var(--bg-surface-1);
  border-color: var(--border-subtle);
  color: var(--text-primary);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  border-radius: var(--radius-md);
  resize: vertical;
}

.import-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--accent-primary);
}

.import-textarea :deep(.el-textarea__inner::placeholder) {
  color: var(--text-tertiary);
  opacity: 0.6;
}

.import-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.import-actions .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-sm) !important;
}

.import-parse-error {
  margin-bottom: 16px;
}

.import-preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 10px 16px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.import-preview-count {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent-primary);
}

.import-submit-row {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.import-submit-row .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 24px;
}

.import-result {
  margin-top: 20px;
}

.import-errors {
  margin-top: 12px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.import-errors-toggle {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  cursor: pointer;
  color: var(--text-secondary);
  font-size: 13px;
  transition: color 0.2s;
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
  padding: 8px 16px;
  font-size: 13px;
  border-bottom: 1px solid var(--border-subtle);
}

.import-error-item:last-child {
  border-bottom: none;
}

.import-error-index {
  color: var(--text-tertiary);
  font-weight: 600;
  min-width: 32px;
}

.import-error-title {
  color: var(--text-primary);
  min-width: 100px;
}

.import-error-msg {
  color: #e06c75;
  flex: 1;
}

/* ===== 移动端基础适配 ===== */
@media (max-width: 768px) {
  /* tab 头部：筛选与操作按钮改为纵向堆叠，避免横向溢出 */
  .tab-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;
  }
  .tab-header .el-button {
    width: 100%;
  }

  /* 表格容器保证可横向滚动 */
  :deep(.el-table) {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
  }
  /* 操作列按钮 touch-friendly */
  :deep(.el-table .el-button) {
    min-height: 32px;
    padding: 4px 8px;
  }

  /* 导入错误项：窄屏堆叠 */
  .import-error-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }
  .import-error-title {
    min-width: 0;
  }
  .import-submit-row {
    justify-content: stretch;
  }
  .import-submit-row .el-button--primary {
    width: 100%;
    min-height: 44px;
  }
}
</style>
