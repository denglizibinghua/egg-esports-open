<template>
  <span
    class="level-badge-wrap"
    :style="{ '--lv-color': getLevelColor(level) }"
    :aria-label="'等级 ' + level + ' · 累计 ' + safeCount + ' 场'"
  >
    <span class="level-badge">
      <span class="lv-prefix">LV.</span>
      <span class="lv-num">{{ levelText }}</span>
    </span>
    <span v-if="showProgress" class="level-rail" aria-hidden="true">
      <span class="level-rail-fill" :style="{ width: progress + '%' }"></span>
    </span>
  </span>
</template>

<script>
import { getLevel, getLevelColor, getLevelProgress } from '@/utils/level'

export default {
  name: 'LevelBadge',
  props: {
    count: { type: Number, required: true, default: 0 },
    showProgress: { type: Boolean, default: true }
  },
  computed: {
    safeCount() {
      var n = Number(this.count)
      if (!Number.isFinite(n) || n < 0) return 0
      return Math.floor(n)
    },
    level() {
      return getLevel(this.safeCount)
    },
    levelText() {
      return String(this.level).padStart(2, '0')
    },
    progress() {
      return getLevelProgress(this.safeCount)
    }
  },
  methods: {
    getLevelColor(lv) {
      return getLevelColor(lv) || 'var(--brand)'
    }
  }
}
</script>

<style scoped>
.level-badge-wrap {
  --lv-color: var(--text-tertiary);
  display: inline-flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: fit-content;
  max-width: 100%;
  vertical-align: middle;
  line-height: 1;
}

.level-badge {
  display: inline-flex;
  align-items: baseline;
  gap: 0;
  height: 18px;
  padding: 0 6px 0 7px;
  background: var(--bg-surface-2);
  background: color-mix(in srgb, var(--lv-color) 13%, var(--bg-surface-2));
  border: 1px solid var(--border-default);
  border-color: color-mix(in srgb, var(--lv-color) 42%, transparent);
  border-radius: var(--radius-sm);
  box-shadow: inset 2px 0 0 var(--lv-color);
  color: var(--text-primary);
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1;
  white-space: nowrap;
  flex-shrink: 0;
  overflow: hidden;
}

.lv-prefix {
  color: var(--text-tertiary);
  font-size: 8.5px;
  letter-spacing: 0.02em;
}

.lv-num {
  color: var(--lv-color);
  font-size: 10.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  letter-spacing: 0.01em;
}

.level-rail {
  display: block;
  width: 52px;
  height: 3px;
  background: var(--bg-surface-3);
  border-radius: 0;
  overflow: hidden;
  flex-shrink: 0;
}

.level-rail-fill {
  display: block;
  height: 100%;
  background: var(--lv-color);
  opacity: 0.9;
  transition: width 0.4s var(--ease-out);
}
</style>
