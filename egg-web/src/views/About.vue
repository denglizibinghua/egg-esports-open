<template>
  <div class="about-page">
    <div class="page-header">
      <h2 class="page-title">关于我们</h2>
    </div>

    <div class="about-card">
      <div class="about-section">
        <h3 class="about-section-title">🎮 关于 Egg 赛事竞猜</h3>
        <p class="about-text">
          Egg Esports 是一个独立的电竞赛事预测平台，专注瓦洛兰特。平台采用纯彩池制（Parimutuel），<strong>0% 抽水</strong>——不设庄家、不抽一分钱，赔率完全由玩家投注决定。你押的每一分都在奖池里，猜中按比例分账。积分永远是积分，不可变现——这不是博彩，是判断力的竞技场。
        </p>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">✨ 核心特色</h3>
        <div class="feature-list">
          <div class="feature-item">
            <span class="feature-icon">🎯</span>
            <div class="feature-body">
              <strong>积分纯靠竞猜</strong>
              <p>无签到、无任务、无额外获取方式，积分只来自你的预测眼光</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🛟</span>
            <div class="feature-body">
              <strong>兜底系统</strong>
              <p>0 积分可领取贷款和低保，让你东山再起的机会</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🏆</span>
            <div class="feature-body">
              <strong>0% 抽水 · 纯彩池</strong>
              <p>不抽一分钱，纯粹的对赌，猜中按比例分账</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📈</span>
            <div class="feature-body">
              <strong>净收益榜</strong>
              <p>只计算押注输赢，低保和贷款不计入排名。能上榜，全凭眼光</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">♾️</span>
            <div class="feature-body">
              <strong>单次押注不设限</strong>
              <p>没有上限，想押多少押多少</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">⏱️</span>
            <div class="feature-body">
              <strong>押注截止前为准</strong>
              <p>结算以最后截止前为准，绝不提前锁定</p>
            </div>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🛡️</span>
            <div class="feature-body">
              <strong>非博彩 · 纯竞技</strong>
              <p>积分不可充值、不可提现，竞赛纯粹靠判断力</p>
            </div>
          </div>
        </div>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">💼 接单说明 · 3天可交付同款</h3>
        <p class="about-text" style="margin-bottom:12px">
          本站为<strong>可复刻的瓦洛兰特预测社区演示站</strong>（已交付 32 场 · 0%抽水纯彩池），支持二次定制，<strong>3天可交付同款</strong>。价格与 <router-link to="/pricing" style="color:var(--accent-primary);text-decoration:none;font-weight:600">/pricing</router-link> 一致：
        </p>
        <div class="order-list">
          <div class="order-item">
            <span class="order-dot"></span>
            <div class="order-body">
              <strong>复刻演示站 · 8000 元起</strong>
              <span>含部署 + 域名 + 首季赛程导入 + 机器人托底，不含菠菜/现金兑付定制</span>
            </div>
          </div>
          <div class="order-item">
            <span class="order-dot"></span>
            <div class="order-body">
              <strong>单场代办 · 800–2000 元 / 场</strong>
              <span>含海报 + 赛程录入 + 榜单结算 + 战报图 9 张，不含线下执行/裁判差旅</span>
            </div>
          </div>
          <div class="order-item">
            <span class="order-dot"></span>
            <div class="order-body">
              <strong>付款 4:5:1 · 3天交付</strong>
              <span>定金 40% / 交付 50% / 质保 10%，常规需求 3 个工作日内上线演示站</span>
            </div>
          </div>
        </div>
        <p class="about-text" style="margin-top:10px;font-size:12px;color:var(--text-tertiary)">
          演示账号 <code>demo_player/demo123</code> 可先体验全流程，再谈定制 → <a href="mailto:contact@example.com" style="color:var(--accent-primary);text-decoration:none">contact@example.com</a>
        </p>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">📬 联系我们</h3>
        <p class="about-text">
          Egg Esports 由独立开发者维护。如果你有任何建议、反馈或合作意向，欢迎通过以下方式联系：
        </p>
        <div class="contact-list">
          <div class="contact-item">
            <!-- Element Plus：字体图标改为 SVG 组件 -->
            <el-icon><Message /></el-icon>
            <span>邮箱：contact@example.com</span>
          </div>
        </div>
      </div>

      <div class="about-section">
        <h3 class="about-section-title">☕ 请作者喝杯咖啡</h3>
        <p class="about-text">如果你喜欢这个平台，欢迎支持独立开发（纯自愿，不影响任何功能）</p>
        <div class="donate-qr">
          <el-image :src="'/uploads/donate-qr.png'" alt="打赏二维码" class="donate-qr-img" fit="contain">
            <template #error><span class="img-fallback">二维码</span></template>
          </el-image>
          <span class="donate-qr-label">微信扫码打赏</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// Element Plus 图标按需引入（用于模板中的 <Message /> 标签）
import { Message } from '@element-plus/icons-vue'

export default {
  name: 'AboutPage',
  components: { Message }
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

/* ── 接单说明 ── */
.order-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.order-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
}
.order-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-primary);
  margin-top: 7px;
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(0, 229, 160, 0.5);
}
.order-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.order-body strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 600;
}
.order-body span {
  color: var(--text-tertiary);
  font-size: 12px;
  line-height: 1.6;
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
