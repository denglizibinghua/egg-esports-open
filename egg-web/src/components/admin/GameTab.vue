<template>
  <div class="game-tab">
    <div class="admin-toolbar">
      <div class="toolbar-block">
        <span class="toolbar-kicker">CATALOG / 游戏</span>
        <span class="toolbar-title">游戏列表</span>
        <span class="toolbar-count">TOTAL <b>{{ games.length }}</b></span>
      </div>
      <div class="toolbar-block toolbar-block--end">
        <el-button type="primary" size="small" :icon="PlusIcon" @click="openGameDialog()">添加游戏</el-button>
      </div>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="gameLoading" class="skeleton-table u-panel">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
      <div v-for="i in 6" :key="'game-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="games.length === 0" class="empty-state u-panel">
      <div class="empty-mark">//</div>
      <h3 class="empty-title">还没有游戏</h3>
      <p class="empty-desc">添加游戏，让赛事按游戏分类管理</p>
      <el-button type="primary" class="empty-action" @click="openGameDialog()">添加游戏</el-button>
    </div>

    <!-- Game Table -->
    <template v-else>
      <div class="table-shell">
        <el-table :data="games" style="width: 100%">
          <el-table-column label="ID" width="64">
            <template #default="scope">
              <span class="cell-id">#{{ scope.row.id }}</span>
            </template>
          </el-table-column>
          <el-table-column label="游戏 / GAME" min-width="220">
            <template #default="scope">
              <div class="game-cell">
                <span class="game-plate">
                  <el-image
                    v-if="scope.row.icon"
                    :src="scope.row.icon"
                    fit="cover"
                    class="game-plate-img"
                  />
                  <span v-else class="game-plate-fallback">{{ (scope.row.name || '?').slice(0, 1) }}</span>
                </span>
                <span class="game-name" :title="scope.row.name">{{ scope.row.name }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="排序" width="90" align="center">
            <template #default="scope">
              <span class="cell-order">{{ scope.row.sortOrder != null ? scope.row.sortOrder : '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 1" class="status-badge status-badge--won">
                <span class="status-dot"></span>启用
              </span>
              <span v-else class="status-badge status-badge--finished">
                <span class="status-dot"></span>停用
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" align="right">
            <template #default="scope">
              <div class="row-actions">
                <el-button link size="small" class="row-btn" @click="openGameDialog(scope.row)">编辑</el-button>
                <el-button link size="small" class="row-btn row-btn--danger" @click="handleDeleteGame(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <!-- Game Dialog -->
    <el-dialog
      :title="gameForm.id ? '编辑游戏' : '添加游戏'"
      v-model="gameDialogVisible"
      width="480px"
    >
      <div class="editor-head">
        <span class="editor-kicker">GAME / EDITOR</span>
      </div>
      <el-form :model="gameForm" label-width="80px" label-position="top" class="game-form">
        <el-form-item label="名称">
          <el-input v-model="gameForm.name" placeholder="例如: VALORANT" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="gameForm.icon" placeholder="图标地址（可选）" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="排序">
            <el-input-number v-model="gameForm.sortOrder" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="状态">
            <el-switch
              v-model="gameForm.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="gameDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="gameSubmitting" @click="handleSaveGame">
          {{ gameForm.id ? '保存修改' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { Plus } from '@element-plus/icons-vue'

export default {
  name: 'GameTab',
  emits: ['games-changed'],
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      PlusIcon: markRaw(Plus),
      games: [],
      gameLoading: false,
      gameDialogVisible: false,
      gameSubmitting: false,
      gameForm: {
        id: null,
        name: '',
        icon: '',
        sortOrder: 0,
        status: 1
      }
    }
  },
  created() {
    this.fetchGames()
  },
  methods: {
    fetchGames() {
      this.gameLoading = true
      request.get('/game/list').then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.games = extractList(data)
      }).catch(() => {
        this.games = []
      }).finally(() => {
        this.gameLoading = false
      })
    },
    openGameDialog(row) {
      if (row) {
        this.gameForm = {
          id: row.id,
          name: row.name || '',
          icon: row.icon || '',
          sortOrder: row.sortOrder != null ? row.sortOrder : 0,
          status: row.status != null ? row.status : 1
        }
      } else {
        this.gameForm = {
          id: null,
          name: '',
          icon: '',
          sortOrder: 0,
          status: 1
        }
      }
      this.gameDialogVisible = true
    },
    handleSaveGame() {
      if (!this.gameForm.name || !this.gameForm.name.trim()) {
        this.$message.warning('请输入游戏名称')
        return
      }
      this.gameSubmitting = true
      var payload = {
        name: this.gameForm.name,
        icon: this.gameForm.icon || null,
        sortOrder: this.gameForm.sortOrder != null ? this.gameForm.sortOrder : 0,
        status: this.gameForm.status
      }
      var promise
      if (this.gameForm.id) {
        promise = request.put('/game/' + this.gameForm.id, payload)
      } else {
        promise = request.post('/game', payload)
      }
      promise.then(() => {
        this.$message.success('保存成功')
        this.gameDialogVisible = false
        this.fetchGames()
        // 通知父组件游戏列表已变更，以便 AnnouncementTab 刷新关联游戏下拉
        this.$emit('games-changed')
      }).catch(() => {}).finally(() => {
        this.gameSubmitting = false
      })
    },
    handleDeleteGame(row) {
      var tip = row.status === 1 ? '游戏当前为启用状态，' : ''
      this.$confirm(tip + '确定删除游戏「' + row.name + '」？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonType: 'danger'
      }).then(() => {
        request.delete('/game/' + row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchGames()
          // 通知父组件游戏列表已变更，以便 AnnouncementTab 刷新关联游戏下拉
          this.$emit('games-changed')
        }).catch(() => {})
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.game-tab {
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
  flex-shrink: 0;
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

.toolbar-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  white-space: nowrap;
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

/* ---------- Table ---------- */
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
  min-width: 720px;
  background: transparent;
}

.table-shell :deep(.el-table th.el-table__cell:first-child .cell),
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

.cell-order {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 12px;
  color: var(--text-secondary);
}

.game-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.game-plate {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  overflow: hidden;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
}

.game-plate-img {
  width: 100%;
  height: 100%;
}

.game-plate-img :deep(.el-image__inner) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.game-plate-fallback {
  font-family: var(--font-mono);
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.02em;
  color: var(--text-tertiary);
}

.game-name {
  min-width: 0;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- Row actions ---------- */
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

.row-actions :deep(.el-button.row-btn--danger) {
  color: var(--accent-danger);
}

.row-actions :deep(.el-button.row-btn--danger:hover) {
  color: var(--accent-danger) !important;
  background: var(--el-color-danger-light-9) !important;
}

/* ---------- Empty ---------- */
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

/* ---------- Editor dialog ---------- */
.editor-head {
  margin: -4px 0 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}

.editor-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--brand);
}

.game-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.game-form :deep(.el-form-item__label) {
  padding-bottom: 5px !important;
  font-size: 11px !important;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--text-tertiary) !important;
  line-height: 1.4;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.game-form :deep(.el-input-number) {
  width: 160px;
}

.game-form :deep(.el-switch.is-checked .el-switch__core) {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
}

.game-form :deep(.el-switch__label) {
  color: var(--text-tertiary);
  font-size: 12px;
}

.game-form :deep(.el-switch__label.is-active) {
  color: var(--accent-primary);
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .admin-toolbar {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
  }

  .toolbar-block {
    justify-content: space-between;
    flex-wrap: wrap;
  }

  .toolbar-block--end :deep(.el-button) {
    width: 100%;
  }

  .table-shell :deep(.el-table) {
    min-width: 680px;
  }

  .row-actions :deep(.el-button.row-btn) {
    min-height: 30px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .game-form :deep(.el-input-number) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .toolbar-count {
    display: none;
  }
}
</style>
