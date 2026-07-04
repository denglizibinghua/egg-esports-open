<template>
  <div class="legal-page">
    <div class="page-header">
      <h2 class="page-title">服务条款</h2>
    </div>

    <div class="legal-card">
      <div class="legal-section">
        <h3 class="legal-section-title">📋 说明</h3>
        <p class="legal-text">
          本页面为服务条款占位页面，完整法律文本将在正式上线前补充。
          Egg Esports 是非盈利性质的电竞赛事预测平台，积分不可充值、不可提现，
          不构成任何形式的博彩或金融业务。
        </p>
      </div>

      <div class="legal-section">
        <h3 class="legal-section-title">⚖️ 服务性质</h3>
        <p class="legal-text">
          平台提供的所有积分、押注、排行等功能均为娱乐竞技用途，
          积分无任何现实货币价值，不可兑换、提现或转让。
        </p>
      </div>

      <div class="legal-section">
        <h3 class="legal-section-title">📜 用户行为规范</h3>
        <p class="legal-text">
          用户应遵守国家法律法规，禁止利用平台从事任何违法违规活动。
          平台有权对违规账号进行封禁处理。
        </p>
      </div>

      <div class="legal-section">
        <h3 class="legal-section-title">📬 联系我们</h3>
        <p class="legal-text">
          如对本服务条款有任何疑问，请联系：contact@eggesports.com
        </p>
      </div>

      <div class="legal-placeholder">
        <el-alert
          title="完整服务条款文本待补充"
          type="info"
          description="本页面为占位内容，正式法律文本将在产品上线前由专业法务人员审定后替换。"
          :closable="false"
          show-icon
        />
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TermsPage'
}
</script>

<style scoped>
.legal-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 16px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

.legal-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

.legal-section {
  margin-bottom: 28px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--border-subtle);
}

.legal-section:last-of-type {
  margin-bottom: 16px;
}

.legal-section-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.legal-text {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.legal-placeholder {
  margin-top: 8px;
}

@media (max-width: 767px) {
  .page-title {
    font-size: 24px;
  }

  .legal-card {
    padding: 20px;
  }

  .legal-section-title {
    font-size: 16px;
  }
}
</style>
