<template>
  <div class="legal-page">
    <header class="legal-header">
      <span class="legal-eyebrow">// LEGAL · 平台条款</span>
      <h1 class="legal-title">服务条款</h1>
      <div class="legal-meta">
        <span>LAST UPDATED · 2026-11-11</span>
        <span class="meta-sep">/</span>
        <span>EGG ESPORTS</span>
      </div>
    </header>

    <article class="legal-body">
      <section class="legal-section">
        <div class="section-head">
          <span class="section-index">01</span>
          <h2 class="legal-section-title">说明</h2>
        </div>
        <p class="legal-text">
          本页面为服务条款占位页面，完整法律文本将在正式上线前补充。
          Egg Esports 是非盈利性质的电竞赛事预测平台，积分不可充值、不可提现，
          不构成任何形式的博彩或金融业务。
        </p>
      </section>

      <section class="legal-section">
        <div class="section-head">
          <span class="section-index">02</span>
          <h2 class="legal-section-title">服务性质</h2>
        </div>
        <p class="legal-text">
          平台提供的所有积分、押注、排行等功能均为娱乐竞技用途，
          积分无任何现实货币价值，不可兑换、提现或转让。
        </p>
      </section>

      <section class="legal-section">
        <div class="section-head">
          <span class="section-index">03</span>
          <h2 class="legal-section-title">用户行为规范</h2>
        </div>
        <p class="legal-text">
          用户应遵守国家法律法规，禁止利用平台从事任何违法违规活动。
          平台有权对违规账号进行封禁处理。
        </p>
      </section>

      <section class="legal-section">
        <div class="section-head">
          <span class="section-index">04</span>
          <h2 class="legal-section-title">联系我们</h2>
        </div>
        <p class="legal-text">
          如对本服务条款有任何疑问，请通过 GitHub Issues 反馈：https://github.com/denglizibinghua/egg-esports-open/issues
        </p>
      </section>

      <div class="legal-placeholder">
        <el-alert
          title="完整服务条款文本待补充"
          type="info"
          description="本页面为占位内容，正式法律文本将在产品上线前由专业法务人员审定后替换。"
          :closable="false"
          show-icon
        />
      </div>
    </article>
  </div>
</template>

<script>
export default {
  name: 'TermsPage'
}
</script>

<style scoped>
.legal-page {
  max-width: 760px;
  margin: 0 auto;
  padding: 4px 0 32px;
}

/* ---------- Header ---------- */
.legal-header {
  position: relative;
  padding-bottom: 22px;
  margin-bottom: 4px;
  border-bottom: 1px solid var(--border-subtle);
}

.legal-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 48px;
  height: 2px;
  background: var(--brand);
}

.legal-eyebrow {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 10px;
}

.legal-title {
  font-family: var(--font-body);
  font-size: 34px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.15;
  color: var(--text-primary);
}

.legal-meta {
  display: flex;
  align-items: center;
  gap: 9px;
  flex-wrap: wrap;
  margin-top: 12px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--text-faint);
}

.meta-sep {
  color: var(--border-strong);
}

/* ---------- Prose ---------- */
.legal-body {
  padding-top: 4px;
}

.legal-section {
  padding-top: 28px;
  margin-top: 28px;
  border-top: 1px solid var(--border-subtle);
}

.legal-section:first-of-type {
  border-top: 0;
  margin-top: 24px;
  padding-top: 0;
}

.section-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 12px;
}

.section-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
  flex-shrink: 0;
}

.legal-section-title {
  font-family: var(--font-body);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.4;
  color: var(--text-primary);
}

.legal-text {
  font-size: 15px;
  line-height: 1.8;
  letter-spacing: 0.012em;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}

.legal-placeholder {
  margin-top: 32px;
}

.legal-placeholder :deep(.el-alert) {
  padding: 12px 14px;
}

/* ---------- Responsive ---------- */
@media (max-width: 767px) {
  .legal-title {
    font-size: 28px;
  }

  .legal-section {
    padding-top: 24px;
    margin-top: 24px;
  }

  .legal-section-title {
    font-size: 16px;
  }

  .legal-text {
    font-size: 14.5px;
    line-height: 1.8;
  }

  .legal-meta {
    font-size: 9px;
    letter-spacing: 0.1em;
  }
}

@media (max-width: 400px) {
  .legal-title {
    font-size: 25px;
  }

  .legal-text {
    font-size: 14px;
  }
}
</style>
