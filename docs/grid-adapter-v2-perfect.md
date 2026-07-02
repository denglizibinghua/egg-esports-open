# GridAdapter v2 完善任务书 — 队标 + Dota 2 双源

**创建时间**: 2026-07-01
**状态**: 待执行（发给 Trae）
**前置**: GridAdapter v1 已完成（CS2 基本同步跑通）

---

## 0. 背景：v1 的两个缺口

v1 把 CS2 基本数据拉回来了，但离"完美"差两件事：

| 缺口 | 现状 | 目标 |
|------|------|------|
| 队伍图标 | `teamALogo=null, teamBLogo=null` | 从 GRID `logoUrl` 提取，转 HTTPS |
| Dota 2 | OpenDota（无队标、仅已结束比赛） | 切到 GRID（有队标、全生命周期） |

另外 v1 的 GridAdapter 是 CS2 硬编码版，需要重构为**通用 Adapter**（titleId 参数化），一份代码同时服务 CS2 和 Dota 2。

---

## 1. 目标

- ✅ CS2 队伍图标提取（`logoUrl` → `RawMatch.teamALogo/teamBLogo`）
- ✅ HTTPS 强制（`http://` → `https://`，防止 Mixed Content）
- ✅ GridAdapter 重构为通用版（构造器接受 `titleId`）
- ✅ 新增 Dota 2 数据源（GRID，替换 OpenDota）
- ✅ 旧 OpenDota 平滑下线（yml 开关关掉即可）
- ✅ 代码零重复，CS2 和 Dota 2 共享同一份 Adapter 逻辑

---

## 2. 实现步骤

### 2.1 重构 GridAdapter：titleId 参数化

**文件**: `egg-server/src/main/java/com/egg/esports/sync/GridAdapter.java`

**变更**:
1. 构造器新增 `String titleId` 参数
2. GraphQL 查询模板改为 `titleIds: { in: ["%s"] }`（用 `titleId` 替换硬编码的 `"3"`）
3. 日志里带 titleId，方便区分 CS2 / Dota 2
4. 外提 `CENTRAL_DATA_URL` 为 `@Value` 注入（从 yml 读取）

**新版构造器签名**:
```java
public GridAdapter(Long gameId, String titleId, String apiKey,
                   String centralDataUrl, RestTemplate restTemplate, ObjectMapper objectMapper)
```

**新版查询模板**（关键变化）:
```java
// 去掉硬编码的 PAGE_SIZE 和 CS2_TITLE_ID 常量
// 改为实例字段：this.titleId, this.pageSize
// 查询在构造器里动态生成
private final String seriesQuery;  // 实例字段，构造器里 build

// 构造器末尾：
this.seriesQuery = """
    query Series($after: Cursor, $gte: DateTime, $lte: DateTime) {
      allSeries(
        first: %d
        after: $after
        filter: {
          startTimeScheduled: { gte: $gte, lte: $lte }
          titleIds: { in: ["%s"] }
        }
        orderBy: StartTimeScheduled
      ) {
        totalCount
        pageInfo { endCursor hasNextPage }
        edges {
          node {
            id
            startTimeScheduled
            tournament { name nameShortened }
            teams {
              baseInfo { id name nameShortened logoUrl }
            }
          }
        }
      }
    }
    """.formatted(pageSize, titleId);
```

> **注意**: GraphQL 查询里 `teams.baseInfo` 比 v1 多了 `logoUrl` 字段

### 2.2 提取队伍图标 + HTTPS 强制

在 `mapToRawMatch()` 中新增 logo 映射（紧接 teamA/teamB 赋值之后）:

```java
// teamA = teams[0].baseInfo.name（保持不变，略）
// teamB = teams[1].baseInfo.name（保持不变，略）

// ===== 新增：队伍图标 + HTTPS 强制 =====
String logoA = teams.get(0).path("baseInfo").path("logoUrl").asText(null);
String logoB = teams.get(1).path("baseInfo").path("logoUrl").asText(null);
raw.setTeamALogo(logoA != null ? toHttps(logoA) : null);
raw.setTeamBLogo(logoB != null ? toHttps(logoB) : null);
```

**`toHttps()` 方法**（参考 `RiotAdapter` 或此处新写，放 GridAdapter 内）:

```java
/**
 * 强制将 http:// 替换为 https://
 * <p>防止浏览器 Mixed Content 警告（HTTPS 页面加载 HTTP 图片）
 */
private String toHttps(String url) {
    if (url == null || url.isEmpty()) return null;
    if (url.startsWith("http://")) {
        return "https://" + url.substring(7);
    }
    return url;
}
```

**注意**: RiotAdapter 里已经有 `toHttps()`，如果它是 `public static` 可以直接复用。否则在 GridAdapter 里新写一个（复制 4 行，不丢人）。

### 2.3 更新 SyncAdapterConfig：注册 CS2 + Dota 2 双 Bean

**文件**: `egg-server/src/main/java/com/egg/esports/config/SyncAdapterConfig.java`

**变更**:
1. 修改 `gridCs2Adapter` Bean：构造器传入 `titleId="3"` + `centralDataUrl`
2. 新增 `gridDota2Adapter` Bean：gameId=3，titleId="5"，替换 OpenDota

**完整代码**（替换现有 gridCs2Adapter Bean）:

```java
// ========== GRID: CS2 (gameId=4, titleId=3) ==========
@Bean
@ConditionalOnProperty(prefix = "egg.sync.adapters.grid-cs2", name = "enabled", havingValue = "true", matchIfMissing = true)
public GameSourceAdapter gridCs2Adapter(
        @Value("${egg.sync.adapters.grid-cs2.game-id:4}") Long gameId,
        @Value("${egg.sync.adapters.grid-cs2.title-id:3}") String titleId,
        @Value("${egg.sync.adapters.grid-cs2.api-key:${egg.grid.api-key}}") String apiKey,
        @Value("${egg.grid.central-data-url}") String centralDataUrl,
        RestTemplate restTemplate, ObjectMapper objectMapper) {
    log.info("注册 GridCs2Adapter: gameId={}, titleId={}", gameId, titleId);
    return new GridAdapter(gameId, titleId, apiKey, centralDataUrl, restTemplate, objectMapper);
}

// ========== GRID: Dota 2 (gameId=3, titleId=5) ==========
@Bean
@ConditionalOnProperty(prefix = "egg.sync.adapters.grid-dota2", name = "enabled", havingValue = "true", matchIfMissing = false)
public GameSourceAdapter gridDota2Adapter(
        @Value("${egg.sync.adapters.grid-dota2.game-id:3}") Long gameId,
        @Value("${egg.sync.adapters.grid-dota2.title-id:5}") String titleId,
        @Value("${egg.sync.adapters.grid-dota2.api-key:${egg.grid.api-key}}") String apiKey,
        @Value("${egg.grid.central-data-url}") String centralDataUrl,
        RestTemplate restTemplate, ObjectMapper objectMapper) {
    log.info("注册 GridDota2Adapter: gameId={}, titleId={}", gameId, titleId);
    return new GridAdapter(gameId, titleId, apiKey, centralDataUrl, restTemplate, objectMapper);
}
```

**Javadoc 更新**: 类注释的 `<ul>` 加入:
```html
<li>gridCs2Adapter — CS2（gameId=4）</li>
<li>gridDota2Adapter — Dota 2（gameId=3）</li>
```

**import**: 确认 `GridAdapter` 已 import（v1 已有）

### 2.4 更新 application.yml

**文件**: `egg-server/src/main/resources/application.yml`

**变更**: 修改 `grid-cs2` 段 + 新增 `grid-dota2` 段 + 关掉 `opendota`

```yaml
      opendota:
        enabled: false        # ← 改为 false（GRID 替代 OpenDota）
        game-id: 3
      grid-cs2:
        enabled: true
        game-id: 4
        title-id: 3           # ← 新增：GRID 内部 CS2 title ID
        api-key: ${GRID_API_KEY}
      grid-dota2:              # ← 新增整段
        enabled: true
        game-id: 3
        title-id: 5           # ← GRID 内部 Dota 2 title ID（⚠️ 需验证）
        api-key: ${GRID_API_KEY}
```

**注意**: `grid-dota2` 和 `opendota` 都映射到 `game-id=3`。yml 配置不冲突（`@ConditionalOnProperty` 按 prefix 隔离），但 `opendota.enabled: false` 确保只启用一个。

### 2.5 验证 Dota 2 title ID

**⚠️ 关键**: Dota 2 的 GRID title ID 是 `"5"`（从 pygrid 项目推断），**上线前必须验证**。

验证方法（本地跑一下）:
```bash
curl -s -X POST https://api-op.grid.gg/central-data/graphql \
  -H "Content-Type: application/json" \
  -H "x-api-key: ${GRID_API_KEY}" \
  -d '{"query":"query { titles { id name } }"}' | python -m json.tool
```

预期输出包含:
```json
{"id": "3", "name": "Counter-Strike 2"}
{"id": "5", "name": "Dota 2"}
```

如果 Dota 2 的 id 不是 `"5"`，修改 yml 中 `grid-dota2.title-id` 为正确值。

---

## 3. 不要做的事

- ❌ **不要删 OpenDotaAdapter.java** — 只关 yml 开关，代码留着以备回退
- ❌ **不要清空 egg_match 表** — 旧 OpenDota Dota 2 数据保留
- ❌ **不要改 MatchSyncService / EggMatchService / RawMatch / SyncResult** — 这些完全不动
- ❌ **不要碰前端** — 前端通过 EggGameController 自动感知 gameId=3/4 的赛事
- ❌ **不要加 Series State API（v2 的事）** — v2 保持启发式状态推断

---

## 4. 验证步骤

### 4.1 编译检查
```bash
cd D:\egg-esports\egg-server
mvn compile
# 确保 GridAdapter 新构造器签名通过，SyncAdapterConfig 双 Bean 注入无误
```

### 4.2 启动后端
```
IDEA 打开 pom.xml → Run EggApplication
观察启动日志应出现:
  "注册 GridCs2Adapter: gameId=4, titleId=3"
  "注册 GridDota2Adapter: gameId=3, titleId=5"
不应出现:
  "注册 OpenDotaAdapter"（因为 enabled: false）
```

### 4.3 手动触发同步
```bash
curl -X POST http://localhost:8088/api/match/sync \
  -H "Authorization: Bearer <admin-jwt>"
```

### 4.4 验证结果

```sql
-- CS2 数据
SELECT COUNT(*) FROM egg_match WHERE game_id = 4;
SELECT id, title, team_a, team_b, team_a_logo, team_b_logo, match_time, status
FROM egg_match WHERE game_id = 4 LIMIT 5;

-- Dota 2 数据（新源）
SELECT COUNT(*) FROM egg_match WHERE game_id = 3;
SELECT id, title, team_a, team_b, team_a_logo, team_b_logo, match_time, status
FROM egg_match WHERE game_id = 3 ORDER BY id DESC LIMIT 5;
```

### 4.5 验收标准

- [ ] 编译通过，无报错
- [ ] 启动日志显示 GridCs2Adapter + GridDota2Adapter 注册成功
- [ ] 启动日志**不**显示 OpenDotaAdapter 注册
- [ ] CS2 赛事 `team_a_logo` 和 `team_b_logo` 有值（非 NULL）
- [ ] Dota 2 赛事 `team_a_logo` 和 `team_b_logo` 有值（非 NULL）
- [ ] 所有 logo URL 以 `https://` 开头（非 `http://`）
- [ ] 同步无报错（日志中无 ERROR 级记录，WARN 仅限个别赛事映射跳过）
- [ ] `perGameStats["game-3"]` 和 `perGameStats["game-4"]` 都有数据
- [ ] 重复同步不产生重复数据（`externalId + gameId` 唯一键去重）

---

## 5. 最终成果：完整数据源矩阵

| 游戏 | gameId | 数据源 | 队标 | 状态覆盖 |
|------|--------|--------|------|---------|
| 无畏契约 | 1 | Riot（官方） | ✅ | 未开始/进行中/已结束 |
| 英雄联盟 | 2 | Riot（官方） | ✅ | 未开始/进行中/已结束 |
| Dota 2 | 3 | GRID（官方） | ✅ | 未开始/进行中/已结束 |
| CS2 | 4 | GRID（官方） | ✅ | 未开始/进行中/已结束 |

全部 4 款游戏都有队标，全部覆盖完整赛事生命周期。这才叫"完美"。

---

## 6. 文件变更总览

| 操作 | 文件 | 变更内容 |
|------|------|---------|
| ✏️ 修改 | `sync/GridAdapter.java` | 构造器加 titleId/centralDataUrl；查询加 logoUrl；mapToRawMatch 加 logo 映射+toHttps |
| ✏️ 修改 | `config/SyncAdapterConfig.java` | gridCs2Adapter 加 titleId 参数；新增 gridDota2Adapter Bean |
| ✏️ 修改 | `resources/application.yml` | grid-cs2 加 title-id；新增 grid-dota2 段；opendota.enabled 改 false |
| ✏️ 修改 | `resources/application-prod.yml` | 同上（生产环境同步） |
| 🗑️ 不动 | `sync/OpenDotaAdapter.java` | 代码留作备源 |
| 🗑️ 不动 | 其他所有文件 | 无改动 |

**预估工作量**: 30-45 分钟

---

## 7. 参考文件（Trae 请读这些）

| 文件 | 用途 |
|------|------|
| `sync/GridAdapter.java`（v1） | 当前代码，在此基础上改 |
| `sync/RiotAdapter.java` | `toHttps()` 参考实现 |
| `sync/RawMatch.java` | `teamALogo`/`teamBLogo` 字段确认 |
| `config/SyncAdapterConfig.java` | Bean 注册模式 |
| `resources/application.yml` | yml 配置结构 |
| 本任务书 2.5 节 | Dota 2 title ID 验证命令 |

---

## 8. 注意事项

1. **title ID "5" 需验证** — 跑一下 2.5 节的 curl 命令，确认 Dota 2 在 GRID 的 ID 是 `"5"`
2. **logoUrl 可能为 null** — 部分冷门队伍没队标，`asText(null)` 会返回 null，`toHttps()` 对 null 返回 null，无 NPE
3. **构造器参数顺序**: `(gameId, titleId, apiKey, centralDataUrl, restTemplate, objectMapper)` — 保持和 SyncAdapterConfig 调用一致
4. **不要删 OpenDotaAdapter.java** — yml 关了就是，万一 GRID 出问题可以秒切回来
5. **旧 Dota 2 数据不冲突** — OpenDota 的 externalId 是数字字符串如 `"1234567"`，GRID 的是 `"2881666"` 格式，`upsertByExternalId` 按 `(externalId, gameId)` 联合唯一键去重，不会冲突
