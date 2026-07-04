<template>
  <div class="sync-panel">
    <div class="sync-action-area">
      <el-button
        type="primary"
        :loading="syncLoading"
        :disabled="syncLoading"
        :icon="RefreshIcon"
        @click="triggerSync"
      >
        {{ syncLoading ? '正在同步...' : '同步全部赛事数据（Riot + PandaScore + Liquipedia + GRID）' }}
      </el-button>
      <p v-if="lastSyncTime" class="sync-last-time">
        上次同步: {{ formatSyncTime(lastSyncTime) }}
      </p>
    </div>

    <!-- Loading Skeleton -->
    <div v-if="syncLoading" class="sync-skeleton">
      <div class="sync-skeleton-grid">
        <div v-for="i in 6" :key="'sync-skel-'+i" class="sync-skeleton-card">
          <div class="skeleton-line skeleton-line--stat-label"></div>
          <div class="skeleton-line skeleton-line--stat-value"></div>
        </div>
      </div>
    </div>

    <!-- Results Summary -->
    <div v-if="syncResult && !syncLoading" class="sync-results">
      <div class="sync-stats-grid">
        <div class="sync-stat-card">
          <span class="sync-stat-value sync-stat-value--gray">{{ syncResult.totalFetched }}</span>
          <span class="sync-stat-label">获取赛事</span>
        </div>
        <div class="sync-stat-card">
          <span class="sync-stat-value sync-stat-value--gray">{{ syncResult.totalSkipped }}</span>
          <span class="sync-stat-label">跳过(历史数据)</span>
        </div>
        <div class="sync-stat-card">
          <span class="sync-stat-value sync-stat-value--green">{{ syncResult.totalCreated }}</span>
          <span class="sync-stat-label">新增</span>
        </div>
        <div class="sync-stat-card">
          <span class="sync-stat-value sync-stat-value--blue">{{ syncResult.totalUpdated }}</span>
          <span class="sync-stat-label">更新</span>
        </div>
      </div>

      <!-- Per-Game Stats -->
      <div v-if="syncResult.perGameStats" class="sync-per-game">
        <div v-for="(stats, gameKey) in syncResult.perGameStats" :key="gameKey" class="sync-per-game-item">
          <span class="sync-per-game-label">{{ getGameNameByKey(gameKey) }}:</span>
          <span class="sync-per-game-val">+{{ stats[0] }} ~{{ stats[1] }} -{{ stats[2] }}</span>
        </div>
      </div>

      <!-- Error Display -->
      <div v-if="syncResult.errors && syncResult.errors.length > 0" class="sync-errors">
        <div class="sync-errors-title">同步错误</div>
        <div class="sync-errors-list">
          <div v-for="(err, idx) in syncResult.errors" :key="'sync-err-'+idx" class="sync-error-item">
            {{ err }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
import request from '@/utils/request'
import { Refresh } from '@element-plus/icons-vue'

export default {
  name: 'SyncTab',
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      RefreshIcon: markRaw(Refresh),
      syncLoading: false,
      syncResult: null,
      lastSyncTime: null
    }
  },
  methods: {
    // 将 perGameStats 的 raw key（如 "game-1"）映射为人类可读的游戏名
    getGameNameByKey(key) {
      var map = {
        'game-1': '无畏契约',
        'game-2': '英雄联盟',
        'game-3': 'Dota 2',
        'game-4': 'CS2'
      }
      return map[key] || key
    },
    triggerSync() {
      this.syncLoading = true
      this.syncResult = null
      // 同步涉及多个外部 API（Riot/OpenDota/GRID），整体耗时较长，
      // 单独放宽到 120 秒，避免默认 15 秒超时导致前端一直 loading
      request.post('/match/sync', {}, { timeout: 120000 }).then(res => {
        var data = res.data || res
        this.syncResult = data
        if (data.syncTime) {
          this.lastSyncTime = data.syncTime
        }
      }).catch(err => {
        var msg = (err && err.response && err.response.data && err.response.data.message) || (err && err.message) || '未知错误'
        this.$message.error('同步失败: ' + msg)
      }).finally(() => {
        this.syncLoading = false
      })
    },
    formatSyncTime(timeStr) {
      if (!timeStr) return '-'
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var m = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var h = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      var sec = d.getSeconds().toString().padStart(2, '0')
      return y + '-' + m + '-' + day + ' ' + h + ':' + min + ':' + sec
    }
  }
}
</script>

<style scoped>
/* Sync Panel */
.sync-panel {
  padding: 4px 0;
}

.sync-action-area {
  margin-bottom: 24px;
  padding: 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}

.sync-action-area .el-button--primary {
  background: var(--accent-primary) !important;
  border-color: var(--accent-primary) !important;
  color: #0a0a0f !important;
  font-weight: 600;
  border-radius: var(--radius-md) !important;
  padding: 10px 24px;
}

.sync-action-area .el-button--primary.is-disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.sync-last-time {
  margin: 12px 0 0 0;
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Sync Skeleton */
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

.sync-skeleton {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  padding: 20px;
}

.sync-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.sync-skeleton-card {
  padding: 16px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.skeleton-line--stat-label {
  width: 60%;
  height: 12px;
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

.skeleton-line--stat-value {
  width: 40%;
  height: 24px;
  background: linear-gradient(90deg, var(--bg-surface-3) 25%, var(--bg-surface-2) 50%, var(--bg-surface-3) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-sm);
}

/* Sync Results */
.sync-results {
  margin-top: 0;
}

.sync-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.sync-stat-card {
  padding: 16px 20px;
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.sync-stat-value {
  font-size: 28px;
  font-weight: 700;
  font-family: var(--font-display);
  line-height: 1.2;
}

.sync-stat-value--gray { color: var(--text-secondary); }
.sync-stat-value--green { color: #67c23a; }
.sync-stat-value--blue { color: #409eff; }
.sync-stat-value--orange { color: #e6a23c; }
.sync-stat-value--purple { color: #9254de; }

.sync-stat-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Sync Errors */
.sync-errors {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.sync-per-game {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 16px;
  margin-top: 8px;
  background: var(--bg-surface-3);
  border-radius: var(--radius-md);
}

.sync-per-game-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
}

.sync-per-game-label {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.sync-per-game-val {
  color: var(--text-secondary);
  font-family: var(--font-mono);
}

.sync-errors-title {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #e06c75;
  border-bottom: 1px solid var(--border-subtle);
}

.sync-errors-list {
  padding: 0;
}

.sync-error-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #e06c75;
  border-bottom: 1px solid var(--border-subtle);
  line-height: 1.5;
}

.sync-error-item:last-child {
  border-bottom: none;
}

/* ===== 移动端基础适配 ===== */
@media (max-width: 768px) {
  /* 同步统计/骨架网格：3 列 → 2 列 */
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .sync-stat-card {
    padding: 12px 14px;
  }
  .sync-stat-value {
    font-size: 24px;
  }
  .sync-action-area {
    padding: 14px;
  }
  .sync-action-area .el-button--primary {
    width: 100%;
    min-height: 44px;
  }
}

@media (max-width: 480px) {
  /* 同步统计在小屏单列堆叠 */
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: 1fr;
  }
}
</style>
