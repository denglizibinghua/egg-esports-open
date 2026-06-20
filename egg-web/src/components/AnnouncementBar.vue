<template>
  <div class="announcement-bar" v-if="announcements.length > 0 && !dismissed">
    <div class="announcement-accent"></div>
    <i class="el-icon-bell announcement-icon"></i>
    <div class="announcement-body">
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
      <i class="el-icon-close"></i>
    </button>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'AnnouncementBar',
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
      rotateTimer: null
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
  beforeDestroy() {
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

@media (max-width: 480px) {
  .announcement-bar {
    padding: 8px 12px;
    gap: 8px;
  }

  .announcement-content {
    white-space: normal;
    line-height: 1.4;
  }
}
</style>
