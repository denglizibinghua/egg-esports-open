<template>
  <div class="showcase-page">
    <!-- HERO -->
    <header class="sc-hero">
      <div class="sc-hero-main">
        <div class="u-eyebrow">DELIVERED WORK // 案例</div>
        <h1 class="sc-title">案例墙 · 已交付</h1>
        <p class="sc-sub">已交付 32 场瓦洛兰特演示赛 · 0%抽水纯彩池 · 3天可交付同款</p>
        <p class="sc-lede">
          已为校社团与小赛事主办累计交付 <strong>{{ deliveredCount }} 场</strong> 瓦洛兰特演示赛，
          <strong>0%抽水纯彩池</strong>，含报名-预测-榜单-结算全链路，<strong>3天可交付同款</strong>。
        </p>
        <div class="sc-actions">
          <el-button type="primary" class="btn-hover-lift" @click="$router.push('/login')">查看演示账号</el-button>
          <el-button @click="$router.push('/pricing')">查看定价</el-button>
        </div>
      </div>

      <aside class="sc-stat u-panel">
        <span class="sc-stat-glow" aria-hidden="true"></span>
        <div class="sc-stat-kicker">TOTAL DELIVERED</div>
        <div class="sc-stat-num">{{ deliveredCount }}</div>
        <div class="sc-stat-label">已交付场次</div>
        <div class="sc-stat-sub">VALORANT · 0%抽水纯彩池</div>
        <div class="sc-stat-foot">
          <span class="sc-stat-rule" aria-hidden="true"></span>
          CAMPUS &amp; COMMUNITY EVENTS
        </div>
      </aside>
    </header>

    <!-- 案例墙 -->
    <section class="sc-section">
      <div class="sc-section-head">
        <div class="sc-section-main">
          <span class="sc-section-index">01</span>
          <h2 class="sc-section-title">合作案例</h2>
          <span class="sc-section-meta">{{ cases.length }} 场 · 全链路交付</span>
        </div>
        <span class="sc-section-count">SELECTED WORK</span>
      </div>

      <div class="case-grid">
        <article v-for="c in cases" :key="c.id" class="case-card card-entrance" @click="handleCaseClick(c)">
          <div class="case-cover" :style="{ background: c.gradient }">
            <span class="case-cover-shade" aria-hidden="true"></span>
            <span class="case-cover-hatch u-hatch" aria-hidden="true"></span>
            <span class="case-cover-index">{{ String(c.id).padStart(2, '0') }}</span>
            <span class="case-badge">{{ c.tournament }}</span>
            <div class="case-cover-core">
              <span class="case-emblem">{{ c.emblem }}</span>
              <span class="case-vs">VS</span>
            </div>
            <!-- 保留 img 仅作可选增强，失败自动隐藏不影响布局 -->
            <img
              v-if="c.cover && !imgFailed[c.id]"
              :src="c.cover"
              :alt="c.title"
              loading="lazy"
              @error="onImgError(c.id, $event)"
            />
          </div>
          <div class="case-body">
            <h3 class="case-title">{{ c.title }}</h3>
            <div class="case-meta">
              <span class="case-score">{{ c.score }}</span>
              <span class="case-time">{{ c.time }}</span>
            </div>
            <p class="case-desc">{{ c.desc }}</p>
          </div>
        </article>
      </div>
    </section>

    <!-- 技术栈 -->
    <section class="sc-section">
      <div class="sc-section-head">
        <div class="sc-section-main">
          <span class="sc-section-index">02</span>
          <h2 class="sc-section-title">技术栈</h2>
          <span class="sc-section-meta">全链路 · 可复刻</span>
        </div>
        <span class="sc-section-count">STACK</span>
      </div>
      <div class="stack-bar">
        <span v-for="s in stacks" :key="s" class="stack-chip">{{ s }}</span>
      </div>
      <p class="sc-stack-note">
        数据源通过 <strong>Adapter</strong> 统一接入（Riot / GRID / OpenDota / Liquipedia 均保留），前台瓦聚焦仅为展示层过滤。
      </p>
    </section>

    <!-- CTA -->
    <section class="sc-cta">
      <span class="sc-cta-hatch u-hatch" aria-hidden="true"></span>
      <div class="sc-cta-inner">
        <div class="sc-cta-copy">
          <div class="u-eyebrow">COMMISSION // 接单</div>
          <h2 class="sc-cta-title">想办一场同款赛事？</h2>
          <p class="sc-cta-text">
            3天交付演示站，含赛程、押注、排行榜、评论区。先用演示账号体验全流程 — 账号
            <strong>demo_player</strong> / 密码 <strong>demo123</strong>
          </p>
        </div>
        <div class="sc-cta-actions">
          <el-button type="primary" size="large" class="btn-hover-lift" @click="$router.push('/login')">demo_player / demo123 去体验</el-button>
          <el-button @click="$router.push('/pricing')">查看定价</el-button>
          <a href="mailto:contact@example.com" class="sc-mail">定制咨询 → contact@example.com</a>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
import request from '@/utils/request'
import { extractList } from '@/utils/pagination'

export default {
  name: 'ShowcasePage',
  data() {
    return {
      deliveredCount: 32,
      imgFailed: {},
      // 占位渐变：不依赖 /uploads/case-*.png，首屏零404；cover 置空，img 通过 v-if 容错
      cases: [
        { id: 1, title: 'EDG vs FPX · 决赛', tournament: 'VCT CN 演示赛', score: '2-1 · Haven 13-4', time: '2026-08-20', desc: '校内总决赛，120人押注，彩池破万｜交付物：海报1张+战报图9张+榜单结算', cover: '', gradient: 'linear-gradient(135deg, #FF4655 0%, #7A1F2E 100%)', emblem: 'EDG · FPX' },
        { id: 2, title: 'TE vs JDG · 半决赛', tournament: 'EGG杯', score: '2-0 · Bind 13-7', time: '2026-08-18', desc: '战术拉满，烟位封点教科书｜交付物：赛程录入+比分直播+榜单', cover: '', gradient: 'linear-gradient(135deg, #00E5A0 0%, #0A4A3A 100%)', emblem: 'TE · JDG' },
        { id: 3, title: 'BLG vs WOL · 小组赛', tournament: 'EGG杯·校园冠军赛', score: '2-1 · Ascent 13-10', time: '2026-08-15', desc: '让一追二，残局 1v2 翻盘｜交付物：海报+榜单结算+战报图9张', cover: '', gradient: 'linear-gradient(135deg, #8B5CF6 0%, #1E1B4B 100%)', emblem: 'BLG · WOL' },
        { id: 4, title: 'DRG vs NOVA · 揭幕战', tournament: 'VCT CN 演示赛·季后赛', score: '2-0 · Lotus 13-6', time: '2026-08-12', desc: '新人枪猛，敢打敢拼｜交付物：赛程录入+榜单+战报图', cover: '', gradient: 'linear-gradient(135deg, #F59E0B 0%, #7C2D12 100%)', emblem: 'DRG · NOVA' },
        { id: 5, title: 'TYLOO vs AG · 季军赛', tournament: 'EGG杯', score: '2-1 · Split 13-9', time: '2026-08-10', desc: '老牌劲旅经验制胜｜交付物：海报+赛程+榜单结算', cover: '', gradient: 'linear-gradient(135deg, #06B6D4 0%, #0C4A6E 100%)', emblem: 'TYLOO · AG' },
        { id: 6, title: 'EDG vs TE · 表演赛', tournament: 'EGG杯·瓦洛兰特邀请赛', score: '2-0 · Sunset 13-8', time: '2026-08-08', desc: '全场高能，弹幕刷屏｜交付物：战报图9张+弹幕托底演示', cover: '', gradient: 'linear-gradient(135deg, #EC4899 0%, #4A044E 100%)', emblem: 'EDG · TE' }
      ],
      stacks: ['Spring Boot 3', 'Vue 3 + Element Plus', 'MySQL + MyBatis-Plus', 'Redis + Lua 限流', 'Adapter 多数据源', 'WebSocket 实时评论']
    }
  },
  created() {
    this.fetchCount()
  },
  methods: {
    handleCaseClick() {
      // 案例为硬编码演示数据，不对应真实 matchId，避免 /match/:id 404/500
      this.$message ? this.$message.info('案例为演示数据，前往赛程查看真实赛事') : null
      // 可选：跳到赛程页
      this.$router.push('/matches').catch(() => {})
    },
    fetchCount() {
      // 仅用 /match/list 计数，/match/count 接口不存在，避免 404/500 弹错
      request.get('/match/list', { params: { status: 2, gameId: 1, page: 1, size: 1 } }).then((res) => {
        const data = res.data ?? res
        // 兼容 IPage { total, records } 与 { list } 两种返回
        const total = data.total ?? data?.data?.total ?? null
        const list = extractList(data)
        if (typeof total === 'number' && total > 0) this.deliveredCount = total
        else if (Array.isArray(list) && list.length > 0 && typeof data.total === 'number') this.deliveredCount = data.total
        else if (Array.isArray(list) && list.length > 0) this.deliveredCount = list.length
      }).catch(() => {
        // 静默失败，保持默认 32，不弹错
      })
    },
    onImgError(id, e) {
      this.$set ? this.$set(this.imgFailed, id, true) : (this.imgFailed[id] = true)
      // 兼容 Vue3 无 $set 时触发响应式
      this.imgFailed = { ...this.imgFailed, [id]: true }
      if (e && e.target) e.target.style.display = 'none'
    }
  }
}
</script>

<style scoped>
.showcase-page {
  max-width: 1080px;
  margin: 0 auto;
}

/* ── Hero ── */
.sc-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(280px, 0.7fr);
  gap: 40px;
  align-items: center;
  padding: 4px 0 40px;
  border-bottom: 1px solid var(--border-subtle);
}
.sc-hero::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 48px;
  height: 2px;
  background: var(--brand);
}
.sc-title {
  margin-top: 14px;
  color: var(--text-primary);
  font-family: var(--font-body);
  font-size: 38px;
  font-weight: 700;
  line-height: 1.14;
  letter-spacing: -0.02em;
}
.sc-sub {
  margin-top: 10px;
  color: var(--text-secondary);
  font-size: 13.5px;
}
.sc-lede {
  margin-top: 18px;
  max-width: 580px;
  color: var(--text-secondary);
  font-size: 14.5px;
  line-height: 1.85;
}
.sc-lede strong {
  color: var(--text-primary);
  font-weight: 600;
}
.sc-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 24px;
}
.sc-actions .el-button + .el-button {
  margin-left: 0;
}

/* Stat block */
.sc-stat {
  padding: 24px 26px 20px;
}
.sc-stat-glow {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  pointer-events: none;
  background: radial-gradient(120% 100% at 100% 0%, var(--brand-soft), transparent 62%);
}
.sc-stat-kicker {
  position: relative;
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}
.sc-stat-num {
  position: relative;
  margin-top: 10px;
  font-family: var(--font-display);
  font-size: clamp(48px, 5.4vw, 64px);
  font-weight: 400;
  line-height: 0.86;
  letter-spacing: 0.02em;
  color: var(--cream);
}
.sc-stat-label {
  position: relative;
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}
.sc-stat-sub {
  position: relative;
  margin-top: 3px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}
.sc-stat-foot {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 18px;
  padding-top: 12px;
  border-top: 1px solid var(--border-subtle);
  font-family: var(--font-mono);
  font-size: 9px;
  letter-spacing: 0.12em;
  color: var(--text-faint);
}
.sc-stat-rule {
  width: 18px;
  height: 2px;
  flex-shrink: 0;
  background: var(--brand);
}

/* ── Section head ── */
.sc-section {
  margin-top: 44px;
}
.sc-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-subtle);
}
.sc-section-main {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}
.sc-section-index {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  color: var(--brand);
}
.sc-section-title {
  font-family: var(--font-body);
  font-size: 19px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}
.sc-section-meta {
  font-size: 11.5px;
  color: var(--text-tertiary);
}
.sc-section-count {
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.16em;
  color: var(--text-faint);
  white-space: nowrap;
}

/* ── Case grid ── */
.case-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}
.case-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.016), transparent 46%),
    var(--bg-surface-1);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition:
    transform var(--duration-normal) var(--ease-out),
    border-color var(--duration-normal) ease,
    box-shadow var(--duration-normal) ease;
}
.case-card:hover {
  transform: translateY(-3px);
  border-color: var(--brand-line);
  box-shadow: var(--shadow-lg);
}
.case-cover {
  position: relative;
  height: 156px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  isolation: isolate;
}
.case-cover-shade {
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    linear-gradient(180deg, rgba(4, 6, 9, 0.26) 0%, rgba(4, 6, 9, 0.52) 58%, rgba(4, 6, 9, 0.78) 100%),
    radial-gradient(130% 110% at 50% 42%, transparent 28%, rgba(4, 6, 9, 0.22) 100%);
}
.case-cover-hatch {
  position: absolute;
  inset: 0;
  z-index: 1;
  opacity: 0.55;
}
.case-cover-index {
  position: absolute;
  top: 10px;
  right: 12px;
  z-index: 3;
  font-family: var(--font-mono);
  font-size: 10px;
  letter-spacing: 0.1em;
  color: var(--cream);
  opacity: 0.55;
}
.case-badge {
  position: absolute;
  top: 10px;
  left: 10px;
  z-index: 3;
  max-width: calc(100% - 56px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 3px 8px;
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--cream);
  background: rgba(4, 6, 9, 0.5);
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-sm);
  backdrop-filter: blur(4px);
}
.case-cover-core {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  padding: 0 16px;
  text-align: center;
}
.case-emblem {
  font-family: var(--font-mono);
  font-size: 10.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--cream);
  opacity: 0.85;
}
.case-vs {
  padding: 2px 14px;
  border-top: 1px solid var(--border-strong);
  border-bottom: 1px solid var(--border-strong);
  font-family: var(--font-display);
  font-size: 40px;
  line-height: 0.9;
  letter-spacing: 0.06em;
  color: var(--cream);
  text-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
}
.case-cover img {
  position: absolute;
  inset: 0;
  z-index: 2;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.case-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  padding: 15px 16px 16px;
}
.case-title {
  font-family: var(--font-body);
  font-size: 14.5px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.case-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 7px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--border-subtle);
}
.case-score {
  font-family: var(--font-mono);
  font-size: 11px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
  color: var(--accent-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.case-time {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--text-tertiary);
  white-space: nowrap;
}
.case-desc {
  margin-top: 9px;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--text-tertiary);
}

/* ── Stack ── */
.stack-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
}
.stack-chip {
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
  transition: border-color var(--duration-fast) ease, color var(--duration-fast) ease;
}
.stack-chip:hover {
  border-color: var(--brand-line);
  color: var(--text-primary);
}
.sc-stack-note {
  margin-top: 14px;
  max-width: 760px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.sc-stack-note strong {
  color: var(--accent-primary);
  font-weight: 600;
}

/* ── CTA band ── */
.sc-cta {
  position: relative;
  margin-top: 52px;
  overflow: hidden;
  background:
    linear-gradient(120deg, var(--brand-soft), transparent 58%),
    var(--bg-surface-1);
  border: 1px solid var(--brand-line);
  border-radius: var(--radius-lg);
}
.sc-cta-hatch {
  position: absolute;
  inset: 0;
  opacity: 0.5;
  pointer-events: none;
}
.sc-cta-inner {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 32px;
  padding: 30px 32px;
}
.sc-cta-copy {
  min-width: 0;
  max-width: 560px;
}
.sc-cta-title {
  margin-top: 10px;
  font-family: var(--font-body);
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--text-primary);
}
.sc-cta-text {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.8;
  color: var(--text-secondary);
}
.sc-cta-text strong {
  font-family: var(--font-mono);
  font-size: 12px;
  font-weight: 600;
  color: var(--accent-primary);
}
.sc-cta-actions {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 9px;
  flex-shrink: 0;
}
.sc-cta-actions .el-button + .el-button {
  margin-left: 0;
}
.sc-mail {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--text-tertiary);
  word-break: break-all;
  transition: color var(--duration-fast) ease;
}
.sc-mail:hover {
  color: var(--brand);
}

@media (max-width: 960px) {
  .sc-hero {
    grid-template-columns: 1fr;
    gap: 28px;
  }
  .case-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .sc-cta-inner {
    flex-direction: column;
    align-items: stretch;
    gap: 22px;
  }
  .sc-cta-actions {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
  }
}

@media (max-width: 640px) {
  .sc-title {
    font-size: 30px;
  }
  .sc-hero {
    padding-bottom: 30px;
  }
  .sc-section {
    margin-top: 34px;
  }
  .sc-section-count {
    display: none;
  }
  .case-grid {
    grid-template-columns: 1fr;
  }
  .sc-cta {
    margin-top: 40px;
  }
  .sc-cta-inner {
    padding: 22px 20px;
  }
  .sc-cta-actions {
    flex-direction: column;
    align-items: stretch;
  }
  .sc-cta-actions .el-button {
    width: 100%;
  }
  .sc-stat-foot {
    white-space: normal;
  }
  .sc-mail {
    text-align: center;
  }
}
</style>
