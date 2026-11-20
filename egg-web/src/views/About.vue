<template>
  <div class="about-page">
    <header class="ab-hero">
      <div class="u-eyebrow">DOSSIER // 关于</div>
      <h1 class="ab-title">关于我们</h1>
      <p class="ab-statement">纯彩池 · <span class="ab-em">0% 抽水</span><br />判断力的竞技场。</p>
      <p class="ab-hero-sub">Egg Esports 是一个独立的电竞赛事预测平台，专注瓦洛兰特。</p>
      <div class="ab-strip">
        <div class="ab-cell">
          <div class="ab-cell-num">0%</div>
          <div class="ab-cell-label">抽水</div>
        </div>
        <div class="ab-cell">
          <div class="ab-cell-num">4</div>
          <div class="ab-cell-label">游戏适配</div>
        </div>
        <div class="ab-cell">
          <div class="ab-cell-num">12</div>
          <div class="ab-cell-label">业务模块</div>
        </div>
        <div class="ab-cell">
          <div class="ab-cell-num">7<span class="ab-unit">天</span></div>
          <div class="ab-cell-label">Token 有效期</div>
        </div>
      </div>
    </header>

    <!-- 01 项目定位 -->
    <section class="ab-section">
      <div class="ab-sec-head">
        <span class="ab-sec-num">01</span>
        <h2 class="ab-sec-title">项目定位</h2>
        <span class="ab-sec-label">POSITION</span>
      </div>
      <div class="ab-prose">
        <p>
          Egg Esports 是一个独立的电竞赛事预测平台，专注瓦洛兰特。平台采用纯彩池制（Parimutuel），<strong>0% 抽水</strong>——不设庄家、不抽一分钱，赔率完全由玩家投注决定。你押的每一分都在奖池里，猜中按比例分账。积分永远是积分，不可变现——这不是博彩，是判断力的竞技场。
        </p>
      </div>
    </section>

    <!-- 02 玩法 -->
    <section class="ab-section">
      <div class="ab-sec-head">
        <span class="ab-sec-num">02</span>
        <h2 class="ab-sec-title">玩法</h2>
        <span class="ab-sec-label">MECHANICS</span>
      </div>
      <div class="ab-features">
        <div class="ab-feature">
          <span class="ab-feature-num">01</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">积分纯靠竞猜</h3>
            <p class="ab-feature-desc">无签到、无任务、无额外获取方式，积分只来自你的预测眼光</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">02</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">兜底系统</h3>
            <p class="ab-feature-desc">0 积分可领取贷款和低保，让你东山再起的机会</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">03</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">0% 抽水 · 纯彩池</h3>
            <p class="ab-feature-desc">不抽一分钱，纯粹的对赌，猜中按比例分账</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">04</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">净收益榜</h3>
            <p class="ab-feature-desc">只计算押注输赢，低保和贷款不计入排名。能上榜，全凭眼光</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">05</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">单次押注不设限</h3>
            <p class="ab-feature-desc">没有上限，想押多少押多少</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">06</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">押注截止前为准</h3>
            <p class="ab-feature-desc">结算以最后截止前为准，绝不提前锁定</p>
          </div>
        </div>
        <div class="ab-feature">
          <span class="ab-feature-num">07</span>
          <div class="ab-feature-body">
            <h3 class="ab-feature-title">非博彩 · 纯竞技</h3>
            <p class="ab-feature-desc">积分不可充值、不可提现，竞赛纯粹靠判断力</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 03 技术栈 -->
    <section class="ab-section">
      <div class="ab-sec-head">
        <span class="ab-sec-num">03</span>
        <h2 class="ab-sec-title">技术栈</h2>
        <span class="ab-sec-label">STACK</span>
      </div>
      <div class="ab-chips">
        <span class="ab-chip">Spring Boot 3</span>
        <span class="ab-chip">Vue 3 + Element Plus</span>
        <span class="ab-chip">MySQL + MyBatis-Plus</span>
        <span class="ab-chip">Redis + Lua 限流</span>
        <span class="ab-chip">Adapter 多数据源</span>
        <span class="ab-chip">WebSocket 实时评论</span>
      </div>
    </section>

    <!-- 04 数据源 -->
    <section class="ab-section">
      <div class="ab-sec-head">
        <span class="ab-sec-num">04</span>
        <h2 class="ab-sec-title">数据源</h2>
        <span class="ab-sec-label">DATA</span>
      </div>
      <div class="ab-prose">
        <p>数据源通过 <strong>Adapter</strong> 统一接入（Riot / GRID / OpenDota / Liquipedia 均保留），前台瓦聚焦仅为展示层过滤。</p>
      </div>
    </section>

    <!-- 05 免责声明 -->
    <section class="ab-section">
      <div class="ab-sec-head">
        <span class="ab-sec-num">05</span>
        <h2 class="ab-sec-title">免责声明</h2>
        <span class="ab-sec-label">NOTICE</span>
      </div>
      <div class="ab-prose">
        <p>本站为校园赛预测演示产品：积分不可充值、不可提现、不可变现；不提供菠菜 / 现金博彩相关功能，所有玩法仅用于判断力竞技与赛事演示。</p>
      </div>
    </section>

    <!-- 联系 -->
    <section class="ab-contact">
      <div class="ab-contact-col">
        <div class="ab-contact-kicker">CONTACT // 联系</div>
        <h2 class="ab-contact-title">联系我们</h2>
        <p>Egg Esports 是一个开源的电竞赛事预测演示项目。如果你有任何建议或发现问题，欢迎通过 GitHub Issues 反馈。</p>
        <div class="ab-contact-row">
          <el-icon><Message /></el-icon>
          <a href="https://github.com/denglizibinghua/egg-esports-open/issues" target="_blank" rel="noopener noreferrer">GitHub Issues</a>
        </div>
      </div>
    </section>
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
  max-width: 880px;
  margin: 0 auto;
}

/* ── Hero ── */
.ab-hero {
  position: relative;
  padding: 6px 0 34px;
  border-bottom: 1px solid var(--border-subtle);
}
.ab-hero::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 48px;
  height: 2px;
  background: var(--brand);
}
.ab-title {
  margin-top: 12px;
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}
.ab-statement {
  margin-top: 18px;
  max-width: 760px;
  font-family: var(--font-body);
  font-size: clamp(28px, 4.4vw, 40px);
  font-weight: 700;
  line-height: 1.24;
  letter-spacing: -0.025em;
  color: var(--cream);
}
.ab-em {
  color: var(--brand);
}
.ab-hero-sub {
  margin-top: 16px;
  max-width: 640px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.ab-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-top: 28px;
  border-top: 1px solid var(--border-subtle);
  border-bottom: 1px solid var(--border-subtle);
}
.ab-cell {
  min-width: 0;
  padding: 16px 18px;
  border-left: 1px solid var(--border-subtle);
}
.ab-cell:first-child {
  border-left: 0;
  padding-left: 0;
}
.ab-cell-num {
  font-family: var(--font-mono);
  font-variant-numeric: tabular-nums;
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}
.ab-unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
}
.ab-cell-label {
  margin-top: 7px;
  font-size: 11px;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
}

/* ── Sections ── */
.ab-section {
  margin-top: 44px;
}
.ab-sec-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-subtle);
}
.ab-sec-num {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
}
.ab-sec-title {
  font-family: var(--font-body);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}
.ab-sec-label {
  margin-left: auto;
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  white-space: nowrap;
}
.ab-prose {
  max-width: 760px;
  margin-top: 16px;
  font-size: 15px;
  line-height: 1.85;
  color: var(--text-secondary);
}
.ab-prose p + p {
  margin-top: 12px;
}
.ab-prose strong {
  color: var(--accent-primary);
  font-weight: 600;
}

/* ── Features ── */
.ab-features {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 32px;
  max-width: 760px;
  margin-top: -1px;
}
.ab-feature {
  display: flex;
  gap: 12px;
  min-width: 0;
  padding: 14px 0;
  border-top: 1px solid var(--border-subtle);
}
.ab-feature:last-child {
  grid-column: 1 / -1;
}
.ab-feature-num {
  padding-top: 3px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--brand);
  flex-shrink: 0;
}
.ab-feature-body {
  min-width: 0;
}
.ab-feature-title {
  font-family: var(--font-body);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}
.ab-feature-desc {
  margin-top: 3px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--text-tertiary);
}

/* ── Chips ── */
.ab-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.ab-chip {
  display: inline-flex;
  align-items: center;
  padding: 5px 10px;
  font-family: var(--font-mono);
  font-size: 10.5px;
  letter-spacing: 0.03em;
  color: var(--text-secondary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}

/* ── Commission ── */
.ab-commission {
  position: relative;
  margin-top: 44px;
  padding: 26px 28px;
  background:
    linear-gradient(120deg, var(--brand-soft), transparent 55%),
    var(--bg-surface-1);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
}
.ab-commission-title {
  margin-top: 10px;
  font-family: var(--font-body);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}
.ab-commission-text {
  margin-top: 12px;
  max-width: 760px;
  font-size: 14px;
  line-height: 1.85;
  color: var(--text-secondary);
}
.ab-commission-text strong {
  color: var(--accent-primary);
  font-weight: 600;
}
.ab-inline-link {
  color: var(--accent-primary);
  font-weight: 600;
}
.ab-inline-link:hover {
  text-decoration: underline;
}
.ab-order-list {
  margin-top: 16px;
}
.ab-order-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--border-subtle);
  min-width: 0;
}
.ab-order-item:last-child {
  border-bottom: 1px solid var(--border-subtle);
}
.ab-order-num {
  padding-top: 2px;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.06em;
  color: var(--brand);
  flex-shrink: 0;
}
.ab-order-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.ab-order-body strong {
  font-size: 13.5px;
  font-weight: 600;
  color: var(--text-primary);
}
.ab-order-body span {
  font-size: 12px;
  line-height: 1.65;
  color: var(--text-tertiary);
}
.ab-demo {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.8;
  color: var(--text-tertiary);
  word-break: break-word;
}
.ab-demo code {
  padding: 1px 6px;
  font-family: var(--font-mono);
  font-size: 11.5px;
  color: var(--accent-primary);
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
}
.ab-demo a {
  color: var(--accent-primary);
  word-break: break-all;
}
.ab-demo a:hover {
  text-decoration: underline;
}
.ab-commission-actions {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-top: 18px;
}
.ab-mail {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-tertiary);
  word-break: break-all;
  transition: color var(--duration-fast) ease;
}
.ab-mail:hover {
  color: var(--brand);
}

/* ── Contact / donate ── */
.ab-contact {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 36px;
  margin-top: 40px;
  padding-top: 30px;
  border-top: 1px solid var(--border-subtle);
}
.ab-contact-col {
  min-width: 0;
}
.ab-contact-kicker {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--text-faint);
}
.ab-contact-title {
  margin-top: 7px;
  font-family: var(--font-body);
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
}
.ab-contact-col p {
  margin-top: 9px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.ab-contact-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  font-size: 13px;
  color: var(--text-secondary);
  word-break: break-word;
}
.ab-contact-row .el-icon {
  color: var(--brand);
  flex-shrink: 0;
}
.ab-donate {
  border-left: 1px solid var(--border-subtle);
  padding-left: 36px;
}
.donate-qr {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-top: 14px;
}
.donate-qr-img {
  width: 168px;
  height: 168px;
  padding: 8px;
  background: var(--cream);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  overflow: hidden;
}
.donate-qr-label {
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--text-tertiary);
}

@media (max-width: 760px) {
  .ab-title {
    font-size: 26px;
  }
  .ab-statement {
    font-size: 26px;
  }
  .ab-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .ab-cell {
    padding: 14px 12px;
  }
  .ab-cell:nth-child(odd) {
    border-left: 0;
    padding-left: 0;
  }
  .ab-cell:nth-child(n + 3) {
    border-top: 1px solid var(--border-subtle);
  }
  .ab-features {
    grid-template-columns: 1fr;
  }
  .ab-commission {
    padding: 20px;
    margin-top: 34px;
  }
  .ab-commission-title {
    font-size: 19px;
  }
  .ab-contact {
    grid-template-columns: 1fr;
    gap: 26px;
  }
  .ab-donate {
    border-left: 0;
    padding-left: 0;
    padding-top: 24px;
    border-top: 1px solid var(--border-subtle);
  }
  .ab-commission-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .ab-commission-actions .el-button {
    width: 100%;
  }
  .ab-mail {
    text-align: center;
  }
}
</style>
