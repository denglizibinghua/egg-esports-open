<template>
  <div>
    <div class="announcement-bar" :class="{ 'announcement-bar--compact': compact }" v-if="visible && announcements.length > 0 && !dismissed">
      <span class="announcement-accent" aria-hidden="true"></span>
      <span class="announcement-label">ALERT</span>
      <div class="announcement-body" @click="showDetail(announcements.length === 1 ? announcements[0] : announcements[currentIndex])">
        <template v-if="announcements.length === 1">
          <span class="announcement-title">{{ announcements[0].title }}</span>
          <span class="announcement-separator">/</span>
          <span class="announcement-content">{{ announcements[0].content }}</span>
        </template>
        <template v-else>
          <transition name="announcement-fade" mode="out-in">
            <div class="announcement-slide" :key="currentIndex">
              <span class="announcement-title">{{ announcements[currentIndex].title }}</span>
              <span class="announcement-separator">/</span>
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
      <button class="announcement-dismiss" @click="dismissed = true" aria-label="关闭公告">
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
            {{ detailAnnouncement.createTime }}
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
// 分页数据提取工具（兼容 records/list/content），统一提取自 @/utils/pagination
import { extractList } from '@/utils/pagination'
import { Bell, Close, Trophy } from '@element-plus/icons-vue'

export default {
  name: 'AnnouncementBar',
  components: { Bell, Close, Trophy },
  props: {
    gameId: {
      type: [Number, String],
      default: null
    },
    compact: {
      type: Boolean,
      default: false
    },
    visible: {
      type: Boolean,
      default: true
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
    fetchAnnouncements() {
      var params = {}
      if (this.gameId) {
        params.gameId = this.gameId
      }
      request.get('/announcement/active', { params: params }).then(function (res) {
        var data = res.data || res
        // 兼容 records/list/content 多种后端返回格式
        this.announcements = extractList(data)
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
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 34px;
  padding: 0 8px 0 14px;
  margin-bottom: 14px;
  overflow: hidden;
  background:
    linear-gradient(90deg, rgba(255, 70, 85, 0.05), transparent 42%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  animation: announcement-enter 0.4s var(--ease-out) both;
}

.announcement-bar--compact {
  min-height: 32px;
  padding: 0 6px 0 12px;
  margin-bottom: 12px;
}

@keyframes announcement-enter {
  from { opacity: 0; transform: translateY(-6px); }
  to { opacity: 1; transform: translateY(0); }
}

.announcement-accent {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 3px;
  background: var(--brand);
}

.announcement-label {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--brand);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.announcement-label::after {
  content: '';
  width: 1px;
  height: 11px;
  background: var(--border-default);
}

.announcement-body {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  cursor: pointer;
  transition: opacity var(--duration-fast) ease;
}

.announcement-body:hover {
  opacity: 0.86;
}

.announcement-slide {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.announcement-title {
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.announcement-separator {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--brand);
  opacity: 0.7;
}

.announcement-content {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--text-tertiary);
}

.announcement-dots {
  display: flex;
  align-items: center;
  gap: 5px;
  flex-shrink: 0;
  margin-left: 4px;
}

.announcement-dot {
  width: 12px;
  height: 2px;
  background: var(--border-strong);
  cursor: pointer;
  transition: background var(--duration-fast) ease, width var(--duration-fast) ease;
}

.announcement-dot--active {
  width: 18px;
  background: var(--brand);
}

.announcement-dot:hover {
  background: var(--text-tertiary);
}

.announcement-dismiss {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  flex-shrink: 0;
  color: var(--text-tertiary);
  background: none;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  transition: color var(--duration-fast) ease, background var(--duration-fast) ease;
}

.announcement-dismiss:hover {
  color: var(--brand);
  background: var(--brand-soft);
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

/* ===== Detail Dialog Content ===== */
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-game-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 8px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
}

.detail-game-tag--global {
  color: var(--accent-secondary);
  background: rgba(91, 140, 255, 0.09);
  border-color: rgba(91, 140, 255, 0.24);
}

.detail-game-tag i {
  font-size: 11px;
}

.detail-time {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  font-variant-numeric: tabular-nums;
}

.detail-content {
  padding: 14px;
  font-size: 13.5px;
  line-height: 1.75;
  color: var(--text-secondary);
  white-space: pre-wrap;
  word-break: break-word;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

@media (max-width: 600px) {
  .announcement-bar {
    gap: 8px;
    padding-left: 10px;
  }
  .announcement-label {
    display: none;
  }
  .announcement-title {
    max-width: 38%;
  }
}
</style>
