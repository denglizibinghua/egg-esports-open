<template>
  <el-dialog
    v-model="dialogVisible"
    width="420px"
    custom-class="popup-announcement-dialog"
    :append-to-body="true"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    @close="onClose"
  >
    <template #header>
      <div class="popup-head">
        <div class="popup-eyebrow">BROADCAST // 公告</div>
        <div class="popup-title-slot">
          <span class="popup-title-mark">//</span>
          <span class="popup-title-text">{{ currentAnnouncement ? currentAnnouncement.title : '公告' }}</span>
        </div>
      </div>
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
          {{ formatPopupTime(currentAnnouncement.createTime) }}
        </span>
        <span class="popup-detail-index" v-if="queue.length > 1">{{ currentIndex + 1 }} / {{ queue.length }}</span>
      </div>
      <div class="popup-detail-content" v-html="safeContent"></div>
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
import DOMPurify from 'dompurify'

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
    },
    // 公告内容经 DOMPurify 清洗后再渲染，防止后台富文本注入
    safeContent() {
      if (!this.currentAnnouncement || !this.currentAnnouncement.content) return ''
      return DOMPurify.sanitize(this.currentAnnouncement.content)
    }
  },
  mounted() {
    this.fetchPopupAnnouncements()
  },
  methods: {
    formatPopupTime(time) {
      if (!time) return ''
      var d = new Date(time)
      if (isNaN(d.getTime())) return String(time)
      var pad = function (n) { return String(n).padStart(2, '0') }
      return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes())
    },
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
:deep(.el-dialog.popup-announcement-dialog) {
  width: 420px;
  background: var(--bg-surface-1) !important;
  border: 1px solid var(--border-default) !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-lg), 0 0 40px rgba(0, 0, 0, 0.55) !important;
  overflow: hidden;
}

:deep(.popup-announcement-dialog .el-dialog__header) {
  padding: 18px 22px 14px !important;
  margin-right: 0 !important;
  border-bottom: 1px solid var(--border-subtle);
  margin-bottom: 0 !important;
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn) {
  top: 16px !important;
  right: 16px !important;
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-tertiary) !important;
  font-size: 16px;
}

:deep(.popup-announcement-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
  color: var(--brand) !important;
}

:deep(.popup-announcement-dialog .el-dialog__body) {
  padding: 16px 22px 18px !important;
  color: var(--text-secondary);
}

:deep(.popup-announcement-dialog .el-dialog__footer) {
  padding: 14px 22px 18px !important;
  border-top: 1px solid var(--border-subtle);
}

/* Header */
.popup-head {
  min-width: 0;
  padding-right: 28px;
}
.popup-eyebrow {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
}
.popup-title-slot {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 8px;
  min-width: 0;
}
.popup-title-mark {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  color: var(--brand);
  flex-shrink: 0;
}
.popup-title-text {
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 700;
  line-height: 1.35;
  color: var(--text-primary);
  word-break: break-word;
}

/* Body */
.popup-detail-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.popup-detail-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  min-width: 0;
}
.popup-detail-game-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}
.popup-detail-game-tag--global {
  color: var(--accent-secondary);
  background: rgba(91, 140, 255, 0.09);
  border-color: rgba(91, 140, 255, 0.26);
}
.popup-detail-game-tag .el-icon {
  font-size: 11px;
}
.popup-detail-time {
  font-family: var(--font-mono);
  font-size: 10px;
  font-variant-numeric: tabular-nums;
  color: var(--text-tertiary);
  white-space: nowrap;
}
.popup-detail-index {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  white-space: nowrap;
}
.popup-detail-content {
  max-height: min(46vh, 420px);
  overflow-y: auto;
  padding: 15px 16px 15px 18px;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--text-primary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-left: 3px solid var(--brand);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  word-break: break-word;
}
.popup-detail-content :deep(a) {
  color: var(--accent-primary);
  text-decoration: underline;
}
.popup-detail-content :deep(img) {
  max-width: 100%;
  height: auto;
}

/* Footer */
.popup-dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.popup-dismiss-today {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  font-size: 11.5px;
  color: var(--text-tertiary);
  cursor: pointer;
  user-select: none;
}
.popup-dismiss-today input[type="checkbox"] {
  width: 13px;
  height: 13px;
  accent-color: var(--brand);
  cursor: pointer;
}
.popup-dismiss-today:hover {
  color: var(--text-secondary);
}

:deep(.el-button.popup-close-btn) {
  height: 32px !important;
  padding: 0 18px !important;
  background: var(--bg-surface-2) !important;
  border: 1px solid var(--border-default) !important;
  color: var(--text-secondary) !important;
  border-radius: var(--radius-sm) !important;
  font-weight: 600;
  transition: all var(--duration-fast) var(--ease-out);
}
:deep(.el-button.popup-close-btn:hover) {
  border-color: var(--brand) !important;
  color: var(--brand) !important;
  background: var(--brand-soft) !important;
}

/* Mobile */
@media (max-width: 540px) {
  :deep(.el-dialog.popup-announcement-dialog) {
    width: 92% !important;
    margin: 0 auto;
  }

  :deep(.popup-announcement-dialog .el-dialog__header) {
    padding: 14px 16px 12px !important;
  }

  :deep(.popup-announcement-dialog .el-dialog__body) {
    padding: 14px 16px !important;
  }

  :deep(.popup-announcement-dialog .el-dialog__footer) {
    padding: 12px 16px 14px !important;
  }

  .popup-dialog-footer {
    flex-direction: column-reverse;
    align-items: stretch;
    gap: 10px;
  }

  .popup-dismiss-today {
    justify-content: center;
  }

  :deep(.el-button.popup-close-btn) {
    width: 100%;
  }

  .popup-detail-content {
    max-height: 42vh;
  }
}
</style>
