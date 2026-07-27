<template>
  <div class="onboarding-card" v-if="visible">
    <div class="onboarding-header" @click="collapsed = !collapsed">
      <span class="onboarding-title">🎮 怎么玩</span>
      <span class="onboarding-toggle">{{ collapsed ? '展开' : '收起' }}</span>
    </div>
    <div class="onboarding-body" v-show="!collapsed">
      <div class="onboarding-steps">
        <div class="onboarding-step">
          <span class="step-number">1</span>
          <div class="step-content">
            <strong>浏览赛事</strong>
            <p>在赛程中选择你熟悉的电竞比赛，Valorant / 英雄联盟 / Dota 2</p>
          </div>
        </div>
        <div class="onboarding-step">
          <span class="step-number">2</span>
          <div class="step-content">
            <strong>查看赔率</strong>
            <p>赔率越高猜中回报越大。鼠标悬停赔率旁的 <span style="color:var(--text-tertiary)">?</span> 了解详情</p>
          </div>
        </div>
        <div class="onboarding-step">
          <span class="step-number">3</span>
          <div class="step-content">
            <strong>押注积分</strong>
            <p>猜中按彩池分账，0%抽水；猜错失去积分。纯靠判断力</p>
          </div>
        </div>
      </div>
      <div class="onboarding-footer">
        <router-link to="/about" class="onboarding-link">了解更多 →</router-link>
        <el-button link size="small" @click="dismiss">不再显示</el-button>
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
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  margin-bottom: 20px;
  overflow: hidden;
  transition: all var(--duration-normal) var(--ease-out);
}
.onboarding-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 20px; cursor: pointer; user-select: none;
  background: linear-gradient(90deg, rgba(0,229,160,0.06), transparent);
}
.onboarding-title { font-size: 16px; font-weight: 600; color: var(--accent-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.onboarding-toggle { font-size: 12px; color: var(--text-tertiary); flex-shrink: 0; margin-left: 8px; }
.onboarding-body { padding: 0 20px 20px; }
.onboarding-steps { display: flex; flex-direction: column; gap: 16px; margin-bottom: 16px; }
.onboarding-step { display: flex; gap: 14px; align-items: flex-start; }
.step-number {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--accent-primary); color: #0a0a0f;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px; flex-shrink: 0;
}
.step-content strong { color: var(--text-primary); font-size: 14px; }
.step-content p { color: var(--text-tertiary); font-size: 13px; margin-top: 2px; line-height: 1.5; }
.onboarding-footer { display: flex; justify-content: space-between; align-items: center; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
.onboarding-link { color: var(--accent-primary); font-size: 13px; text-decoration: none; }
.onboarding-link:hover { text-decoration: underline; }
</style>
