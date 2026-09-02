<template>
  <div class="pricing-page">
    <div class="page-header">
      <h2 class="page-title">定价 · 接单方案</h2>
      <p class="page-subtitle">已交付 32 场瓦洛兰特演示赛 · 0%抽水纯彩池 · 3天可交付同款</p>
    </div>

    <!-- 两档主卡 -->
    <div class="pricing-grid">
      <!-- 复刻演示站 -->
      <div class="pricing-card pricing-card--featured">
        <div class="pricing-card-tag">热门</div>
        <div class="pricing-card-head">
          <h3 class="pricing-card-title">复刻演示站</h3>
          <div class="pricing-card-price"><span class="price-num">8000</span><span class="price-unit"> 元起</span></div>
          <p class="pricing-card-note">部署 + 域名 + 首季赛程导入 + 机器人托底，不含菠菜</p>
        </div>
        <ul class="pricing-list">
          <li><span class="dot"></span>整站部署到你的域名/服务器</li>
          <li><span class="dot"></span>首季赛程一键导入（含瓦洛兰特演示数据）</li>
          <li><span class="dot"></span>机器人托底：无人下注时自动填充彩池与弹幕</li>
          <li><span class="dot"></span>含后台管理、排行榜、评论区、银行兜底全链路</li>
          <li><span class="dot"></span>3 天交付，交付后 7 天免费小修</li>
        </ul>
        <div class="pricing-card-actions">
          <el-button type="primary" class="btn-hover-lift pricing-cta" @click="$router.push('/login')">立即体验演示</el-button>
          <a href="mailto:contact@example.com" class="pricing-mail">定制咨询 → contact@example.com</a>
        </div>
      </div>

      <!-- 单场代办 -->
      <div class="pricing-card">
        <div class="pricing-card-head">
          <h3 class="pricing-card-title">单场代办</h3>
          <div class="pricing-card-price"><span class="price-num">800–2000</span><span class="price-unit"> 元 / 场</span></div>
          <p class="pricing-card-note">按场计费，适合零散赛事/校内杯赛</p>
        </div>
        <ul class="pricing-list">
          <li><span class="dot"></span>海报设计 1 张（适配朋友圈/群公告）</li>
          <li><span class="dot"></span>赛程录入 + 对阵配置 + 直播源绑定</li>
          <li><span class="dot"></span>榜单结算 + 积分彩池清分</li>
          <li><span class="dot"></span>战报图 9 张（赛后一键出图，含比分/高光）</li>
          <li><span class="dot"></span>可选：解说词条/弹幕预热（加量议价）</li>
        </ul>
        <div class="pricing-card-actions">
          <el-button class="pricing-cta" @click="$router.push('/login')">查看演示赛程</el-button>
          <a href="mailto:contact@example.com" class="pricing-mail">单场咨询 → contact@example.com</a>
        </div>
      </div>
    </div>

    <!-- 定金比例 + 说明 -->
    <div class="pricing-card pricing-card--wide">
      <h3 class="about-section-title" style="margin-bottom:10px">💰 付款方式 · 定金 4:5:1</h3>
      <div class="pay-steps">
        <div class="pay-step">
          <div class="pay-step-num">4</div>
          <div class="pay-step-label">定金 40%</div>
          <div class="pay-step-desc">确认需求后支付，锁定排期</div>
        </div>
        <div class="pay-sep">:</div>
        <div class="pay-step">
          <div class="pay-step-num">5</div>
          <div class="pay-step-label">交付 50%</div>
          <div class="pay-step-desc">演示站可访问/首场可开赛</div>
        </div>
        <div class="pay-sep">:</div>
        <div class="pay-step">
          <div class="pay-step-num">1</div>
          <div class="pay-step-label">尾款 10%</div>
          <div class="pay-step-desc">验收后 7 天内结清</div>
        </div>
      </div>
      <p class="pricing-footnote">不含菠菜/现金博彩相关功能；单场代办可单场起订，复刻站含首季赛程，超出另计。开票需另议。</p>
      <div class="pricing-wide-actions">
        <el-button type="primary" class="btn-hover-lift" @click="$router.push('/login')">先用演示账号体验</el-button>
        <a href="mailto:contact@example.com" class="contact-link">商务邮箱 → contact@example.com</a>
      </div>
    </div>

    <!-- FAQ / 交付清单小字 -->
    <div class="pricing-card pricing-card--wide">
      <h3 class="about-section-title">📦 交付物清单</h3>
      <div class="deliver-grid">
        <div class="deliver-item"><strong>域名与部署</strong><span>含 Nginx/HTTPS 配置说明，源码交付可选</span></div>
        <div class="deliver-item"><strong>赛程与榜单</strong><span>首季导入 + 榜单/结算演示数据</span></div>
        <div class="deliver-item"><strong>机器人托底</strong><span>彩池与评论区无人时自动兜底</span></div>
        <div class="deliver-item"><strong>海报与战报图</strong><span>单场代办含 1 张海报 + 9 张战报图</span></div>
        <div class="deliver-item"><strong>3 天交付</strong><span>常规需求 3 个工作日内上线演示站</span></div>
        <div class="deliver-item"><strong>不含菠菜</strong><span>积分不可变现，仅为校园赛预测玩法</span></div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PricingPage'
}
</script>

<style scoped>
.pricing-page {
  max-width: 960px;
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
.page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 6px;
}

/* 2卡网格：桌面3列→平板2列→手机1列（按要求 3→2→1，配合下方宽卡共3列视觉） */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
.pricing-card {
  background: var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 22px;
  box-shadow: var(--shadow-md);
  position: relative;
  display: flex;
  flex-direction: column;
}
.pricing-card--featured {
  border-color: rgba(0, 229, 160, 0.30);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45), 0 0 0 1px rgba(0, 229, 160, 0.12);
}
.pricing-card--wide {
  margin-bottom: 16px;
}
.pricing-card-tag {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.06em;
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--accent-primary);
  color: #0a0a0f;
}
.pricing-card-head {
  margin-bottom: 14px;
}
.pricing-card-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}
.pricing-card-price {
  margin-top: 6px;
  line-height: 1;
}
.price-num {
  font-family: var(--font-display);
  font-size: 30px;
  color: var(--accent-primary);
  letter-spacing: 0.5px;
}
.price-unit {
  font-size: 13px;
  color: var(--text-tertiary);
  margin-left: 4px;
}
.pricing-card-note {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 6px;
  line-height: 1.5;
}
.pricing-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 9px;
  flex: 1;
}
.pricing-list li {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}
.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-primary);
  margin-top: 7px;
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(0, 229, 160, 0.5);
}
.pricing-card-actions {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.pricing-cta {
  width: 100%;
}
.pricing-mail {
  font-size: 12px;
  color: var(--accent-primary);
  text-decoration: none;
  text-align: center;
}
.pricing-mail:hover {
  text-decoration: underline;
}
.about-section-title {
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 600;
}
.pay-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.pay-step {
  flex: 1;
  min-width: 120px;
  text-align: center;
  padding: 14px 10px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}
.pay-step-num {
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--accent-primary);
  line-height: 1;
}
.pay-step-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-top: 4px;
}
.pay-step-desc {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.pay-sep {
  font-size: 22px;
  color: var(--text-tertiary);
  font-weight: 700;
}
.pricing-footnote {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 12px;
  line-height: 1.6;
  text-align: center;
}
.pricing-wide-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
  flex-wrap: wrap;
  justify-content: center;
}
.contact-link {
  font-size: 13px;
  color: var(--accent-primary);
  text-decoration: none;
}
.contact-link:hover {
  text-decoration: underline;
}
.deliver-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-top: 12px;
}
.deliver-item {
  padding: 12px;
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.deliver-item strong {
  font-size: 13px;
  color: var(--text-primary);
}
.deliver-item span {
  font-size: 11px;
  color: var(--text-tertiary);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .deliver-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 600px) {
  .page-title {
    font-size: 24px;
  }
  .pricing-grid {
    grid-template-columns: 1fr;
  }
  .deliver-grid {
    grid-template-columns: 1fr;
  }
  .pay-steps {
    flex-direction: column;
  }
  .pay-sep {
    transform: rotate(90deg);
  }
}
</style>
