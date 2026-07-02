# 前端 CS2 + GRID 适配任务书

**创建时间**: 2026-07-01
**状态**: 待执行（发给 Trae）
**改动量**: 2 个文件，~20 行

---

## 背景

后端已接入 GRID（CS2 + Dota 2），数据库 `egg_game` 表已有 CS2（id=4, status=1），前端 `/api/game/list` 动态渲染游戏列表，绝大部分组件**无需改动**即可自动适配。

仅两处需要手动调整：Home 页 Hero 主题色，管理后台同步页。

---

## 1. Home.vue — 加 CS2 Hero 主题色

**文件**: `src/views/Home.vue`

### 1.1 computed: heroThemeClass（约 L358-364）

在现有三个主题后加 CS2 分支：

```javascript
heroThemeClass() {
    if (this.selectedGameId === 1) return 'hero--valorant'
    if (this.selectedGameId === 2) return 'hero--lol'
    if (this.selectedGameId === 3) return 'hero--dota'
    if (this.selectedGameId === 4) return 'hero--cs2'     // ← 新增
    return 'hero--default'
}
```

### 1.2 CSS: 新增 `.hero--cs2` 样式（约 L558-588 之后）

在 `.hero--dota` 后面加：

```css
.hero--cs2 {
  background:
    radial-gradient(ellipse 80% 100% at 20% 50%, rgba(222,155,40,0.30) 0%, transparent 50%),
    radial-gradient(ellipse 60% 80% at 85% 30%, rgba(235,179,52,0.20) 0%, transparent 50%),
    radial-gradient(ellipse 40% 60% at 50% 80%, rgba(222,155,40,0.10) 0%, transparent 60%),
    #0d0c09;
}
```

> CS2 经典配色：橙金/暗色底。参考现有瓦（红蓝）、LoL（蓝金）、Dota（紫红）的风格。

---

## 2. AdminDashboard.vue — 同步页改版

**文件**: `src/views/admin/AdminDashboard.vue`

### 2.1 同步按钮文案更新（约 L197-209）

**改前**:
```html
<el-button ... @click="triggerSync">
  {{ syncLoading ? '正在同步...' : '从 Riot API 同步赛事数据' }}
</el-button>
```

**改后**:
```html
<el-button ... @click="triggerSync">
  {{ syncLoading ? '正在同步...' : '同步全部赛事数据（Riot + GRID）' }}
</el-button>
```

### 2.2 Per-Game Stats 标签人类可读（约 L243-248）

**改前**（显示 raw key 如 "game-1"、"game-2"）:
```html
<div v-for="(stats, gameKey) in syncResult.perGameStats" :key="gameKey" class="sync-per-game-item">
  <span class="sync-per-game-label">{{ gameKey }}:</span>
  <span class="sync-per-game-val">+{{ stats[0] }} ~{{ stats[1] }} -{{ stats[2] }}</span>
</div>
```

**改后**（映射为游戏名）:
```html
<div v-for="(stats, gameKey) in syncResult.perGameStats" :key="gameKey" class="sync-per-game-item">
  <span class="sync-per-game-label">{{ getGameNameByKey(gameKey) }}:</span>
  <span class="sync-per-game-val">+{{ stats[0] }} ~{{ stats[1] }} -{{ stats[2] }}</span>
</div>
```

**methods 新增**:
```javascript
getGameNameByKey(key) {
  var map = {
    'game-1': '无畏契约',
    'game-2': '英雄联盟',
    'game-3': 'Dota 2',
    'game-4': 'CS2'
  };
  return map[key] || key;
}
```

---

## 3. 不需要改的地方（确认清单）

| 组件 | 确认理由 |
|------|---------|
| Home.vue 游戏 Tab | `v-for="game in games"` — CS2 自动出现 |
| MatchList.vue 游戏 Tab | 同上 |
| AdminDashboard 游戏管理 Tab | `fetchGames()` → 动态加载，CS2 自动出现 |
| MatchCreate.vue 游戏下拉 | `<el-select>` 动态遍历 `games` 数组 |
| MatchEdit.vue | 同上 |
| AnnouncementBar 关联游戏 | 下拉框动态加载 `adminGames` |
| Leaderboard.vue | 无游戏筛选逻辑 |
| App.vue 导航栏 | 无游戏硬编码 |
| router/index.js | 无游戏相关路由 |

---

## 4. 验证

```bash
# 前端
cd D:\egg-esports\egg-web
npm run serve

# 确认：
# 1. 首页游戏 Tab 中出现 "CS2"
# 2. 选中 CS2 后 Hero 区变为橙金色主题
# 3. 管理后台 → 游戏管理 → CS2 出现在列表中
# 4. 管理后台 → 数据同步 → 按钮文案为 "同步全部赛事数据（Riot + GRID）"
# 5. 同步后 per-game stats 显示 "CS2:" 而非 "game-4:"
```

---

## 5. 文件变更总览

| 操作 | 文件 | 行数 |
|------|------|------|
| ✏️ 修改 | `src/views/Home.vue` | +8 行（1 行 computed + 7 行 CSS） |
| ✏️ 修改 | `src/views/admin/AdminDashboard.vue` | +10 行（按钮文案 + getGameNameByKey 方法 + 模板绑定） |

**预估工作量**: 10 分钟
