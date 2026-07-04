<template>
  <div class="announcement-tab">
    <div class="tab-header">
      <span class="announcement-tab-title">公告列表</span>
      <el-button type="primary" size="small" :icon="PlusIcon" @click="openAnnouncementDialog()">发布公告</el-button>
    </div>

    <div v-if="announcementLoading" class="skeleton-table">
      <div class="skeleton-table-header">
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
      <div v-for="i in 4" :key="'ann-skel-'+i" class="skeleton-table-row">
        <div class="skeleton-line skeleton-line--cell-lg"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-md"></div>
        <div class="skeleton-line skeleton-line--cell-sm"></div>
      </div>
    </div>

    <div v-else-if="announcements.length === 0" class="empty-state">
      <div class="empty-icon">📢</div>
      <h3 class="empty-title">暂无公告</h3>
      <p class="empty-desc">发布公告，向用户传达重要信息</p>
      <el-button type="primary" class="empty-action" @click="openAnnouncementDialog()">发布公告</el-button>
    </div>

    <template v-else>
      <el-table :data="announcements" style="width: 100%">
        <el-table-column label="标题" min-width="150" prop="title" />
        <el-table-column label="关联游戏" width="120">
          <template #default="scope">
            {{ getGameName(scope.row.gameId) || '全部' }}
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
        <el-table-column label="时间范围" width="220">
          <template #default="scope">
            <span class="ann-time-range">{{ formatDate(scope.row.startTime) }} ~ {{ formatDate(scope.row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="scope">
            {{ formatDate(scope.row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="scope">
            <el-button link size="small" @click="openAnnouncementDialog(scope.row)">编辑</el-button>
            <el-button link size="small" class="delete-btn" @click="handleDeleteAnnouncement(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
      width="520px"
    >
      <el-form :model="announcementForm" label-width="80px" class="announcement-form">
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
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="announcementForm.startTime"
            type="datetime"
            placeholder="选择开始时间"
            style="width: 100%"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="announcementForm.endTime"
            type="datetime"
            placeholder="选择结束时间"
            style="width: 100%"
            value-format="yyyy-MM-dd HH:mm:ss"
          />
        </el-form-item>
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
        type: 'warning'
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

/* Announcement Tab */
.announcement-tab-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.delete-btn {
  color: var(--accent-danger) !important;
}

.ann-time-range {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.announcement-form .el-form-item {
  margin-bottom: 18px;
}

.announcement-form .el-textarea__inner {
  background: var(--bg-surface-1) !important;
  border-color: var(--border-default) !important;
  color: var(--text-primary) !important;
  border-radius: var(--radius-md) !important;
}

.announcement-form .el-textarea__inner:focus {
  border-color: var(--accent-primary) !important;
}

.announcement-form .el-select .el-input__inner {
  cursor: pointer;
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

  /* 公告表单 label 宽度收紧 */
  .announcement-form :deep(.el-form-item__label) {
    width: 64px !important;
  }
  .announcement-form :deep(.el-input-number) {
    width: 140px !important;
  }
}
</style>
