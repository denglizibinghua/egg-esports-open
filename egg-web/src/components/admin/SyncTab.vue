<template>
  <div class="sync-panel">
    <section class="sync-control u-panel">
      <div class="control-head">
        <div class="control-copy">
          <div class="control-kicker">SYNC / DATA PIPELINE</div>
          <h3 class="control-title">赛事数据同步</h3>
          <p class="control-desc">从外部数据源拉取赛程与赛果，已存在赛事会被更新，历史数据自动跳过。</p>
        </div>
        <span :class="['control-state', { 'is-running': syncLoading }]">
          <span class="state-led"></span>{{ syncLoading ? 'RUNNING' : 'STANDBY' }}
        </span>
      </div>

      <div class="control-body">
        <div class="source-row">
          <span class="source-label">SOURCES</span>
          <span class="source-chip">RIOT</span>
          <span class="source-chip">PANDASCORE</span>
          <span class="source-chip">LIQUIPEDIA</span>
          <span class="source-chip">GRID</span>
        </div>

        <div class="control-actions">
          <span v-if="lastSyncTime" class="sync-last-time">LAST SYNC · {{ formatSyncTime(lastSyncTime) }}</span>
          <el-button
            type="primary"
            class="sync-run-btn"
            :loading="syncLoading"
            :disabled="syncLoading"
            :icon="RefreshIcon"
            @click="triggerSync"
          >
            {{ syncLoading ? '正在同步...' : '立即同步' }}
          </el-button>
        </div>
      </div>
    </section>

    <!-- Loading Skeleton -->
    <div v-if="syncLoading" class="sync-skeleton u-panel">
      <div class="sync-skeleton-grid">
        <div v-for="i in 4" :key="'sync-skel-'+i" class="sync-skeleton-card">
          <div class="skeleton-line skeleton-line--stat-label shimmer-bg"></div>
          <div class="skeleton-line skeleton-line--stat-value shimmer-bg"></div>
        </div>
      </div>
    </div>

    <!-- Idle -->
    <div v-else-if="!syncResult" class="sync-idle u-panel">
      <div class="idle-mark">//</div>
      <div class="idle-text">
        <strong>等待同步指令</strong>
        <span>执行同步后，这里会显示本次拉取的统计与错误日志。</span>
      </div>
    </div>

    <!-- Results Summary -->
    <template v-if="syncResult && !syncLoading">
      <section class="sync-results u-panel">
        <div class="results-head">
          <span class="results-kicker">RESULT / 本次同步</span>
          <span class="results-meta">TOTAL {{ syncResult.totalFetched }}</span>
        </div>

        <div class="sync-stats-grid">
          <div class="sync-stat-card sync-stat-card--fetched">
            <span class="sync-stat-label">获取赛事 / FETCHED</span>
            <span class="sync-stat-value sync-stat-value--gray">{{ syncResult.totalFetched }}</span>
          </div>
          <div class="sync-stat-card sync-stat-card--skipped">
            <span class="sync-stat-label">跳过(历史数据) / SKIPPED</span>
            <span class="sync-stat-value sync-stat-value--muted">{{ syncResult.totalSkipped }}</span>
          </div>
          <div class="sync-stat-card sync-stat-card--created">
            <span class="sync-stat-label">新增 / CREATED</span>
            <span class="sync-stat-value sync-stat-value--green">{{ syncResult.totalCreated }}</span>
          </div>
          <div class="sync-stat-card sync-stat-card--updated">
            <span class="sync-stat-label">更新 / UPDATED</span>
            <span class="sync-stat-value sync-stat-value--blue">{{ syncResult.totalUpdated }}</span>
          </div>
        </div>

        <!-- Per-Game Stats -->
        <div v-if="syncResult.perGameStats" class="sync-per-game">
          <div class="sync-per-game-head">PER GAME / 分游戏统计</div>
          <div class="sync-per-game-list">
            <div v-for="(stats, gameKey) in syncResult.perGameStats" :key="gameKey" class="sync-per-game-item">
              <span class="sync-per-game-label">{{ getGameNameByKey(gameKey) }}</span>
              <span class="sync-per-game-val">+{{ stats[0] }} ~{{ stats[1] }} -{{ stats[2] }}</span>
            </div>
          </div>
        </div>

        <!-- Error Display -->
        <div v-if="syncResult.errors && syncResult.errors.length > 0" class="sync-errors">
          <div class="sync-errors-title">
            <span class="error-led"></span>SYNC ERRORS / 同步错误
          </div>
          <div class="sync-errors-list">
            <div v-for="(err, idx) in syncResult.errors" :key="'sync-err-'+idx" class="sync-error-item">
              <span class="sync-error-idx">{{ String(idx + 1).padStart(2, '0') }}</span>
              <span class="sync-error-msg">{{ err }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
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
.sync-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---------- Control panel ---------- */
.sync-control {
  padding: 18px 20px;
}

.control-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.control-copy {
  min-width: 0;
}

.control-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.control-title {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}

.control-desc {
  margin-top: 5px;
  max-width: 560px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--text-tertiary);
}

.control-state {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 3px 9px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--accent-primary);
  background: var(--accent-primary-soft);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
}

.control-state.is-running {
  color: var(--accent-warning);
  background: var(--bg-surface-2);
}

.state-led {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  box-shadow: 0 0 8px currentColor;
}

.control-state.is-running .state-led {
  animation: sync-pulse 1.2s ease-in-out infinite;
}

@keyframes sync-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.25; }
}

.control-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding-top: 14px;
}

.source-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.source-label {
  margin-right: 4px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  white-space: nowrap;
}

.source-chip {
  padding: 2px 7px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.control-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-shrink: 0;
}

.sync-last-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  letter-spacing: 0.04em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.sync-run-btn {
  min-width: 132px;
}

.sync-run-btn :deep(.el-icon) {
  transition: transform var(--duration-normal) ease;
}

.sync-run-btn.is-loading :deep(.el-icon) {
  animation: sync-rotate 1s linear infinite;
}

@keyframes sync-rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ---------- Idle ---------- */
.sync-idle {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 26px 22px;
  border-style: dashed;
  background: transparent;
}

.idle-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 17px;
  font-weight: 700;
  color: var(--brand);
  background: var(--brand-soft);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-sm);
}

.idle-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.idle-text strong {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.idle-text span {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

/* ---------- Results ---------- */
.sync-results {
  padding: 16px;
}

.results-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}

.results-kicker {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.results-meta {
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.12em;
  color: var(--text-faint);
}

.sync-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.sync-stat-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 13px 14px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--border-default);
  border-radius: var(--radius-sm);
}

.sync-stat-card--created {
  border-left-color: var(--accent-primary);
}

.sync-stat-card--updated {
  border-left-color: var(--accent-secondary);
}

.sync-stat-card--skipped {
  border-left-color: var(--text-faint);
}

.sync-stat-label {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary);
  line-height: 1.4;
}

.sync-stat-value {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}

.sync-stat-value--gray { color: var(--text-primary); }
.sync-stat-value--muted { color: var(--text-secondary); }
.sync-stat-value--green { color: var(--accent-primary); }
.sync-stat-value--blue { color: var(--accent-secondary); }

/* ---------- Per game ---------- */
.sync-per-game {
  margin-top: 14px;
  padding: 12px 14px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

.sync-per-game-head {
  margin-bottom: 8px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.sync-per-game-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.sync-per-game-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11.5px;
  min-width: 0;
}

.sync-per-game-label {
  color: var(--text-secondary);
  font-weight: 600;
  white-space: nowrap;
}

.sync-per-game-val {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

/* ---------- Errors ---------- */
.sync-errors {
  margin-top: 14px;
  background: var(--bg-inset);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--accent-danger);
  border-radius: var(--radius-sm);
  overflow: hidden;
}

.sync-errors-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.12em;
  color: var(--accent-danger);
  border-bottom: 1px solid var(--border-subtle);
}

.error-led {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent-danger);
  box-shadow: 0 0 8px var(--accent-danger);
}

.sync-errors-list {
  padding: 0;
}

.sync-error-item {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 8px 14px;
  border-bottom: 1px solid var(--border-subtle);
  line-height: 1.55;
}

.sync-error-item:last-child {
  border-bottom: none;
}

.sync-error-idx {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-faint);
}

.sync-error-msg {
  min-width: 0;
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--text-secondary);
  word-break: break-word;
}

/* ---------- Skeleton ---------- */
.sync-skeleton {
  padding: 16px;
}

.sync-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.sync-skeleton-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-sm);
}

.skeleton-line {
  border-radius: var(--radius-sm);
}

.skeleton-line--stat-label {
  width: 62%;
  height: 10px;
}

.skeleton-line--stat-value {
  width: 42%;
  height: 24px;
}

/* ---------- Responsive ---------- */
@media (max-width: 1024px) {
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .sync-control {
    padding: 16px 14px;
  }

  .control-head {
    flex-direction: column;
    gap: 10px;
  }

  .control-body {
    align-items: stretch;
    flex-direction: column;
    gap: 14px;
  }

  .control-actions {
    align-items: stretch;
    flex-direction: column;
    gap: 8px;
  }

  .sync-run-btn {
    width: 100%;
    min-height: 44px;
  }

  .sync-last-time {
    white-space: normal;
    word-break: break-all;
  }

  .sync-results {
    padding: 14px;
  }

  .sync-stat-value {
    font-size: 24px;
  }

  .sync-idle {
    align-items: flex-start;
    padding: 20px 16px;
  }
}

@media (max-width: 480px) {
  .sync-stats-grid,
  .sync-skeleton-grid {
    grid-template-columns: 1fr;
  }

  .results-head {
    flex-direction: column;
    gap: 4px;
  }

  .sync-error-item {
    flex-direction: column;
    gap: 2px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .control-state.is-running .state-led,
  .sync-run-btn.is-loading :deep(.el-icon) {
    animation: none;
  }
}
</style>
