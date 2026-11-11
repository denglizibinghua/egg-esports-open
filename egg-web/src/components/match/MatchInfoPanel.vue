<template>
  <!-- 顶部返回栏 + 赛事状态徽章 -->
  <div class="panel-toolbar">
    <el-button link :icon="ArrowLeft" class="back-btn" @click="$router.back()">返回</el-button>
    <span v-if="match.status === 0" class="status-badge status-badge--upcoming">
      <span class="status-dot"></span>未开始
    </span>
    <span v-else-if="match.status === 1" class="status-badge status-badge--live">
      <span class="status-dot status-dot--pulse"></span>进行中
    </span>
    <span v-else-if="match.status === 2" class="status-badge status-badge--finished">已结束</span>
  </div>

  <!-- 广播比分板：联赛 + 时间、双方队伍、比分/VS、结果、地图比分 -->
  <div class="match-info-card" :class="{ 'match-info-card--live': match.status === 1 }">
    <div class="card-top">
      <span class="tournament-chip">{{ match.tournament || '常规赛事' }}</span>
      <span class="match-time">{{ formatTime(match.matchTime) }}</span>
    </div>

    <div class="teams-display">
      <div class="team-display team-a-display">
        <div class="team-logo-large">
          <el-image :src="fixLogo(match.teamALogo)" alt="" fit="contain">
            <template #error><span class="team-logo-placeholder-large">{{ match.teamA === 'TBD' ? '?' : (match.teamA || '').charAt(0) }}</span></template>
          </el-image>
        </div>
        <div class="team-name-large" :class="{ 'team-name-tbd': match.teamA === 'TBD' }">{{ match.teamA === 'TBD' ? '待定' : match.teamA }}</div>
      </div>

      <div class="sb-center">
        <div v-if="hasScore(match)" class="score-display-large" :class="{ 'score--live': match.status === 1, 'score--finished': match.status === 2 }">
          <span class="score-large-a">{{ match.scoreA }}</span>
          <span class="score-large-sep">-</span>
          <span class="score-large-b">{{ match.scoreB }}</span>
        </div>
        <div v-else class="vs-text">VS</div>
      </div>

      <div class="team-display team-b-display">
        <div class="team-logo-large">
          <el-image :src="fixLogo(match.teamBLogo)" alt="" fit="contain">
            <template #error><span class="team-logo-placeholder-large">{{ match.teamB === 'TBD' ? '?' : (match.teamB || '').charAt(0) }}</span></template>
          </el-image>
        </div>
        <div class="team-name-large" :class="{ 'team-name-tbd': match.teamB === 'TBD' }">{{ match.teamB === 'TBD' ? '待定' : match.teamB }}</div>
      </div>
    </div>

    <!-- 比赛结果展示（胜者横幅：薄荷绿） -->
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
import { fixLogoUrl } from '@/utils/format'

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
    fixLogo(url) { return fixLogoUrl(url) },
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
/* ── 顶部工具栏：返回 + 状态 ── */
.panel-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 30px;
  margin-bottom: 14px;
}

.panel-toolbar :deep(.el-button.back-btn) {
  color: var(--text-tertiary);
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
  transition: color var(--duration-fast) ease;
}

.panel-toolbar :deep(.el-button.back-btn:hover) {
  color: var(--brand);
}

/* ── 广播比分板 ── */
.match-info-card {
  position: relative;
  padding: 22px 24px 20px;
  background-color: var(--bg-surface-1);
  background-image: linear-gradient(180deg, var(--bg-surface-2) 0%, transparent 55%);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.match-info-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 24px;
  width: 64px;
  height: 2px;
  background: var(--brand);
  pointer-events: none;
}

.match-info-card--live {
  border-color: var(--brand-line);
  background-image: linear-gradient(180deg, var(--brand-soft) 0%, transparent 48%);
}

.match-info-card--live::before {
  width: calc(100% - 48px);
  background: linear-gradient(90deg, var(--brand), var(--brand-soft) 42%, transparent 82%);
}

.card-top {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding-bottom: 18px;
  margin-bottom: 22px;
  border-bottom: 1px solid var(--border-subtle);
  text-align: center;
}

.card-top .tournament-chip {
  max-width: min(100%, 320px);
  font-size: 10px;
  letter-spacing: 0.08em;
}

.match-time {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.teams-display {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 18px;
}

.team-display {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* 80px 队伍 Logo 板 */
.team-logo-large {
  position: relative;
  width: 80px;
  height: 80px;
  padding: 10px;
  border-radius: var(--radius-lg);
  background-color: var(--bg-surface-3);
  background-image: linear-gradient(160deg, var(--bg-surface-4) 0%, var(--bg-surface-2) 100%);
  border: 1px solid var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.team-logo-large::after {
  content: '';
  position: absolute;
  left: 50%;
  bottom: 5px;
  width: 18px;
  height: 2px;
  transform: translateX(-50%);
  background: var(--brand);
  opacity: 0.85;
}

.team-b-display .team-logo-large::after {
  background: var(--team-b);
}

.team-logo-large .el-image {
  width: 100%;
  height: 100%;
}

.team-logo-placeholder-large {
  font-family: var(--font-display);
  font-size: 26px;
  letter-spacing: 0.04em;
  color: var(--text-secondary);
}

.team-name-large {
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -0.01em;
  color: var(--text-primary);
  text-align: center;
  max-width: 100%;
  overflow: hidden;
  overflow-wrap: anywhere;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

/* 中央：比分或 VS */
.sb-center {
  align-self: start;
  display: grid;
  place-items: center;
  height: 80px;
  min-width: 64px;
  padding: 0 2px;
}

.score-display-large {
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-weight: 800;
  font-size: 44px;
  line-height: 1;
  letter-spacing: -0.04em;
}

.score--live {
  color: var(--brand);
  text-shadow: 0 0 18px var(--brand-line);
}

.score--finished {
  color: var(--text-primary);
}

.score-large-a,
.score-large-b {
  min-width: 28px;
  text-align: center;
}

.score-large-sep {
  font-size: 26px;
  font-weight: 400;
  color: var(--text-faint);
  transform: translateY(-2px);
}

.vs-text {
  font-family: var(--font-display);
  font-size: 26px;
  line-height: 1;
  letter-spacing: 0.14em;
  color: var(--text-secondary);
  padding: 7px 10px 5px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  background: var(--bg-surface-2);
}

/* 薄荷绿胜者横幅 */
.match-result-display {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 10px;
  max-width: 460px;
  margin: 22px auto 0;
  padding: 9px 16px;
  background: var(--accent-primary-soft);
  border: 1px solid var(--border-subtle);
  border-left: 2px solid var(--accent-primary);
  border-radius: var(--radius-sm);
  color: var(--accent-primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-align: center;
}

.match-result-display::before {
  content: 'RESULT';
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.18em;
  color: var(--text-tertiary);
}

.winner-name {
  color: var(--accent-primary);
}

/* 地图比分条 */
.map-chips {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}

/* TBD Team Name */
.team-name-tbd {
  color: var(--text-tertiary);
  font-style: italic;
}

/* ===== 移动端适配 ===== */
@media (max-width: 768px) {
  .panel-toolbar {
    gap: 10px;
    margin-bottom: 12px;
  }

  .match-info-card {
    padding: 18px 14px 16px;
  }

  .match-info-card::before {
    left: 14px;
  }

  .match-info-card--live::before {
    width: calc(100% - 28px);
  }

  .card-top {
    padding-bottom: 14px;
    margin-bottom: 18px;
  }

  .teams-display {
    gap: 12px;
  }

  .team-logo-large {
    width: 68px;
    height: 68px;
    padding: 8px;
  }

  .sb-center {
    height: 68px;
    min-width: 56px;
  }

  .team-name-large {
    font-size: 15px;
  }

  .score-display-large {
    font-size: 34px;
  }

  .vs-text {
    font-size: 21px;
  }

  .match-result-display {
    margin-top: 18px;
  }
}

@media (max-width: 480px) {
  .match-info-card {
    padding: 16px 12px 14px;
  }

  .teams-display {
    gap: 8px;
  }

  .team-logo-large {
    width: 62px;
    height: 62px;
    padding: 7px;
    border-radius: var(--radius-md);
  }

  .sb-center {
    height: 62px;
    min-width: 48px;
  }

  .team-name-large {
    font-size: 13.5px;
  }

  .score-display-large {
    font-size: 30px;
    letter-spacing: -0.05em;
  }

  .score-large-sep {
    font-size: 20px;
  }

  .score-large-a,
  .score-large-b {
    min-width: 20px;
  }

  .vs-text {
    font-size: 18px;
    padding: 6px 8px 4px;
  }

  .map-chips {
    gap: 6px;
  }
}
</style>
