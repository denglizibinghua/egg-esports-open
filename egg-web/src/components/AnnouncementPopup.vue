<template>
  <el-dialog
    v-model="dialogVisible"
    width="360px"
    custom-class="popup-announcement-dialog"
    :append-to-body="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    @close="onClose"
  >
    <template #header>
      <span class="popup-title-slot">
        <span class="popup-title-icon">🔔</span>
        {{ currentAnnouncement ? currentAnnouncement.title : '公告' }}
      </span>
    </template>
    <div v-if="currentAnnouncement" class="popup-detail-body">
      <div class="popup-detail-meta">
        <span v-if="currentAnnouncement.gameId" class="popup-detail-game-tag">
          <el-icon><Trophy /></el-icon> 游戏专属
        </span>
        <span v-else class="popup-detail-game-tag popup-detail-game-tag--global">
          <el-icon><Bell /></el-icon> 全站公告
        </span>
        <span class="popup-detail-time" v-if="currentAnnouncement.createTime">
          {{ currentAnnouncement.createTime }}
        </span>
      </div>
      <div class="popup-detail-content">{{ currentAnnouncement.content }}</div>
    </div>
    <template #footer>
      <span class="popup-dialog-footer">
        <label class="popup-dismiss-today">
          <input type="checkbox" v-model="dismissToday" />
          <span>7天不再提醒</span>
        </label>
        <el-button class="popup-close-btn" @click="closeAndNext">关闭</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script>
import request from '@/utils/request'
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { Bell, Trophy } from '@element-plus/icons-vue'

export default {
  name: 'AnnouncementPopup',
  components: { Bell, Trophy },
  data() {
    return {
      queue: [],
      currentIndex: 0,
      dialogVisible: false,
      dismissToday: false
    }
  },
  computed: {
    currentAnnouncement() {
      return this.queue.length > 0 ? this.queue[this.currentIndex] : null
    }
  },
  mounted() {
    this.fetchPopupAnnouncements()
  },
  methods: {
    getTodayStr() {
      var d = new Date()
      var y = d.getFullYear()
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      return y + '-' + m + '-' + day
    },
    getDismissedIds() {
      try {
        var raw = localStorage.getItem('egg_popup_dismissed')
        return raw ? JSON.parse(raw) : []
      } catch (e) {
        return []
      }
    },
    saveDismissedIds(ids) {
      localStorage.setItem('egg_popup_dismissed', JSON.stringify(ids))
    },
    isDismissedToday() {
      // 7天逻辑：egg_popup_dismissed_today 存过期时间戳
      var val = localStorage.getItem('egg_popup_dismissed_today')
      if (!val) return false
      // 兼容旧版 "YYYY-MM-DD" 值：当日仍视为屏蔽
      if (/^\d{4}-\d{2}-\d{2}$/.test(val)) return val === this.getTodayStr()
      var ts = Number(val)
      return !isNaN(ts) && Date.now() < ts
    },
    setDismissed7Days() {
      var expire = Date.now() + 7 * 24 * 60 * 60 * 1000
      localStorage.setItem('egg_popup_dismissed_today', String(expire))
    },
    fetchPopupAnnouncements() {
      if (this.isDismissedToday()) return

      var self = this
      request.get('/announcement/active', { params: { displayType: 2 } }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        var list = extractList(data)

        var dismissedIds = self.getDismissedIds()
        var filtered = list.filter(function (item) {
          return dismissedIds.indexOf(item.id) === -1
        })

        if (filtered.length > 0) {
          self.queue = filtered
          self.currentIndex = 0
          // D5: 首绘不阻塞，3s 后再弹
          setTimeout(function () {
            // 若期间已被屏蔽则不再弹
            if (self.isDismissedToday()) return
            self.dialogVisible = true
          }, 3000)
        }
      }).catch(function () {})
    },
    onClose() {
      if (this.dismissToday) {
        this.setDismissed7Days()
      }
    },
    closeAndNext() {
      if (this.currentAnnouncement) {
        var dismissedIds = this.getDismissedIds()
        if (dismissedIds.indexOf(this.currentAnnouncement.id) === -1) {
          dismissedIds.push(this.currentAnnouncement.id)
          this.saveDismissedIds(dismissedIds)
        }
      }

      if (this.dismissToday) {
        this.setDismissed7Days()
        this.dialogVisible = false
        return
      }

      if (this.currentIndex < this.queue.length - 1) {
        this.currentIndex++
      } else {
        this.dialogVisible = false
      }
    }
  }
}
</script>

<style scoped>
/* ===== Popup Announcement Dialog ===== */
:deep(.popup-announcement-dialog) {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-top: 3px solid var(--accent-primary);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg), 0 0 40px rgba(0, 0, 0, 0.6);
}

:deep(.popup-announcement-dialog .el-dialog__header) {
  padding: 20px 24px 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--border-subtle);
}

:deep(.popup-announcement-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 700;
  font-family: var(--font-display);
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn) {
  top: 20px;
  right: 24px;
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-tertiary);
  font-size: 18px;
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn .el-dialog__close:hover) {
  color: var(--accent-primary);
}

:deep(.popup-announcement-dialog .el-dialog__body) {
  padding: 20px 24px;
  color: var(--text-secondary);
}

:deep(.popup-announcement-dialog .el-dialog__footer) {
  padding: 16px 24px 20px;
  margin-top: 8px;
  border-top: 1px solid var(--border-subtle);
}

.popup-title-slot {
  display: flex;
  align-items: center;
  gap: 10px;
}

.popup-title-icon {
  font-size: 22px;
  line-height: 1;
}

.popup-detail-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.popup-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  background: var(--bg-surface-2);
  padding: 8px 14px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
  margin-bottom: 20px;
}

.popup-detail-game-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: rgba(0, 229, 160, 0.1);
  color: var(--accent-primary);
  border: 1px solid rgba(0, 229, 160, 0.2);
}

.popup-detail-game-tag--global {
  background: rgba(91, 140, 255, 0.1);
  color: #5b8cff;
  border-color: rgba(91, 140, 255, 0.2);
}

.popup-detail-game-tag i {
  font-size: 11px;
}

.popup-detail-time {
  font-size: 12px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.popup-detail-content {
  font-size: 14px;
  line-height: 2.0;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 20px;
  background: var(--bg-surface-2);
  border-radius: 0 var(--radius-md) var(--radius-md) 0;
  border: 1px solid var(--border-subtle);
  border-left: 4px solid var(--accent-primary);
}

.popup-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.popup-dismiss-today {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
  cursor: pointer;
  user-select: none;
}

.popup-dismiss-today input[type="checkbox"] {
  accent-color: var(--accent-primary);
  cursor: pointer;
}

.popup-dismiss-today:hover {
  color: var(--text-secondary);
}

:deep(.popup-close-btn) {
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-md) !important;
  padding: 8px 24px !important;
  font-weight: 500;
  transition: all var(--duration-fast) var(--ease-out);
}

:deep(.popup-close-btn:hover) {
  border-color: var(--accent-primary) !important;
  color: var(--accent-primary) !important;
  background: rgba(0, 229, 160, 0.06) !important;
}

/* Mobile */
@media (max-width: 540px) {
  :deep(.popup-announcement-dialog) {
    width: 92% !important;
    margin: 0 auto;
  }

  :deep(.popup-announcement-dialog .el-dialog__header) {
    padding: 16px 16px 12px;
  }

  :deep(.popup-announcement-dialog .el-dialog__body) {
    padding: 16px;
  }

  :deep(.popup-announcement-dialog .el-dialog__footer) {
    padding: 12px 16px 16px;
  }

  .popup-dialog-footer {
    flex-direction: column-reverse;
    gap: 12px;
    align-items: stretch;
  }

  .popup-dismiss-today {
    justify-content: center;
  }
}
</style>
