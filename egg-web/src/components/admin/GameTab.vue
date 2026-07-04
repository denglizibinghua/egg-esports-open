<template>
  <div class="game-tab">
    <div class="tab-header">
      <span class="announcement-tab-title">游戏列表</span>
      <el-button type="primary" size="small" :icon="PlusIcon" @click="openGameDialog()">添加游戏</el-button>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="gameLoading" class="skeleton-table">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
      <div v-for="i in 6" :key="'game-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else-if="games.length === 0" class="empty-state">
      <div class="empty-icon">🎮</div>
      <h3 class="empty-title">还没有游戏</h3>
      <p class="empty-desc">添加游戏，让赛事按游戏分类管理</p>
      <el-button type="primary" class="empty-action" @click="openGameDialog()">添加游戏</el-button>
    </div>

    <!-- Game Table -->
    <template v-else>
      <el-table :data="games" style="width: 100%">
        <el-table-column label="ID" width="70" prop="id" />
        <el-table-column label="游戏名称" min-width="150" prop="name" />
        <el-table-column label="图标" width="120" align="center">
          <template #default="scope">
            <el-image
              v-if="scope.row.icon"
              :src="scope.row.icon"
              style="width: 32px; height: 32px; border-radius: var(--radius-sm);"
              fit="cover"
            />
            <span v-else class="game-icon-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column label="排序" width="80" align="center" prop="sortOrder" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="scope">
            <span v-if="scope.row.status === 1" class="status-badge status-badge--live">
              <span class="status-dot status-dot--pulse"></span>启用
            </span>
            <span v-else class="status-badge status-badge--finished">
              <span class="status-dot"></span>停用
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="scope">
            <el-button link size="small" @click="openGameDialog(scope.row)">编辑</el-button>
            <el-button link size="small" class="delete-btn" @click="handleDeleteGame(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <!-- Game Dialog -->
    <el-dialog
      :title="gameForm.id ? '编辑游戏' : '添加游戏'"
      v-model="gameDialogVisible"
      width="480px"
    >
      <el-form :model="gameForm" label-width="80px" class="announcement-form">
        <el-form-item label="名称">
          <el-input v-model="gameForm.name" placeholder="例如: VALORANT" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="图标URL">
          <el-input v-model="gameForm.icon" placeholder="图标地址（可选）" />
        </el-form-item>
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
        type: 'warning'
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
.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
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

/* Game Tab（沿用原 announcement-tab-title 命名以保持与原代码一致） */
.announcement-tab-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.game-icon-empty {
  color: var(--text-tertiary);
  font-size: 14px;
}

.delete-btn {
  color: var(--accent-danger) !important;
}

/* Game Tab 表单样式（沿用原 announcement-form 命名以保持与原代码一致） */
.announcement-form .el-form-item {
  margin-bottom: 18px;
}

.announcement-form .el-input-number {
  width: 160px;
}

.announcement-form .el-switch__label {
  color: var(--text-tertiary);
}

.announcement-form .el-switch__label.is-active {
  color: var(--accent-primary);
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

  /* 游戏表单 label 宽度收紧 */
  .announcement-form :deep(.el-form-item__label) {
    width: 64px !important;
  }
  .announcement-form :deep(.el-input-number) {
    width: 140px !important;
  }
}
</style>
