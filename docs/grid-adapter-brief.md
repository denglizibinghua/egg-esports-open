# GridAdapter 开发任务书 — 接入 GRID Open Access

**创建时间**: 2026-07-01
**状态**: 待执行（发给 Trae）
**目标**: 新增 GridAdapter，从 GRID Open Access 拉取 CS2 赛事数据，扩展 egg-esports 游戏覆盖面

---

## 1. 背景

egg-esports 当前有 3 款游戏的数据源：

| 游戏 | gameId | Adapter | 数据质量 |
|------|--------|---------|---------|
| 无畏契约 | 1 | RiotAdapter | ✅ 6 个联赛，Riot 官方 |
| 英雄联盟 | 2 | RiotAdapter | ✅ 5 个联赛，Riot 官方 |
| Dota 2 | 3 | OpenDotaAdapter | ⚠️ 第三方，仅已结束比赛 |

**缺失**: 平台完全没有 **CS2（Counter-Strike 2）** 的赛事数据。CS2 是全球第二大电竞项目，缺失严重影响平台内容完整性。

GRID Open Access 已获批（API Key 通过环境变量 ${GRID_API_KEY} 注入，不入库），覆盖 CS2 + Dota 2 共 87,000+ 场官方比赛数据。

---

## 2. 目标

- 新增 `GridAdapter`，实现 `GameSourceAdapter` 接口
- 从 GRID Central Data GraphQL API 拉取 CS2 赛事
- 注册为 Spring Bean，无缝融入现有同步架构（`MatchSyncService.syncAll()` 自动遍历）
- 新增游戏 ID=4（CS2），数据库 + 前端可见

---

## 3. 技术设计

### 3.1 GRID API 总览

| 项目 | 值 |
|------|-----|
| 端点 | `https://api-op.grid.gg/central-data/graphql` |
| 方法 | POST（GraphQL over HTTP） |
| 认证 | Header: `x-api-key: {key}` |
| 协议 | GraphQL（cursor 分页） |
| 限频 | Central Data 20 req/min，Series State 180 req/min（overall）/ 6 req/min per series |
| 分页 | Cursor-based，默认 10 条/页，最大 50 条/页（用 `first: 50`） |
| 数据范围 | CS2 + Dota 2（OA 不含瓦/LoL/R6） |

### 3.2 核心 GraphQL 查询

```graphql
query CS2Series($after: Cursor, $gte: DateTime, $lte: DateTime) {
  allSeries(
    first: 50
    after: $after
    filter: {
      startTimeScheduled: { gte: $gte, lte: $lte }
      titleIds: { in: ["3"] }       # 3 = CS2（GRID 内部 title ID）
    }
    orderBy: StartTimeScheduled
  ) {
    totalCount
    pageInfo {
      endCursor
      hasNextPage
    }
    edges {
      node {
        id
        startTimeScheduled
        tournament { name nameShortened }
        teams {
          baseInfo { id name nameShortened }
        }
      }
    }
  }
}
```

**变量示例**:
```json
{
  "gte": "2026-06-24T00:00:00Z",
  "lte": "2026-07-31T23:59:59Z"
}
```

### 3.3 状态获取策略（v1）

GRID Central Data 的 `allSeries` 查询**不返回比赛状态字段**。v1 采用**启发式状态推断**：

```java
// 根据 startTimeScheduled 推断状态
if (matchTime.isAfter(LocalDateTime.now())) {
    status = 0;  // 未来 → 未开始
} else if (matchTime.isBefore(LocalDateTime.now().minusHours(3))) {
    status = 2;  // 3小时前 → 已结束
} else {
    status = 1;  // 近期 → 进行中
}
```

> **后续 v2**: 通过 Series State API（`live-data-feed/series-state/graphql`）批量查询已完成比赛的 `started`/`finished`/`teams[].won`/`teams[].score` 来获取精确状态和比分。

> **参考**: pygrid 项目（`github.com/Allan-Cao/pygrid`）的 `queries/series-state/SeriesState.graphql` 有完整的 Series State 查询示例。

### 3.4 数据映射：GRID → RawMatch

| RawMatch 字段 | GRID 来源 | 备注 |
|--------------|-----------|------|
| `externalId` | `node.id` | GRID series ID |
| `gameId` | 固定 `4` | CS2 |
| `title` | `"teamA vs teamB"`（拼接） | v1 不依赖 GRID title 字段，自行拼接 `teamA + " vs " + teamB` |
| `teamA` | `node.teams[0].baseInfo.name` | |
| `teamB` | `node.teams[1].baseInfo.name` | |
| `teamALogo` | `null` | GRID OA 不提供队标 |
| `teamBLogo` | `null` | GRID OA 不提供队标 |
| `tournament` | `node.tournament.name` | |
| `matchTime` | `node.startTimeScheduled` | ISO 8601 → `LocalDateTime` (Asia/Shanghai) |
| `status` | 启发式推断（见 3.3） | 未来=0, 近期=1, 3h前=2 |
| `result` | `null`（v1） | 后续通过 Series State API 补 |
| `scoreA` | `null`（v1） | 后续通过 Series State API 补 |
| `scoreB` | `null`（v1） | 后续通过 Series State API 补 |

### 3.5 分页策略

GRID 使用 cursor-based 分页（不是 offset）：

```
第1次请求: after=null → 获取 pageInfo.endCursor
第2次请求: after=endCursor → 获取下一个 endCursor
...直到 hasNextPage=false
```

### 3.6 时间窗口策略

每次同步拉取 **前 7 天 + 后 30 天** 的赛事：
- `from = now - 7天`
- `to = now + 30天`

在 `MatchSyncService.syncAll()` 中已有 `CUTOFF_DATE = 2026-01-01` 过滤，早于此日期的会被自动跳过。

---

## 4. 实现清单

### 4.1 新建文件

#### 📄 `GridAdapter.java`
**路径**: `egg-server/src/main/java/com/egg/esports/sync/GridAdapter.java`

```
职责：实现 GameSourceAdapter，封装 GRID GraphQL 调用和数据映射

结构（参考 RiotAdapter）：
  - 构造器注入：gameId, apiKey, restTemplate, objectMapper
  - fetchMatches(): 
    1. 构建时间窗口（now-7d ~ now+30d）
    2. 循环分页调用 executeGraphQLQuery()
    3. 遍历 edges，逐个 mapToRawMatch()
    4. 返回 List<RawMatch>
  - executeGraphQLQuery(after, from, to): 
    1. 构建 JSON body { query, variables }
    2. POST 到 central-data/graphql
    3. 反序列化响应
    4. 返回 pageInfo + edges
  - mapToRawMatch(node):
    1. 安全取值（.path() 避免 NPE）
    2. 映射字段 → RawMatch
    3. 跳过 teams.size() < 2 的无效数据
  - 异常处理：每个 catch 打 log，不抛异常阻塞整体同步
```

**关键实现细节**:
- GraphQL POST body 格式：
  ```json
  {
    "query": "query AllSeries($after: Cursor, $from: DateTime, $to: DateTime) { ... }",
    "variables": { "after": null, "from": "2026-06-24T00:00:00Z", "to": "2026-07-31T23:59:59Z" }
  }
  ```
- Header: `Content-Type: application/json` + `x-api-key: {key}`
- 反序列化路径：
  - `root.at("/data/allSeries/edges")` → 赛事列表
  - `root.at("/data/allSeries/pageInfo/endCursor")` → 下一页 cursor
  - `root.at("/data/allSeries/pageInfo/hasNextPage")` → 是否有下一页
- 使用 `RestTemplate.exchange(url, HttpMethod.POST, entity, String.class)` 发请求
- 线程安全：Adapter 是无状态 Bean，RestTemplate 和 ObjectMapper 都是线程安全的

#### 📄 `migration_v26_grid_cs2.sql`
**路径**: `egg-esports/docs/migration_v26_grid_cs2.sql`

```sql
-- Migration V26: GRID Open Access — CS2 游戏入口
INSERT INTO egg_game (id, name, sort_order, status) VALUES
    (4, 'CS2', 4, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

SELECT * FROM egg_game ORDER BY sort_order;
```

### 4.2 修改文件

#### ✏️ `SyncAdapterConfig.java`
**路径**: `egg-server/src/main/java/com/egg/esports/config/SyncAdapterConfig.java`

在现有 3 个 Bean 后新增：

```java
@Bean
@ConditionalOnProperty(prefix = "egg.sync.adapters.grid-cs2", name = "enabled", havingValue = "true", matchIfMissing = true)
public GameSourceAdapter gridCs2Adapter(
        @Value("${egg.sync.adapters.grid-cs2.game-id:4}") Long gameId,
        @Value("${egg.sync.adapters.grid-cs2.api-key:${egg.grid.api-key}}") String apiKey,
        RestTemplate restTemplate, ObjectMapper objectMapper) {
    log.info("注册 GridCs2Adapter: gameId={}", gameId);
    return new GridAdapter(gameId, apiKey, restTemplate, objectMapper);
}
```

#### ✏️ `application.yml`
**路径**: `egg-server/src/main/resources/application.yml`

在 `egg.sync.adapters:` 下新增：

```yaml
      grid-cs2:
        enabled: true
        game-id: 4
        api-key: ${GRID_API_KEY}
```

同时在 `egg:` 下新增 GRID 公共配置（与 riot 同级）：

```yaml
  grid:
    api-key: ${GRID_API_KEY}
    central-data-url: https://api-op.grid.gg/central-data/graphql
```

#### ✏️ `application-prod.yml`
**路径**: `egg-server/src/main/resources/application-prod.yml`

无需改动（GridAdapter 使用默认 API Key，生产环境通过环境变量 `GRID_API_KEY` 覆盖）。

---

## 5. 不做的事（v1 范围外）

- ❌ **比分/结果同步**: v1 仅同步赛程（status + 基本信息），不处理 scoreA/scoreB/result。后续可通过 Series State API（`live-data-feed/series-state/graphql`）补
- ❌ **队标图片**: GRID OA 不提供队标图，保持 `null`
- ❌ **Dota 2 双源**: OpenDotaAdapter 已覆盖 Dota 2，v1 不引入 GRID Dota 2 避免重复
- ❌ **前端改动**: 仅后端，`EggGameController.listActive()` 会自动返回 gameId=4，前端无需改动即可展示 CS2 赛事
- ❌ **WebSocket 实时推送**: 现有架构通过定时轮询即可

---

## 6. 验证步骤

### 6.1 本地测试

```bash
# 1. 执行迁移
mysql -u admin -padmin123 egg_esports < docs/migration_v26_grid_cs2.sql

# 2. 启动后端，观察启动日志
# 应看到: "注册 GridCs2Adapter: gameId=4"

# 3. 手动触发同步
curl -X POST http://localhost:8088/api/match/sync \
  -H "Authorization: Bearer <admin-jwt>"

# 4. 查看日志输出
# 应看到: "开始同步 adapter: GridAdapter (gameId=4)"
# 以及 CS2 赛事的获取/新增/更新/跳过统计

# 5. 验证数据库
mysql> SELECT COUNT(*) FROM egg_match WHERE game_id = 4;
# 应有数据
mysql> SELECT id, title, tournament, match_time, status FROM egg_match WHERE game_id = 4 LIMIT 10;
```

### 6.2 验收标准

- [ ] GridAdapter 启动时被注册为 Bean（日志可见）
- [ ] `POST /api/match/sync` 返回的 `perGameStats["game-4"]` 有数据
- [ ] `egg_match` 表中 `game_id=4` 的记录不为空
- [ ] 同步日志无异常（ERROR 日志仅限个别赛事映射失败，不阻塞整体）
- [ ] CS2 赛事在前端赛事列表可见（gameId=4 的比赛自动展示）
- [ ] 重复同步不产生重复数据（`upsertByExternalId` 通过 `externalId + gameId` 唯一键去重）

---

## 7. 注意事项

1. **GraphQL 不是 REST**: POST body 是 `{"query": "...", "variables": {...}}`，响应格式是 `{"data": {...}}`，不是 REST 风格的扁平 JSON
2. **分页是 cursor-based**: 不是 `page=1&size=100`，是 `after: "cursor_string"`。每次请求拿 `pageInfo.endCursor` 作为下一次的 `after`。每页最大 50 条（`first: 50`），不是 100
3. **Rate Limit 严格**: Central Data 仅 20 req/min。不要并发请求，请求间加 `Thread.sleep(3000)`（3 秒间隔 = 最多 20 req/min）
4. **时间格式**: GRID 使用 ISO 8601（`2026-06-24T12:00:00Z`），映射到 `LocalDateTime` 时注意时区转换（UTC → Asia/Shanghai）
5. **API Key 不要暴露**: application.yml 中用环境变量 `${GRID_API_KEY}` 占位，默认值仅开发用。生产通过 systemd 环境变量注入
6. **异常不阻塞**: 单个赛事映射失败只打 log 不抛异常，参考 RiotAdapter L79-81
7. **teamA/teamB 顺序**: GRID teams[] 的顺序是固定的，首次入库后不会因 API 翻转导致队伍错位（`upsertByExternalId` 已锁定队伍名不更新）
8. **GRID OA 限制**: 仅 CS2 + Dota 2。LoL、瓦、R6 等不在 OA 范围。如需瓦/LoL 的 GRID 数据需付费升级
9. **⚠️ 端点域名**: 用 `api-op.grid.gg`（不是 `api.grid.gg`），后者是商业版
10. **⚠️ CS2 title ID**: 必须在查询中加 `titleIds: { in: ["3"] }` 过滤，否则会拉回 Dota 2 等其他游戏数据

---

## 8. 文件变更总览

| 操作 | 文件 | 行数估算 |
|------|------|---------|
| 新建 | `sync/GridAdapter.java` | ~180 行 |
| 新建 | `docs/migration_v26_grid_cs2.sql` | ~5 行 |
| 修改 | `config/SyncAdapterConfig.java` | +10 行 |
| 修改 | `resources/application.yml` | +6 行 |

**预估总工作量**: 1-2 小时（含调试）

---

## 9. 参考文件（Trae 请读这些）

**项目内**:

| 文件 | 用途 |
|------|------|
| `sync/GameSourceAdapter.java` | 接口定义 |
| `sync/RiotAdapter.java` | REST Adapter 参考实现（构造器、fetchMatches、mapToRawMatch 模式） |
| `sync/OpenDotaAdapter.java` | 简单 Adapter 参考实现 |
| `sync/RawMatch.java` | 中间 DTO 字段定义 |
| `sync/MatchSyncService.java` | 编排逻辑（看 syncAll() 如何遍历所有 adapter） |
| `sync/SyncResult.java` | 同步结果统计 |
| `config/SyncAdapterConfig.java` | Bean 注册模式（`@ConditionalOnProperty` 用法） |
| `module/match/EggMatchService.java` | upsertByExternalId 逻辑 |
| `resources/application.yml` | yml 配置结构 |
| `docs/migration_v12_multi_game.sql` | 添加新游戏的迁移参考 |

**外部参考**:

| 资源 | 用途 |
|------|------|
| `github.com/Allan-Cao/pygrid` | Python GRID 客户端，有完整的 GraphQL 查询定义（`queries/` 目录）和分页逻辑（`client.py`） |
| `grid.helpjuice.com/client-help/open-access-quickstart` | GRID OA 官方快速入门 |
| `grid.helpjuice.com/client-help/rate-limits-for-products` | 限频说明 |
