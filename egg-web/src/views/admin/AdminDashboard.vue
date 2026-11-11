<template>
  <div class="admin-dashboard">
    <header class="page-header">
      <div class="header-main">
        <div class="page-eyebrow">// CONTROL ROOM</div>
        <h1 class="page-title">管理后台</h1>
        <p class="page-sub">赛事编排 · 结算调度 · 公告与数据同步</p>
      </div>
      <div class="header-hud" aria-hidden="true">
        <span class="hud-item hud-item--live"><span class="hud-dot"></span>ACCESS / ADMIN</span>
        <span class="hud-sep"></span>
        <span class="hud-item">MODE / CRUD</span>
      </div>
    </header>

    <!-- 管理后台主体：5 个 Tab，分别由 4 个子组件承载 -->
    <!-- 路由权限校验（requiresAdmin）在 router/index.js 全局守卫中完成，组件内不重复 -->
    <el-tabs v-model="activeTab" class="admin-tabs">
      <!-- 赛事管理：赛事列表 + 状态筛选 + 结算 -->
      <el-tab-pane name="match">
        <template #label>
          <span class="tab-label"><span class="tab-idx">01</span>赛事管理</span>
        </template>
        <MatchManageTab ref="matchListRef" mode="list" />
      </el-tab-pane>

      <!-- 批量导入：JSON 解析 + 预览 + 提交。lazy 保证首次切到才挂载，与原行为一致 -->
      <el-tab-pane name="import" lazy>
        <template #label>
          <span class="tab-label"><span class="tab-idx">02</span>批量导入</span>
        </template>
        <MatchManageTab mode="import" @import-success="onImportSuccess" />
      </el-tab-pane>

      <!-- 数据同步：触发同步 + 结果统计 -->
      <el-tab-pane name="sync" lazy>
        <template #label>
          <span class="tab-label"><span class="tab-idx">03</span>数据同步</span>
        </template>
        <SyncTab />
      </el-tab-pane>

      <!-- 公告管理：列表 + 增删改。gamesVersion 用于跨 Tab 刷新关联游戏下拉 -->
      <el-tab-pane name="announcement" lazy>
        <template #label>
          <span class="tab-label"><span class="tab-idx">04</span>公告管理</span>
        </template>
        <AnnouncementTab :games-version="gamesVersion" />
      </el-tab-pane>

      <!-- 游戏管理：列表 + 增删改。增删改后 emit games-changed 驱动公告下拉刷新 -->
      <el-tab-pane name="game" lazy>
        <template #label>
          <span class="tab-label"><span class="tab-idx">05</span>游戏管理</span>
        </template>
        <GameTab @games-changed="onGamesChanged" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import MatchManageTab from '@/components/admin/MatchManageTab.vue'
import SyncTab from '@/components/admin/SyncTab.vue'
import AnnouncementTab from '@/components/admin/AnnouncementTab.vue'
import GameTab from '@/components/admin/GameTab.vue'

export default {
  name: 'AdminDashboard',
  components: { MatchManageTab, SyncTab, AnnouncementTab, GameTab },
  data() {
    return {
      // 当前激活的 Tab
      activeTab: 'match',
      // 游戏列表版本号：GameTab 增删改游戏后递增，驱动 AnnouncementTab 重新拉取关联游戏下拉
      gamesVersion: 0
    }
  },
  methods: {
    // 批量导入成功后，刷新赛事列表（match 模式的 MatchManageTab 实例）
    onImportSuccess() {
      if (this.$refs.matchListRef) {
        this.$refs.matchListRef.refresh()
      }
    },
    // 游戏列表变更后，递增版本号，驱动 AnnouncementTab 刷新关联游戏下拉
    onGamesChanged() {
      this.gamesVersion++
    }
  }
}
</script>

<style scoped>
.admin-dashboard {
  position: relative;
}

/* ---------- Page header ---------- */
.page-header {
  position: relative;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 22px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-subtle);
}

.page-header::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -1px;
  width: 44px;
  height: 2px;
  background: var(--brand);
  box-shadow: 0 0 12px var(--brand-line);
}

.page-eyebrow {
  font-family: var(--font-mono);
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: var(--brand);
  margin-bottom: 8px;
}

.page-title {
  font-family: var(--font-body);
  font-size: 32px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--text-primary);
}

.page-sub {
  margin-top: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
  letter-spacing: 0.02em;
}

/* ---------- Header HUD ---------- */
.header-hud {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-sm);
  background: var(--bg-surface-1);
  font-family: var(--font-mono);
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.14em;
  white-space: nowrap;
}

.hud-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--text-tertiary);
}

.hud-item--live {
  color: var(--accent-primary);
}

.hud-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--accent-primary);
  box-shadow: 0 0 8px var(--accent-primary);
}

.hud-sep {
  width: 1px;
  height: 12px;
  background: var(--border-default);
}

/* ---------- Tabs ---------- */
.admin-tabs {
  margin-top: 0;
}

.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: 20px;
}

.admin-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px !important;
  background: var(--border-subtle) !important;
}

.admin-tabs :deep(.el-tabs__item) {
  height: 46px !important;
  padding: 0 18px !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  letter-spacing: 0.04em;
  color: var(--text-tertiary) !important;
  transition: color var(--duration-fast) ease;
}

.admin-tabs :deep(.el-tabs__item:hover) {
  color: var(--text-primary) !important;
}

.admin-tabs :deep(.el-tabs__item.is-active) {
  color: var(--text-primary) !important;
}

.admin-tabs :deep(.el-tabs__active-bar) {
  height: 2px !important;
  background: linear-gradient(90deg, var(--brand), var(--brand-deep)) !important;
  box-shadow: 0 0 12px var(--brand-line);
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tab-idx {
  font-family: var(--font-mono);
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.1em;
  color: var(--text-faint);
  transition: color var(--duration-fast) ease;
}

.admin-tabs :deep(.el-tabs__item.is-active) .tab-idx {
  color: var(--brand);
}

/* ---------- Responsive ---------- */
@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }

  .page-title {
    font-size: 26px;
  }

  .header-hud {
    align-self: stretch;
    justify-content: flex-start;
    overflow-x: auto;
  }

  .admin-tabs :deep(.el-tabs__header) {
    margin-bottom: 16px;
  }

  .admin-tabs :deep(.el-tabs__nav-wrap),
  .admin-tabs :deep(.el-tabs__nav-scroll) {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .admin-tabs :deep(.el-tabs__nav-scroll::-webkit-scrollbar) {
    display: none;
  }

  .admin-tabs :deep(.el-tabs__item) {
    padding: 0 11px !important;
    height: 42px !important;
  }

  .tab-idx {
    display: none;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 23px;
  }

  .page-sub {
    font-size: 12px;
  }
}
</style>
