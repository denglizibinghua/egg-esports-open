<template>
  <div class="about-page">
    <div class="page-header">
      <h2 class="page-title">关于我们</h2>
    </div>

    <div class="about-card">
      <div class="about-section">
        <h3 class="about-section-title">🎮 关于 Egg 赛事竞猜</h3>
        <p class="about-text">
          Egg 赛事竞猜是一个纯粹的电子竞技预测平台，目前只覆盖 Valorant、英雄联盟、Dota 2 三大热门电竞项目。
          我们相信预测的乐趣在于公平——平台采用纯彩池制（Parimutuel），<strong>0% 抽水</strong>，你押的每一分都在奖池里。
        </p>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">✨ 核心特色</h3>
        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-icon">💰</span>
            <div class="feature-body">
              <strong>0% 抽水 · 纯彩池</strong>
              <p>不抽一分钱，猜中按比例分账，公平透明</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🏦</span>
            <div class="feature-body">
              <strong>兜底系统</strong>
              <p>积分输光？银行自动发放救济贷款，让你东山再起，但是有负债惩罚，不会干扰积分榜</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <div class="feature-body">
              <strong>三维排行榜</strong>
              <p>积分榜 · 胜率榜 · 亏损榜，纯押注实力说话</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🔒</span>
            <div class="feature-body">
              <strong>积分纯靠竞猜</strong>
              <p>无签到、无任务，积分只来自你的预测眼光</p>
            </div>
          </div>
        </div>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">📬 联系我们</h3>
        <p class="about-text">
          这是一个大学生的独立项目，如果你有任何建议、反馈或合作意向，欢迎通过以下方式联系：
        </p>
        <div class="contact-list">
          <div class="contact-item">
            <i class="el-icon-message"></i>
            <span>邮箱：contact@example.com</span>
          </div>
        </div>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">☕ 请作者喝杯咖啡</h3>
        <p class="about-text">如果你喜欢这个平台，欢迎打赏支持独立开发~</p>
        <div class="donate-qr">
          <img src="/uploads/donate-qr.png" alt="打赏二维码" class="donate-qr-img" />
          <span class="donate-qr-label">微信扫码打赏</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AboutPage'
}
</script>

<style scoped>
.about-page {
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

.about-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 32px;
  box-shadow: var(--shadow-md);
}

.about-section {
  margin-bottom: 28px;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--border-subtle);
}

.about-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.about-section-title {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.about-text {
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.8;
}

.about-text strong {
  color: var(--accent-primary);
  font-weight: 600;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-md);
  border: 1px solid var(--border-subtle);
}

.feature-icon {
  font-size: 24px;
  flex-shrink: 0;
  line-height: 1.3;
}

.feature-body strong {
  color: var(--text-primary);
  font-size: 14px;
}

.feature-body p {
  color: var(--text-tertiary);
  font-size: 13px;
  margin-top: 4px;
}

.contact-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.contact-item i {
  color: var(--accent-primary);
  font-size: 16px;
}

.contact-link {
  color: var(--accent-primary);
  text-decoration: none;
  font-weight: 500;
}

.contact-link:hover {
  text-decoration: underline;
}

/* ── Donate ── */
.donate-qr {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 20px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-lg);
  border: 1px dashed var(--border-default);
}

.donate-qr-img {
  width: 200px;
  height: 200px;
  border-radius: var(--radius-md);
  border: 2px solid var(--border-subtle);
  background: #fff;
  padding: 8px;
  object-fit: contain;
}

.donate-qr-label {
  color: var(--text-tertiary);
  font-size: 12px;
}

/* Mobile */
@media (max-width: 767px) {
  .page-title {
    font-size: 24px;
  }

  .about-card {
    padding: 20px;
  }

  .about-section-title {
    font-size: 16px;
  }

  .feature-item {
    padding: 12px;
    gap: 10px;
  }
}
</style>
