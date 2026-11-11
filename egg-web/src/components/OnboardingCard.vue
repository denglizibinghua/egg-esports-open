<template>
  <div class="onboarding-card" :class="{ 'onboarding-card--open': !collapsed }" v-if="visible">
    <div class="onboarding-head" @click="collapsed = !collapsed">
      <div class="onboarding-head-main">
        <span class="onboarding-mark">//</span>
        <span class="onboarding-eyebrow">BRIEFING</span>
        <span class="onboarding-title">怎么玩</span>
        <span class="onboarding-meta">3 步 · 0% 抽水</span>
      </div>
      <span class="onboarding-toggle">
        {{ collapsed ? '展开' : '收起' }}
        <span class="onboarding-caret" aria-hidden="true"></span>
      </span>
    </div>

    <div class="onboarding-body" v-show="!collapsed">
      <div class="onboarding-steps">
        <div class="onboarding-step">
          <span class="step-number">01</span>
          <div class="step-content">
            <strong>浏览赛事</strong>
            <p>在赛程中选择你熟悉的电竞比赛，Valorant / 英雄联盟 / Dota 2</p>
          </div>
        </div>
        <div class="onboarding-step">
          <span class="step-number">02</span>
          <div class="step-content">
            <strong>查看赔率</strong>
            <p>赔率越高猜中回报越大。鼠标悬停赔率旁的 <span style="color:var(--text-tertiary)">?</span> 了解详情</p>
          </div>
        </div>
        <div class="onboarding-step">
          <span class="step-number">03</span>
          <div class="step-content">
            <strong>押注积分</strong>
            <p>猜中按彩池分账，0%抽水；猜错失去积分。纯靠判断力</p>
          </div>
        </div>
      </div>
      <div class="onboarding-footer">
        <router-link to="/about" class="onboarding-link">了解更多 →</router-link>
        <el-button link size="small" class="onboarding-dismiss" @click="dismiss">不再显示</el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OnboardingCard',
  data() {
    return {
      visible: false,
      collapsed: true
    }
  },
  created() {
    var dismissed = localStorage.getItem('egg_onboarding_dismissed')
    if (!dismissed) {
      this.visible = true
    }
  },
  methods: {
    dismiss() {
      this.visible = false
      localStorage.setItem('egg_onboarding_dismissed', '1')
    }
  }
}
</script>

<style scoped>
.onboarding-card {
  margin-bottom: 16px;
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.018), transparent 44%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}

.onboarding-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 44px;
  padding: 0 16px;
  cursor: pointer;
  user-select: none;
  background: linear-gradient(90deg, rgba(255, 70, 85, 0.055), transparent 52%);
  box-shadow: inset 2px 0 0 var(--brand);
  transition: background var(--duration-fast) ease;
}

.onboarding-head:hover {
  background: linear-gradient(90deg, rgba(255, 70, 85, 0.085), transparent 58%);
}

.onboarding-head-main {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.onboarding-mark {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
}

.onboarding-eyebrow {
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}

.onboarding-title {
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: var(--text-primary);
  white-space: nowrap;
}

.onboarding-meta {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.onboarding-toggle {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
}

.onboarding-caret {
  width: 5px;
  height: 5px;
  border-right: 1px solid currentColor;
  border-bottom: 1px solid currentColor;
  transform: rotate(45deg) translateY(-1px);
  transition: transform var(--duration-normal) var(--ease-out);
}

.onboarding-card--open .onboarding-caret {
  transform: rotate(225deg) translateY(-1px);
}

.onboarding-body {
  padding: 0 16px 14px;
}

.onboarding-steps {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid var(--border-subtle);
}

.onboarding-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-width: 0;
  padding: 14px 16px 4px 0;
}

.onboarding-step + .onboarding-step {
  padding-left: 16px;
  border-left: 1px solid var(--border-subtle);
}

.step-number {
  flex-shrink: 0;
  padding-top: 2px;
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
}

.step-content {
  min-width: 0;
}

.step-content strong {
  display: block;
  margin-bottom: 2px;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.step-content p {
  margin: 0;
  font-size: 12px;
  line-height: 1.55;
  color: var(--text-tertiary);
}

.onboarding-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
}

.onboarding-link {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--brand);
  transition: color var(--duration-fast) ease;
}

.onboarding-link:hover {
  color: var(--brand-hot);
}

.onboarding-card .onboarding-dismiss {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

.onboarding-card .onboarding-dismiss:hover {
  color: var(--brand);
}

@media (max-width: 768px) {
  .onboarding-steps {
    grid-template-columns: minmax(0, 1fr);
  }
  .onboarding-step {
    padding: 12px 0 0;
  }
  .onboarding-step + .onboarding-step {
    margin-top: 10px;
    padding-left: 0;
    padding-top: 10px;
    border-left: 0;
    border-top: 1px solid var(--border-subtle);
  }
}

@media (max-width: 480px) {
  .onboarding-head {
    padding: 0 12px;
  }
  .onboarding-meta {
    display: none;
  }
  .onboarding-body {
    padding: 0 12px 12px;
  }
}
</style>
