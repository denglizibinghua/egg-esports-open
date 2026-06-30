<template>
  <div>
  <div class="announcement-bar" v-if="announcements.length > 0 && !dismissed">
    <div class="announcement-accent"></div>
    <el-icon class="announcement-icon"><Bell /></el-icon>
    <div class="announcement-body" @click="showDetail(announcements.length === 1 ? announcements[0] : announcements[currentIndex])" style="cursor: pointer">
      <template v-if="announcements.length === 1">
        <span class="announcement-title">{{ announcements[0].title }}</span>
        <span class="announcement-separator">&middot;</span>
        <span class="announcement-content">{{ announcements[0].content }}</span>
      </template>
      <template v-else>
        <transition name="announcement-fade" mode="out-in">
          <div class="announcement-slide" :key="currentIndex">
            <span class="announcement-title">{{ announcements[currentIndex].title }}</span>
            <span class="announcement-separator">&middot;</span>
            <span class="announcement-content">{{ announcements[currentIndex].content }}</span>
          </div>
        </transition>
        <div class="announcement-dots" v-if="announcements.length > 1">
          <span
            v-for="(a, idx) in announcements"
            :key="'dot-' + idx"
            class="announcement-dot"
            :class="{ 'announcement-dot--active': idx === currentIndex }"
            @click="currentIndex = idx"
          ></span>
        </div>
      </template>
    </div>
    <button class="announcement-dismiss" @click="dismissed = true">
      <el-icon><Close /></el-icon>
    </button>
  </div>

  <!-- Announcement Detail Dialog -->
  <el-dialog
    :title="detailAnnouncement ? detailAnnouncement.title : '公告详情'"
    v-model="detailVisible"
    width="480px"
    custom-class="announcement-detail-dialog"
    :append-to-body="true"
  >
    <div v-if="detailAnnouncement" class="detail-body">
      <div class="detail-meta">
        <span v-if="detailAnnouncement.gameId" class="detail-game-tag">
          <el-icon><Trophy /></el-icon> 游戏专属
        </span>
        <span v-else class="detail-game-tag detail-game-tag--global">
          <el-icon><Bell /></el-icon> 全站公告
        </span>
        <span class="detail-time" v-if="detailAnnouncement.createTime">
          {{ formatTime(detailAnnouncement.createTime) }}
        </span>
      </div>
      <div class="detail-content">{{ detailAnnouncement.content }}</div>
    </div>
    <template #footer>
      <span>
        <el-button @click="detailVisible = false">关闭</el-button>
      </span>
    </template>
  </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'
import { Bell, Close, Trophy } from '@element-plus/icons-vue'

export default {
  name: 'AnnouncementBar',
  components: { Bell, Close, Trophy },
  props: {
    gameId: {
      type: [Number, String],
      default: null
    }
  },
  data() {
    return {
      announcements: [],
      dismissed: false,
      currentIndex: 0,
      rotateTimer: null,
      detailVisible: false,
      detailAnnouncement: null
    }
  },
  watch: {
    gameId() {
      this.fetchAnnouncements()
    }
  },
  created() {
    this.fetchAnnouncements()
  },
  beforeUnmount() {
    this.stopRotate()
  },
  methods: {
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return m + '-' + day + ' ' + h + ':' + min
    },
    fetchAnnouncements() {
      var params = {}
      if (this.gameId) {
        params.gameId = this.gameId
      }
      request.get('/announcement/active', { params: params }).then(function (res) {
        var data = res.data || res
        if (Array.isArray(data)) {
          this.announcements = data
        } else if (data.records) {
          this.announcements = data.records
        } else {
          this.announcements = data.list || data.content || []
        }
        this.currentIndex = 0
        this.startRotate()
      }.bind(this)).catch(function () {
        this.announcements = []
      }.bind(this))
    },
    startRotate() {
      this.stopRotate()
      if (this.announcements.length > 1) {
        var self = this
        this.rotateTimer = setInterval(function () {
          self.currentIndex = (self.currentIndex + 1) % self.announcements.length
        }, 5000)
      }
    },
    stopRotate() {
      if (this.rotateTimer) {
        clearInterval(this.rotateTimer)
        this.rotateTimer = null
      }
    },
    showDetail(announcement) {
      if (!announcement) return
      this.detailAnnouncement = announcement
      this.detailVisible = true
    }
  }
}
</script>

<style scoped>
.announcement-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  animation: announcement-enter 0.4s var(--ease-out) both;
}

@keyframes announcement-enter {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.announcement-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: var(--accent-primary);
  border-radius: 3px 0 0 3px;
}

.announcement-icon {
  color: var(--accent-primary);
  font-size: 16px;
  flex-shrink: 0;
  margin-left: 4px;
}

.announcement-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  transition: opacity var(--duration-fast) ease;
}

.announcement-body:hover {
  opacity: 0.85;
}

.announcement-slide {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.announcement-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  flex-shrink: 0;
}

.announcement-separator {
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.announcement-content {
  font-size: 13px;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.announcement-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  margin-left: 8px;
}

.announcement-dot {
  width: 5px;
  height: 5px;
  border-radius: var(--radius-full);
  background: var(--border-default);
  cursor: pointer;
  transition: background var(--duration-fast) ease,
              transform var(--duration-fast) ease;
}

.announcement-dot--active {
  background: var(--accent-primary);
  transform: scale(1.3);
}

.announcement-dot:hover {
  background: var(--text-tertiary);
}

.announcement-dismiss {
  background: none;
  border: none;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 4px;
  font-size: 14px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  transition: color var(--duration-fast) ease,
              background var(--duration-fast) ease;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.announcement-dismiss:hover {
  color: var(--text-primary);
  background: var(--bg-surface-3);
}

/* Transition */
.announcement-fade-enter-active,
.announcement-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s var(--ease-out);
}

.announcement-fade-enter {
  opacity: 0;
  transform: translateY(6px);
}

.announcement-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

@media (max-width: 767px) {
  .announcement-bar {
    padding: 8px 12px;
    gap: 8px;
    font-size: 12px;
  }
}

/* ===== Detail Dialog ===== */
:deep(.announcement-detail-dialog) {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg), 0 0 40px rgba(0, 0, 0, 0.6);
}

:deep(.announcement-detail-dialog .el-dialog__header) {
  padding: 20px 24px 12px;
  border-bottom: 1px solid var(--border-subtle);
}

:deep(.announcement-detail-dialog .el-dialog__title) {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  font-family: var(--font-display);
}

:deep(.announcement-detail-dialog .el-dialog__headerbtn) {
  top: 20px;
  right: 24px;
}

:deep(.announcement-detail-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: var(--text-tertiary);
  font-size: 18px;
}

:deep(.announcement-detail-dialog .el-dialog__headerbtn .el-dialog__close:hover) {
  color: var(--accent-primary);
}

:deep(.announcement-detail-dialog .el-dialog__body) {
  padding: 20px 24px;
  color: var(--text-secondary);
}

:deep(.announcement-detail-dialog .el-dialog__footer) {
  padding: 12px 24px 20px;
  border-top: 1px solid var(--border-subtle);
}

:deep(.announcement-detail-dialog .el-button) {
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  color: var(--text-secondary);
  border-radius: var(--radius-md);
  padding: 8px 24px;
  font-weight: 500;
  transition: all var(--duration-fast) var(--ease-out);
}

:deep(.announcement-detail-dialog .el-button:hover) {
  border-color: var(--accent-primary);
  color: var(--accent-primary);
  background: rgba(0, 229, 160, 0.06);
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.detail-game-tag {
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

.detail-game-tag--global {
  background: rgba(91, 140, 255, 0.1);
  color: #5b8cff;
  border-color: rgba(91, 140, 255, 0.2);
}

.detail-game-tag i {
  font-size: 11px;
}

.detail-time {
  font-size: 12px;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.detail-content {
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  padding: 16px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
}

/* Mobile dialog */
@media (max-width: 540px) {
  :deep(.announcement-detail-dialog) {
    width: 92% !important;
    margin: 0 auto;
  }

  :deep(.announcement-detail-dialog .el-dialog__header) {
    padding: 16px 16px 12px;
  }

  :deep(.announcement-detail-dialog .el-dialog__body) {
    padding: 16px;
  }

  :deep(.announcement-detail-dialog .el-dialog__footer) {
    padding: 12px 16px 16px;
  }
}
</style>
