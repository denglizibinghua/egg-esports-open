<template>
  <div class="announcement-tab">
    <div class="admin-toolbar">
      <div class="toolbar-block">
        <span class="toolbar-kicker">NOTICE / 公告</span>
        <span class="toolbar-title">公告列表</span>
        <span class="toolbar-count">TOTAL <b>{{ announcementTotal }}</b></span>
      </div>
      <div class="toolbar-block toolbar-block--end">
        <el-button type="primary" size="small" :icon="PlusIcon" @click="openAnnouncementDialog()">发布公告</el-button>
      </div>
    </div>

    <div v-if="announcementLoading" class="skeleton-table u-panel">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
      <div v-for="i in 4" :key="'ann-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-lg shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-md shimmer-bg"></div>
        <div class="skeleton-line skeleton-line--cell-sm shimmer-bg"></div>
      </div>
    </div>

    <div v-else-if="announcements.length === 0" class="empty-state u-panel">
      <div class="empty-mark">//</div>
      <h3 class="empty-title">暂无公告</h3>
      <p class="empty-desc">发布公告，向用户传达重要信息</p>
      <el-button type="primary" class="empty-action" @click="openAnnouncementDialog()">发布公告</el-button>
    </div>

    <template v-else>
      <div class="table-shell">
        <el-table :data="announcements" style="width: 100%">
          <el-table-column label="标题 / TITLE" min-width="200">
            <template #default="scope">
              <div class="notice-cell">
                <span class="notice-title" :title="scope.row.title">{{ scope.row.title }}</span>
                <span v-if="scope.row.content" class="notice-desc" :title="scope.row.content">{{ scope.row.content }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="关联游戏" width="120">
            <template #default="scope">
              <span v-if="getGameName(scope.row.gameId)" class="tournament-chip">{{ getGameName(scope.row.gameId) }}</span>
              <span v-else class="cell-muted">全部</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100" align="center">
            <template #default="scope">
              <span v-if="scope.row.status === 1" class="status-badge status-badge--live">
                <span class="status-dot status-dot--pulse"></span>活跃
              </span>
              <span v-else class="status-badge status-badge--finished">
                <span class="status-dot"></span>停用
              </span>
            </template>
          </el-table-column>
          <el-table-column label="时间范围 / WINDOW" width="230">
            <template #default="scope">
              <span class="ann-time-range">{{ formatDate(scope.row.startTime) }} ~ {{ formatDate(scope.row.endTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" width="150">
            <template #default="scope">
              <span class="cell-time">{{ formatDate(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" align="right">
            <template #default="scope">
              <div class="row-actions">
                <el-button link size="small" class="row-btn" @click="openAnnouncementDialog(scope.row)">编辑</el-button>
                <el-button link size="small" class="row-btn row-btn--danger" @click="handleDeleteAnnouncement(scope.row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </template>

    <el-pagination
      v-if="announcementTotal > 0"
      background
      layout="prev, pager, next"
      :total="announcementTotal"
      :page-size="announcementPageSize"
      v-model:current-page="announcementPage"
      @current-change="fetchAnnouncements"
    />

    <!-- Announcement Dialog -->
    <el-dialog
      :title="announcementForm.id ? '编辑公告' : '发布公告'"
      v-model="announcementDialogVisible"
      width="560px"
    >
      <div class="editor-head">
        <span class="editor-kicker">ANNOUNCEMENT / EDITOR</span>
      </div>
      <el-form :model="announcementForm" label-width="80px" label-position="top" class="announcement-form">
        <el-form-item label="标题">
          <el-input v-model="announcementForm.title" placeholder="公告标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input
            v-model="announcementForm.content"
            type="textarea"
            :rows="3"
            placeholder="公告内容"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="关联游戏">
            <el-select v-model="announcementForm.gameId" placeholder="全部游戏（可选）" clearable style="width: 100%">
              <el-option
                v-for="game in adminGames"
                :key="game.id"
                :label="game.name"
                :value="game.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-switch
              v-model="announcementForm.status"
              :active-value="1"
              :inactive-value="0"
              active-text="启用"
              inactive-text="停用"
            />
          </el-form-item>
        </div>
        <div class="form-grid">
          <el-form-item label="开始时间">
            <el-date-picker
              v-model="announcementForm.startTime"
              type="datetime"
              placeholder="选择开始时间"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker
              v-model="announcementForm.endTime"
              type="datetime"
              placeholder="选择结束时间"
              style="width: 100%"
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>
        </div>
        <el-form-item label="排序">
          <el-input-number v-model="announcementForm.sortOrder" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="announcementDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="announcementSubmitting" @click="handleSaveAnnouncement">
          {{ announcementForm.id ? '保存修改' : '发布' }}
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
  name: 'AnnouncementTab',
  props: {
    // 游戏列表版本号：GameTab 增删改游戏后父组件会递增此值，触发本组件重新拉取游戏下拉
    gamesVersion: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      PlusIcon: markRaw(Plus),
      announcements: [],
      announcementLoading: false,
      announcementPage: 1,
      announcementPageSize: 20,
      announcementTotal: 0,
      announcementDialogVisible: false,
      announcementSubmitting: false,
      announcementForm: {
        id: null,
        title: '',
        content: '',
        gameId: null,
        status: 1,
        startTime: '',
        endTime: '',
        sortOrder: 0
      },
      // 关联游戏下拉数据（与 GameTab 共享同一接口 /game/list）
      adminGames: []
    }
  },
  watch: {
    // 游戏列表版本变化时重新拉取下拉数据（对应原组件 GameTab 改后刷新 adminGames 的行为）
    gamesVersion() {
      this.fetchAdminGames()
    }
  },
  created() {
    this.fetchAnnouncements()
    this.fetchAdminGames()
  },
  methods: {
    fetchAnnouncements() {
      this.announcementLoading = true
      request.get('/announcement/list', {
        params: { page: this.announcementPage, size: this.announcementPageSize }
      }).then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.announcements = extractList(data)
        if (Array.isArray(data)) {
          this.announcementTotal = data.length
        } else if (data.records) {
          this.announcementTotal = data.total || data.records.length
        } else {
          this.announcementTotal = data.total || data.totalElements || 0
        }
      }).catch(() => {}).finally(() => {
        this.announcementLoading = false
      })
    },
    fetchAdminGames() {
      request.get('/game/list').then(res => {
        const data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.adminGames = extractList(data)
      }).catch(() => {
        this.adminGames = []
      })
    },
    getGameName(gameId) {
      if (!gameId) return ''
      var game = this.adminGames.find(function (g) { return g.id === gameId })
      return game ? game.name : ''
    },
    openAnnouncementDialog(row) {
      if (row) {
        this.announcementForm = {
          id: row.id,
          title: row.title || '',
          content: row.content || '',
          gameId: row.gameId || null,
          status: row.status != null ? row.status : 1,
          startTime: row.startTime || '',
          endTime: row.endTime || '',
          sortOrder: row.sortOrder || 0
        }
      } else {
        this.announcementForm = {
          id: null,
          title: '',
          content: '',
          gameId: null,
          status: 1,
          startTime: '',
          endTime: '',
          sortOrder: 0
        }
      }
      this.announcementDialogVisible = true
    },
    handleSaveAnnouncement() {
      if (!this.announcementForm.title || !this.announcementForm.title.trim()) {
        this.$message.warning('请输入公告标题')
        return
      }
      if (!this.announcementForm.content || !this.announcementForm.content.trim()) {
        this.$message.warning('请输入公告内容')
        return
      }
      this.announcementSubmitting = true
      var payload = {
        title: this.announcementForm.title,
        content: this.announcementForm.content,
        gameId: this.announcementForm.gameId || null,
        status: this.announcementForm.status,
        startTime: this.announcementForm.startTime || null,
        endTime: this.announcementForm.endTime || null,
        sortOrder: this.announcementForm.sortOrder || 0
      }
      var promise
      if (this.announcementForm.id) {
        promise = request.put('/announcement/' + this.announcementForm.id, payload)
      } else {
        promise = request.post('/announcement', payload)
      }
      promise.then(() => {
        this.$message.success(this.announcementForm.id ? '修改成功' : '发布成功')
        this.announcementDialogVisible = false
        this.fetchAnnouncements()
      }).catch(() => {}).finally(() => {
        this.announcementSubmitting = false
      })
    },
    handleDeleteAnnouncement(row) {
      this.$confirm('确定删除公告「' + row.title + '」？', '确认删除', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonType: 'danger'
      }).then(() => {
        request.delete('/announcement/' + row.id).then(() => {
          this.$message.success('删除成功')
          this.fetchAnnouncements()
        }).catch(() => {})
      }).catch(() => {})
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
.announcement-tab {
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

.announcement-tab :deep(.el-pagination) {
  margin-top: 0;
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
  min-width: 880px;
  background: transparent;
}

.table-shell :deep(.el-table th.el-table__cell:first-child .cell),
.table-shell :deep(.el-table td.el-table__cell:first-child .cell) {
  padding-left: 18px;
}

.notice-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.notice-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cell-muted {
  font-size: 12px;
  color: var(--text-tertiary);
}

.ann-time-range {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10.5px;
  letter-spacing: 0.01em;
  color: var(--text-secondary);
  white-space: nowrap;
}

.cell-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11.5px;
  color: var(--text-secondary);
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

.announcement-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.announcement-form :deep(.el-form-item__label) {
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

.announcement-form :deep(.el-textarea__inner) {
  font-family: var(--font-body);
  color: var(--text-primary);
  line-height: 1.6;
}

.announcement-form :deep(.el-input-number) {
  width: 160px;
}

.announcement-form :deep(.el-switch.is-checked .el-switch__core) {
  background: var(--accent-primary);
  border-color: var(--accent-primary);
}

.announcement-form :deep(.el-switch__label) {
  color: var(--text-tertiary);
  font-size: 12px;
}

.announcement-form :deep(.el-switch__label.is-active) {
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
    min-width: 840px;
  }

  .row-actions :deep(.el-button.row-btn) {
    min-height: 30px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .announcement-form :deep(.el-input-number) {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .toolbar-count {
    display: none;
  }
}
</style>
