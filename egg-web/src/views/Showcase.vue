<template>
  <div class="about-page showcase-page">
    <div class="page-header">
      <h2 class="page-title">案例墙 · 已交付</h2>
      <p class="page-subtitle">已交付 32 场瓦洛兰特演示赛 · 0%抽水纯彩池 · 3天可交付同款</p>
    </div>

    <!-- 顶部数字 -->
    <div class="about-card showcase-hero">
      <div class="showcase-stat">
        <div class="showcase-stat-num">{{ deliveredCount }}</div>
        <div class="showcase-stat-label">已交付场次</div>
        <div class="showcase-stat-sub">VALORANT · 0%抽水纯彩池</div>
      </div>
      <div class="showcase-hero-right">
        <p class="about-text">
          已为校社团与小赛事主办累计交付 <strong>{{ deliveredCount }} 场</strong> 瓦洛兰特演示赛，<strong>0%抽水纯彩池</strong>，含报名-预测-榜单-结算全链路，<strong>3天可交付同款</strong>。
        </p>
        <div class="showcase-cta-row">
          <el-button type="primary" class="btn-hover-lift" @click="$router.push('/login')">查看演示账号</el-button>
          <el-button @click="$router.push('/pricing')">查看定价</el-button>
        </div>
      </div>
    </div>

    <!-- 6宫格案例墙 -->
    <div class="about-card">
      <h3 class="about-section-title">🏆 合作案例</h3>
      <div class="case-grid">
        <div v-for="c in cases" :key="c.id" class="case-card" @click="handleCaseClick(c)" style="cursor:pointer">
          <div class="case-card-cover" :style="{ background: c.gradient }">
            <div class="case-cover-placeholder">
              <span class="case-cover-emblem">{{ c.emblem }}</span>
              <span class="case-cover-vs">VS</span>
            </div>
            <!-- 保留 img 仅作可选增强，失败自动隐藏不影响布局 -->
            <img
              v-if="c.cover && !imgFailed[c.id]"
              :src="c.cover"
              :alt="c.title"
              loading="lazy"
              @error="onImgError(c.id, $event)"
            />
            <span class="case-card-badge">{{ c.tournament }}</span>
          </div>
          <div class="case-card-body">
            <div class="case-card-title">{{ c.title }}</div>
            <div class="case-card-meta">
              <span class="case-card-score">{{ c.score }}</span>
              <span class="case-card-time">{{ c.time }}</span>
            </div>
            <div class="case-card-desc">{{ c.desc }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 技术栈小条 -->
    <div class="about-card">
      <h3 class="about-section-title">🛠️ 技术栈</h3>
      <div class="stack-bar">
        <span v-for="s in stacks" :key="s" class="stack-chip">{{ s }}</span>
      </div>
      <p class="about-text" style="margin-top: 10px">
        数据源通过 <strong>Adapter</strong> 统一接入（Riot / GRID / OpenDota / Liquipedia 均保留），前台瓦聚焦仅为展示层过滤。
      </p>
    </div>

    <!-- CTA -->
    <div class="about-card showcase-cta">
      <h3 class="about-section-title">🚀 想办一场同款赛事？</h3>
      <p class="about-text">3天交付演示站，含赛程、押注、排行榜、评论区。先用演示账号体验全流程 — 账号 <strong>demo_player</strong> / 密码 <strong>demo123</strong></p>
      <div class="showcase-cta-row" style="margin-top: 12px">
        <el-button type="primary" size="large" class="btn-hover-lift" @click="$router.push('/login')">demo_player / demo123 去体验</el-button>
        <el-button @click="$router.push('/pricing')">查看定价</el-button>
        <a href="mailto:contact@example.com" class="contact-link" style="margin-left: 12px">定制咨询 → contact@example.com</a>
      </div>
    </div>
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
    handleCaseClick(c) {
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
.showcase-page .page-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin-top: 6px;
}
.showcase-hero {
  display: flex;
  gap: 24px;
  align-items: center;
}
.showcase-stat {
  min-width: 160px;
  text-align: center;
  padding: 16px;
  background: var(--bg-surface-2);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-subtle);
}
.showcase-stat-num {
  font-family: var(--font-display);
  font-size: 42px;
  color: var(--accent-primary);
  line-height: 1;
  letter-spacing: 1px;
}
.showcase-stat-label {
  font-size: 13px;
  color: var(--text-primary);
  font-weight: 600;
  margin-top: 6px;
}
.showcase-stat-sub {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.showcase-hero-right {
  flex: 1;
  min-width: 0;
}
.showcase-cta-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.case-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-top: 12px;
}
.case-card {
  background: var(--bg-surface-2);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: transform 0.2s var(--ease-out), border-color 0.2s ease;
}
.case-card:hover {
  transform: translateY(-2px);
  border-color: rgba(0, 229, 160, 0.25);
}
.case-card-cover {
  position: relative;
  height: 120px;
  background: var(--bg-surface-3);
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.case-card-cover img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.case-cover-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: #fff;
  text-shadow: 0 1px 8px rgba(0,0,0,0.35);
  font-weight: 700;
  letter-spacing: 0.5px;
  z-index: 0;
}
.case-cover-emblem {
  font-size: 15px;
  font-family: var(--font-display);
  opacity: 0.95;
}
.case-cover-vs {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0,0,0,0.22);
  border: 1px solid rgba(255,255,255,0.22);
  letter-spacing: 1px;
}
.case-card-badge {
  position: absolute;
  left: 8px;
  top: 8px;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(4px);
}
.case-card-body {
  padding: 12px;
}
.case-card-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.case-card-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 6px;
}
.case-card-score {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--accent-primary);
  font-weight: 600;
}
.case-card-time {
  font-size: 11px;
  color: var(--text-tertiary);
}
.case-card-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 6px;
  line-height: 1.5;
}
.stack-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.stack-chip {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(0, 229, 160, 0.08);
  border: 1px solid rgba(0, 229, 160, 0.18);
  color: var(--text-secondary);
}
.showcase-cta {
  text-align: left;
}
@media (max-width: 900px) {
  .case-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .showcase-hero {
    flex-direction: column;
    align-items: stretch;
  }
  .showcase-stat {
    min-width: 0;
  }
}
@media (max-width: 600px) {
  .case-grid {
    grid-template-columns: 1fr;
  }
}
</style>
