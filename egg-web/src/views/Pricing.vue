<template>
  <div class="pricing-page">
    <header class="pr-header">
      <div class="pr-head-main">
        <div class="u-eyebrow">COMMISSION // 定价</div>
        <h1 class="pr-title">定价 · 接单方案</h1>
        <p class="pr-sub">已交付 32 场瓦洛兰特演示赛 · 0%抽水纯彩池 · 3天可交付同款</p>
      </div>
      <div class="pr-head-facts">
        <div class="pr-fact">
          <span class="pr-fact-k">DELIVERY</span>
          <span class="pr-fact-v">3 DAYS</span>
        </div>
        <div class="pr-fact">
          <span class="pr-fact-k">DEPOSIT</span>
          <span class="pr-fact-v">4 : 5 : 1</span>
        </div>
      </div>
    </header>

    <!-- 三档方案 -->
    <div class="pricing-grid">
      <!-- 单场代办 -->
      <article class="pricing-card card-entrance">
        <div class="pr-card-top">
          <span class="pr-card-index">01</span>
          <span class="pr-card-type">PER MATCH</span>
        </div>
        <h2 class="pr-card-title">单场代办</h2>
        <div class="pr-price"><span class="pr-price-num">800–2000</span><span class="pr-price-unit">元 / 场</span></div>
        <p class="pr-card-note">按场计费，适合零散赛事/校内杯赛</p>
        <ul class="pr-list">
          <li><span class="pr-check" aria-hidden="true"></span><span>海报设计 1 张（适配朋友圈/群公告）</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>赛程录入 + 对阵配置 + 直播源绑定</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>榜单结算 + 积分彩池清分</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>战报图 9 张（赛后一键出图，含比分/高光）</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>可选：解说词条/弹幕预热（加量议价）</span></li>
        </ul>
        <div class="pr-actions">
          <el-button class="pr-cta" @click="$router.push('/login')">查看演示赛程</el-button>
          <a href="mailto:contact@example.com" class="pr-mail">单场咨询 → contact@example.com</a>
        </div>
      </article>

      <!-- 复刻演示站 · 推荐 -->
      <article class="pricing-card pricing-card--featured card-entrance">
        <span class="pr-flag">推荐</span>
        <div class="pr-card-top">
          <span class="pr-card-index">02</span>
          <span class="pr-card-type">FULL SITE</span>
        </div>
        <h2 class="pr-card-title">复刻演示站</h2>
        <div class="pr-price"><span class="pr-price-num">8000</span><span class="pr-price-unit">元起</span></div>
        <p class="pr-card-note">部署 + 域名 + 首季赛程导入 + 机器人托底，不含菠菜</p>
        <ul class="pr-list">
          <li><span class="pr-check" aria-hidden="true"></span><span>整站部署到你的域名/服务器</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>首季赛程一键导入（含瓦洛兰特演示数据）</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>机器人托底：无人下注时自动填充彩池与弹幕</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>含后台管理、排行榜、评论区、银行兜底全链路</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>3 天交付，交付后 7 天免费小修</span></li>
        </ul>
        <div class="pr-actions">
          <el-button type="primary" class="btn-hover-lift pr-cta" @click="$router.push('/login')">立即体验演示</el-button>
          <a href="mailto:contact@example.com" class="pr-mail">定制咨询 → contact@example.com</a>
        </div>
      </article>

      <!-- 按需定制 -->
      <article class="pricing-card card-entrance">
        <div class="pr-card-top">
          <span class="pr-card-index">03</span>
          <span class="pr-card-type">CUSTOM</span>
        </div>
        <h2 class="pr-card-title">按需定制</h2>
        <div class="pr-price"><span class="pr-price-num pr-price-num--text">另计</span><span class="pr-price-unit">超出部分</span></div>
        <p class="pr-card-note">首季赛程、开票与特殊需求另议</p>
        <ul class="pr-list">
          <li><span class="pr-check" aria-hidden="true"></span><span>首季赛程之外的赛程导入与配置</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>支持二次定制与功能扩展</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>开票与合同需求另议</span></li>
          <li><span class="pr-check" aria-hidden="true"></span><span>商务邮箱沟通报价与排期</span></li>
        </ul>
        <div class="pr-actions">
          <a href="mailto:contact@example.com" class="pr-ghost-btn">邮件沟通定制</a>
          <a href="mailto:contact@example.com" class="pr-mail">商务邮箱 → contact@example.com</a>
        </div>
      </article>
    </div>

    <!-- 付款方式 -->
    <section class="pr-info">
      <div class="pr-info-head">
        <span class="pr-info-index">A</span>
        <h2 class="pr-info-title">付款方式 · 定金 4:5:1</h2>
        <span class="pr-info-meta">PAYMENT</span>
      </div>
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
      <p class="pr-footnote">不含菠菜/现金博彩相关功能；单场代办可单场起订，复刻站含首季赛程，超出另计。开票需另议。</p>
      <div class="pr-info-actions">
        <el-button type="primary" class="btn-hover-lift" @click="$router.push('/login')">先用演示账号体验</el-button>
        <a href="mailto:contact@example.com" class="pr-mail">商务邮箱 → contact@example.com</a>
      </div>
    </section>

    <!-- 交付物清单 -->
    <section class="pr-info">
      <div class="pr-info-head">
        <span class="pr-info-index">B</span>
        <h2 class="pr-info-title">交付物清单</h2>
        <span class="pr-info-meta">INCLUDED</span>
      </div>
      <div class="deliver-grid">
        <div class="deliver-item">
          <span class="deliver-num">01</span>
          <div class="deliver-body"><strong>域名与部署</strong><span>含 Nginx/HTTPS 配置说明，源码交付可选</span></div>
        </div>
        <div class="deliver-item">
          <span class="deliver-num">02</span>
          <div class="deliver-body"><strong>赛程与榜单</strong><span>首季导入 + 榜单/结算演示数据</span></div>
        </div>
        <div class="deliver-item">
          <span class="deliver-num">03</span>
          <div class="deliver-body"><strong>机器人托底</strong><span>彩池与评论区无人时自动兜底</span></div>
        </div>
        <div class="deliver-item">
          <span class="deliver-num">04</span>
          <div class="deliver-body"><strong>海报与战报图</strong><span>单场代办含 1 张海报 + 9 张战报图</span></div>
        </div>
        <div class="deliver-item">
          <span class="deliver-num">05</span>
          <div class="deliver-body"><strong>3 天交付</strong><span>常规需求 3 个工作日内上线演示站</span></div>
        </div>
        <div class="deliver-item">
          <span class="deliver-num">06</span>
          <div class="deliver-body"><strong>不含菠菜</strong><span>积分不可变现，仅为校园赛预测玩法</span></div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
export default {
  name: 'PricingPage'
}
</script>

<style scoped>
.pricing-page {
  max-width: 1080px;
  margin: 0 auto;
}

/* ── Header ── */
.pr-header {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 28px;
  padding-bottom: 22px;
  border-bottom: 1px solid var(--border-subtle);
}
.pr-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 48px;
  height: 2px;
  background: var(--brand);
}
.pr-title {
  margin-top: 12px;
  font-family: var(--font-body);
  font-size: 36px;
  font-weight: 700;
  line-height: 1.12;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}
.pr-sub {
  margin-top: 8px;
  font-size: 13.5px;
  color: var(--text-secondary);
}
.pr-head-facts {
  display: flex;
  gap: 24px;
  padding-bottom: 4px;
  flex-shrink: 0;
}
.pr-fact {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.pr-fact-k {
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}
.pr-fact-v {
  font-family: var(--font-mono);
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
  color: var(--text-primary);
}

/* ── Tier cards ── */
.pricing-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: stretch;
  gap: 16px;
  margin-top: 26px;
}
.pricing-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.016), transparent 45%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  transition:
    transform var(--duration-normal) var(--ease-out),
    border-color var(--duration-fast) ease,
    box-shadow var(--duration-normal) ease;
}
.pricing-card:hover {
  transform: translateY(-3px);
  border-color: var(--border-strong);
  box-shadow: var(--shadow-lg);
}
.pricing-card--featured {
  border-color: var(--brand);
  background:
    linear-gradient(180deg, var(--brand-soft), transparent 42%),
    var(--bg-surface-1);
  box-shadow: var(--shadow-brand);
}
.pricing-card--featured:hover {
  border-color: var(--brand);
  box-shadow: var(--shadow-lg), var(--shadow-brand);
}
.pricing-card--featured::before {
  content: '';
  position: absolute;
  top: -1px;
  left: 20px;
  right: 20px;
  height: 2px;
  background: var(--brand);
}
.pr-flag {
  position: absolute;
  top: 0;
  right: 18px;
  padding: 4px 10px 5px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--cream);
  background: var(--brand);
  border-radius: 0 0 var(--radius-sm) var(--radius-sm);
}
.pr-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.pricing-card--featured .pr-card-top {
  padding-right: 46px;
}
.pr-card-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--brand);
}
.pr-card-type {
  font-family: var(--font-mono);
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
}
.pr-card-title {
  margin-top: 12px;
  font-family: var(--font-body);
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
}
.pr-price {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 10px;
  min-width: 0;
}
.pr-price-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--cream);
}
.pr-price-num--text {
  font-family: var(--font-body);
  font-size: 24px;
  letter-spacing: 0;
  color: var(--text-primary);
}
.pr-price-unit {
  font-size: 12px;
  color: var(--text-tertiary);
}
.pr-card-note {
  margin-top: 9px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-tertiary);
  min-height: 40px;
}
.pr-list {
  list-style: none;
  margin: 16px 0 0;
  padding: 14px 0 0;
  border-top: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  gap: 9px;
}
.pr-list li {
  display: flex;
  align-items: flex-start;
  gap: 9px;
  font-size: 12.5px;
  line-height: 1.6;
  color: var(--text-secondary);
}
.pr-list li > span:last-child {
  min-width: 0;
}
.pr-check {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  flex-shrink: 0;
  border-right: 1px solid var(--accent-primary);
  border-bottom: 1px solid var(--accent-primary);
  transform: rotate(45deg);
}
.pr-actions {
  display: flex;
  flex-direction: column;
  gap: 9px;
  margin-top: auto;
  padding-top: 18px;
}
.pr-cta {
  width: 100%;
}
.pr-ghost-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 38px;
  padding: 0 16px;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  color: var(--text-primary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) ease;
}
.pr-ghost-btn:hover {
  border-color: var(--brand);
  color: var(--cream);
  background: var(--brand-soft);
}
.pr-mail {
  font-family: var(--font-mono);
  font-size: 10.5px;
  color: var(--text-tertiary);
  text-align: center;
  word-break: break-all;
  transition: color var(--duration-fast) ease;
}
.pr-mail:hover {
  color: var(--brand);
}

/* ── Info panels ── */
.pr-info {
  margin-top: 20px;
  padding: 22px 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.016), transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}
.pr-info-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}
.pr-info-index {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
}
.pr-info-title {
  font-family: var(--font-body);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}
.pr-info-meta {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  white-space: nowrap;
}

/* ── Payment ── */
.pay-steps {
  display: flex;
  align-items: stretch;
  margin-top: 4px;
}
.pay-step {
  flex: 1;
  min-width: 0;
  padding: 16px 18px 6px;
  border-left: 1px solid var(--border-subtle);
}
.pay-step:first-child {
  border-left: 0;
  padding-left: 0;
}
.pay-step-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  color: var(--brand);
}
.pay-step-label {
  margin-top: 7px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.pay-step-desc {
  margin-top: 3px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-tertiary);
}
.pay-sep {
  display: none;
}
.pr-footnote {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
  font-size: 11.5px;
  line-height: 1.75;
  color: var(--text-tertiary);
}
.pr-info-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 16px;
}
.pr-info-actions .pr-mail {
  font-size: 11px;
  text-align: left;
}

/* ── Deliverables ── */
.deliver-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 28px;
  margin-top: 4px;
}
.deliver-item {
  display: flex;
  gap: 10px;
  min-width: 0;
  padding: 13px 0 12px;
  border-top: 1px solid var(--border-subtle);
}
.deliver-num {
  padding-top: 2px;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--text-faint);
  flex-shrink: 0;
}
.deliver-body {
  min-width: 0;
}
.deliver-body strong {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.deliver-body span {
  display: block;
  margin-top: 2px;
  font-size: 11.5px;
  line-height: 1.6;
  color: var(--text-tertiary);
}

@media (max-width: 920px) {
  .pr-head-facts {
    display: none;
  }
  .pricing-grid {
    grid-template-columns: 1fr;
  }
  .deliver-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .pr-header {
    padding-bottom: 18px;
  }
  .pr-title {
    font-size: 28px;
  }
  .pricing-grid {
    margin-top: 20px;
  }
  .pricing-card {
    padding: 20px;
  }
  .pr-price-num {
    font-size: 30px;
  }
  .pr-card-note {
    min-height: 0;
  }
  .pr-info {
    padding: 18px 16px;
  }
  .pay-steps {
    flex-direction: column;
  }
  .pay-step {
    border-left: 0;
    border-top: 1px solid var(--border-subtle);
    padding: 13px 0;
  }
  .pay-step:first-child {
    border-top: 0;
    padding-top: 10px;
  }
  .deliver-grid {
    grid-template-columns: 1fr;
  }
  .pr-info-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .pr-info-actions .el-button {
    width: 100%;
  }
  .pr-info-actions .pr-mail {
    text-align: center;
  }
}
</style>
