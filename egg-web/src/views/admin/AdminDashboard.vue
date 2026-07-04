<template>
  <div class="admin-dashboard">
    <div class="page-header">
      <h2 class="page-title">管理后台</h2>
    </div>

    <!-- 管理后台主体：5 个 Tab，分别由 4 个子组件承载 -->
    <!-- 路由权限校验（requiresAdmin）在 router/index.js 全局守卫中完成，组件内不重复 -->
    <el-tabs v-model="activeTab">
      <!-- 赛事管理：赛事列表 + 状态筛选 + 结算 -->
      <el-tab-pane label="赛事管理" name="match">
        <MatchManageTab ref="matchListRef" mode="list" />
      </el-tab-pane>

      <!-- 批量导入：JSON 解析 + 预览 + 提交。lazy 保证首次切到才挂载，与原行为一致 -->
      <el-tab-pane label="批量导入" name="import" lazy>
        <MatchManageTab mode="import" @import-success="onImportSuccess" />
      </el-tab-pane>

      <!-- 数据同步：触发同步 + 结果统计 -->
      <el-tab-pane label="数据同步" name="sync" lazy>
        <SyncTab />
      </el-tab-pane>

      <!-- 公告管理：列表 + 增删改。gamesVersion 用于跨 Tab 刷新关联游戏下拉 -->
      <el-tab-pane label="公告管理" name="announcement" lazy>
        <AnnouncementTab :games-version="gamesVersion" />
      </el-tab-pane>

      <!-- 游戏管理：列表 + 增删改。增删改后 emit games-changed 驱动公告下拉刷新 -->
      <el-tab-pane label="游戏管理" name="game" lazy>
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
.page-header {
  margin-bottom: 24px;
}

.page-title {
  color: var(--text-primary);
  font-family: var(--font-display);
  font-size: 32px;
  letter-spacing: 1px;
}

/* ===== 移动端基础适配 ===== */
@media (max-width: 768px) {
  .page-title {
    font-size: 26px;
  }
}

@media (max-width: 480px) {
  .page-title {
    font-size: 22px;
  }
}
</style>
