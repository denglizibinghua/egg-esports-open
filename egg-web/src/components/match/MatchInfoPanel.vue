<template>
  <!-- 顶部返回栏 + 赛事状态徽章 -->
  <div class="detail-header">
    <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
    <span v-if="match.status === 0" class="status-badge status-badge--upcoming">
      <span class="status-dot"></span>未开始
    </span>
    <span v-else-if="match.status === 1" class="status-badge status-badge--live">
      <span class="status-dot status-dot--pulse"></span>进行中
    </span>
    <span v-else-if="match.status === 2" class="status-badge status-badge--finished">已结束</span>
  </div>

  <!-- 比赛信息卡片：联赛、时间、队伍、比分、结果 -->
  <div class="match-info-card">
    <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
    <div class="match-time">{{ formatTime(match.matchTime) }}</div>

    <div class="teams-display">
      <div class="team-display team-a-display">
        <div class="team-logo-large">
          <el-image :src="match.teamALogo" alt="" fit="contain">
            <template #error><span class="team-logo-placeholder-large">{{ match.teamA === 'TBD' ? '?' : (match.teamA || '').charAt(0) }}</span></template>
          </el-image>
        </div>
        <div class="team-name-large" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
      </div>

      <div class="vs-section">
        <div class="vs-text">VS</div>
      </div>

      <div class="team-display team-b-display">
        <div class="team-logo-large">
          <el-image :src="match.teamBLogo" alt="" fit="contain">
            <template #error><span class="team-logo-placeholder-large">{{ match.teamB === 'TBD' ? '?' : (match.teamB || '').charAt(0) }}</span></template>
          </el-image>
        </div>
        <div class="team-name-large" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
      </div>
    </div>

    <!-- 比分展示（进行中/已结束且有比分时显示） -->
    <div v-if="hasScore(match)" class="score-display-large" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
      <span class="score-large-a">{{ match.scoreA }}</span>
      <span class="score-large-sep">-</span>
      <span class="score-large-b">{{ match.scoreB }}</span>
    </div>

    <!-- 比赛结果展示 -->
    <div v-if="match.status === 2 && match.result !== null && match.result !== undefined" class="match-result-display">
      <template v-if="match.result === 0">
        <span class="winner-name">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</span> 获胜!
      </template>
      <template v-else-if="match.result === 1">
        <span class="winner-name">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</span> 获胜!
      </template>
      <template v-else-if="match.result === 2">
        平局
      </template>
    </div>

    <!-- 地图比分条 — THESPIKE -->
    <div v-if="match.status === 2 && mapScores.length" class="map-chips">
      <span
        v-for="(m, idx) in mapScores"
        :key="idx"
        class="map-chip"
        :class="{ 'map-chip--win': m.winner !== null && m.winner !== undefined }"
      >{{ m.map }} {{ m.scoreA }}-{{ m.scoreB }}</span>
    </div>
  </div>
</template>

<script>
import { markRaw } from 'vue'
// Element Plus SVG 图标（迁移自 Element UI 字体图标）
import { ArrowLeft } from '@element-plus/icons-vue'

export default {
  name: 'MatchInfoPanel',
  props: {
    // 比赛对象：含 status/matchTime/teamA/teamB/scoreA/scoreB/result/tournament 等
    match: { type: Object, required: true }
  },
  data() {
    return {
      // 暴露图标组件供模板 :icon 绑定使用（markRaw 避免被响应式化）
      ArrowLeft: markRaw(ArrowLeft)
    }
  },
  computed: {
    mapScores() {
      if (this.match.mapScores && Array.isArray(this.match.mapScores)) return this.match.mapScores
      if (this.match.mapScoresJson) {
        try {
          var parsed = typeof this.match.mapScoresJson === 'string' ? JSON.parse(this.match.mapScoresJson) : this.match.mapScoresJson
          if (Array.isArray(parsed)) return parsed
        } catch (e) { /* ignore */ }
      }
      return []
    }
  },
  methods: {
    // 判断是否有比分（进行中/已结束且双方比分都不为空）
    hasScore(match) {
      return (match.status === 1 || match.status === 2) &&
        match.scoreA !== null && match.scoreA !== undefined &&
        match.scoreB !== null && match.scoreB !== undefined
    },
    // 格式化时间为 YYYY-MM-DD HH:mm
    formatTime(timeStr) {
      if (!timeStr) return ''
      var d = new Date(timeStr)
      var y = d.getFullYear()
      var month = (d.getMonth() + 1).toString().padStart(2, '0')
      var day = d.getDate().toString().padStart(2, '0')
      var hour = d.getHours().toString().padStart(2, '0')
      var min = d.getMinutes().toString().padStart(2, '0')
      return y + '-' + month + '-' + day + ' ' + hour + ':' + min
    }
  }
}
</script>

<style scoped>
.detail-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.back-btn {
  color: var(--text-tertiary) !important;
  font-size: 14px;
  transition: color var(--duration-fast) ease;
}

.back-btn:hover {
  color: var(--accent-primary) !important;
}

.match-info-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 40px 32px;
  text-align: center;
  box-shadow: var(--shadow-md);
}

.match-time {
  color: var(--text-tertiary);
  font-family: var(--font-mono);
  font-size: 13px;
  margin-bottom: 32px;
}

.teams-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  margin-bottom: 24px;
}

.team-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.team-logo-large {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-full);
  overflow: hidden;
  background: var(--bg-surface-3);
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid var(--border-default);
  transition: border-color var(--duration-normal) ease;
}

.team-logo-large img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.team-logo-placeholder-large {
  font-size: 28px;
  font-weight: 700;
  color: var(--accent-primary);
}

.team-name-large {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.vs-section {
  flex-shrink: 0;
}

.vs-text {
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--accent-primary);
  letter-spacing: 3px;
}

/* Large Score Display */
.score-display-large {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  font-family: var(--font-mono);
  font-size: 40px;
  font-weight: 800;
  margin-top: 8px;
  margin-bottom: 16px;
  font-variant-numeric: tabular-nums;
  letter-spacing: 2px;
  animation: score-reveal 0.5s var(--ease-out) both;
}

@keyframes score-reveal {
  from {
    opacity: 0;
    transform: scale(0.85);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.score--live {
  color: var(--accent-primary);
  text-shadow: 0 0 20px rgba(0, 229, 160, 0.35);
}

.score--finished {
  color: var(--text-secondary);
}

.score-large-a,
.score-large-b {
  font-variant-numeric: tabular-nums;
  min-width: 40px;
  text-align: center;
}

.score-large-sep {
  opacity: 0.4;
  font-weight: 400;
  font-size: 32px;
}

.match-result-display {
  font-size: 20px;
  font-weight: 600;
  color: var(--accent-primary);
  margin-top: 16px;
}

.winner-name {
  color: var(--accent-primary);
}

/* Map chips — THESPIKE */
.map-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}

.map-chip {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  font-size: 12px;
  font-family: var(--font-mono);
  font-weight: 600;
  color: var(--text-secondary);
  background: rgba(255,255,255,0.04);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.map-chip--win {
  border: 1px solid var(--accent-primary);
  box-shadow: 0 0 12px rgba(0,229,160,.35);
  color: var(--accent-primary);
  background: rgba(0,229,160,0.08);
}

/* TBD Team Name */
.team-name-tbd {
  color: var(--text-tertiary) !important;
  font-style: italic;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  /* 返回栏 */
  .detail-header {
    gap: 12px;
    margin-bottom: 16px;
  }

  /* 比赛信息卡片：缩小内边距 */
  .match-info-card {
    padding: 24px 16px;
  }
  .match-time {
    font-size: 12px;
    margin-bottom: 20px;
  }
  /* 队伍展示：缩小间距与 logo */
  .teams-display {
    gap: 20px;
    margin-bottom: 16px;
  }
  .team-logo-large {
    width: 56px;
    height: 56px;
  }
  .team-logo-placeholder-large {
    font-size: 22px;
  }
  .team-name-large {
    font-size: 16px;
  }
  .vs-text {
    font-size: 22px;
    letter-spacing: 2px;
  }
  /* 比分缩小 */
  .score-display-large {
    font-size: 30px;
    gap: 10px;
    letter-spacing: 1px;
  }
  .score-large-sep {
    font-size: 24px;
  }
  .match-result-display {
    font-size: 16px;
    margin-top: 12px;
  }
}

/* 小屏手机进一步收紧 */
@media (max-width: 480px) {
  .match-info-card {
    padding: 20px 12px;
  }
  .teams-display {
    gap: 12px;
  }
  .team-name-large {
    font-size: 14px;
  }
  .team-logo-large {
    width: 48px;
    height: 48px;
  }
  .vs-text {
    font-size: 18px;
  }
  .score-display-large {
    font-size: 24px;
  }
}
</style>