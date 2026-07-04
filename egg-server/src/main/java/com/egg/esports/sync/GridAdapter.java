package com.egg.esports.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GRID Open Access 通用 Adapter（CS2 / Dota 2 共用）
 * <p>通过 GRID Central Data GraphQL API 拉取赛事数据，构造器接受 titleId 区分游戏
 * <p>端点: https://api-op.grid.gg/central-data/graphql（注意是 api-op，非商业版 api.grid.gg）
 * <p>特性：
 * <ul>
 *   <li>GraphQL over HTTP，POST body: {"query": "...", "variables": {...}}</li>
 *   <li>cursor-based 分页，每页最大 50 条（first: 50）</li>
 *   <li>Central Data 限频 20 req/min，请求间间隔 3 秒</li>
 *   <li>allSeries 不返回状态字段，采用启发式状态推断（基于 startTimeScheduled）</li>
 *   <li>提取 teams[].baseInfo.logoUrl 作为队标，强制 HTTPS 防止 Mixed Content</li>
 * </ul>
 */
@Slf4j
public class GridAdapter implements GameSourceAdapter {

    /** 最大分页数保护（避免异常情况下死循环，50 * 20 = 1000 条） */
    private static final int MAX_PAGES = 20;

    /** 请求间隔（毫秒），Central Data 限频 20 req/min → 3 秒间隔 */
    private static final long REQUEST_INTERVAL_MS = 3000L;

    /** 时间窗口：向前 7 天 */
    private static final long LOOKBACK_DAYS = 7;

    /** 时间窗口：向后 30 天 */
    private static final long LOOKAHEAD_DAYS = 30;

    /** 启发式状态推断：进行中窗口（小时），开始后 3 小时内视为进行中 */
    private static final long ONGOING_WINDOW_HOURS = 3;

    /** GRID 期望的 UTC 时间格式（ISO 8601 带 Z 后缀） */
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .withZone(ZoneId.of("UTC"));

    private final Long gameId;
    private final String titleId;
    private final String apiKey;
    private final String centralDataUrl;
    private final int pageSize;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** GraphQL 查询模板（构造器里按 titleId / pageSize 动态生成） */
    private final String seriesQuery;

    /**
     * @param gameId          游戏ID（CS2=4, Dota2=3）
     * @param titleId        GRID 内部 title ID（CS2="28", Dota2="2"）
     * @param apiKey         GRID API Key
     * @param centralDataUrl GRID Central Data GraphQL 端点
     * @param restTemplate   HTTP 客户端（线程安全）
     * @param objectMapper   JSON 序列化/反序列化（线程安全）
     */
    public GridAdapter(Long gameId, String titleId, String apiKey,
                       String centralDataUrl, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.titleId = titleId;
        this.apiKey = apiKey;
        this.centralDataUrl = centralDataUrl;
        this.pageSize = 50;  // GRID 每页最大 50 条
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        // 构建查询模板：titleIds 过滤指定游戏，baseInfo 含 logoUrl 提取队标
        // 注意：startTimeScheduled 过滤器期望 String 类型（非 DateTime），传 ISO 8601 字符串
        this.seriesQuery = """
                query Series($after: Cursor, $gte: String, $lte: String) {
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
                          baseInfo { id name nameShortened logoUrl }
                        }
                      }
                    }
                  }
                }
                """.formatted(pageSize, titleId);
    }

    @Override
    public Long getGameId() {
        return gameId;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<RawMatch> fetchMatches() {
        List<RawMatch> result = new ArrayList<>();

        // 时间窗口：now - 7天 ~ now + 30天（北京时间）
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        LocalDateTime now = LocalDateTime.now(shanghaiZone);
        LocalDateTime fromLdt = now.minusDays(LOOKBACK_DAYS);
        LocalDateTime toLdt = now.plusDays(LOOKAHEAD_DAYS);

        // 转换为 UTC ISO 8601 格式（GRID 期望带 Z 后缀）
        String fromUtc = UTC_FORMATTER.format(fromLdt.atZone(shanghaiZone));
        String toUtc = UTC_FORMATTER.format(toLdt.atZone(shanghaiZone));

        log.info("GridAdapter(gameId={}, titleId={}) 开始同步赛事，时间窗口: {} ~ {} (UTC)",
                gameId, titleId, fromUtc, toUtc);

        // cursor-based 分页拉取
        String cursor = null;
        int totalPages = 0;
        int totalCount = -1;

        while (totalPages < MAX_PAGES) {
            totalPages++;
            // 不再 catch Exception：任一页网络失败会向上抛出，触发 @Retryable 整体重试
            // 已成功拉取的页在重试时会被重复处理（RawMatch 入库按 externalId 去重，可接受）
            JsonNode responseNode = executeGraphQLQuery(cursor, fromUtc, toUtc);
            if (responseNode == null) {
                break;
            }

            JsonNode allSeries = responseNode.at("/data/allSeries");
            if (allSeries.isMissingNode()) {
                log.warn("GRID 响应缺少 data.allSeries 节点，停止同步");
                break;
            }

            // 首次记录 totalCount（用于日志展示）
            if (totalCount < 0) {
                totalCount = allSeries.path("totalCount").asInt(0);
                log.info("GRID(titleId={}) 赛事总数: {}", titleId, totalCount);
            }

            JsonNode edges = allSeries.path("edges");
            if (!edges.isArray() || edges.isEmpty()) {
                log.info("GRID 第 {} 页无赛事数据，结束分页", totalPages);
                break;
            }

            // 映射当前页赛事（单个失败不阻塞整体）
            for (JsonNode edge : edges) {
                try {
                    RawMatch raw = mapToRawMatch(edge.path("node"), now);
                    if (raw != null) {
                        result.add(raw);
                    }
                } catch (Exception e) {
                    // 单条数据映射异常为业务异常，不影响整体同步
                    log.error("映射 GRID 赛事失败: {}", e.getMessage(), e);
                }
            }

            // 检查是否有下一页
            JsonNode pageInfo = allSeries.path("pageInfo");
            boolean hasNextPage = pageInfo.path("hasNextPage").asBoolean(false);
            String endCursor = pageInfo.path("endCursor").asText(null);

            log.info("GRID(titleId={}) 第 {} 页处理完成，本页 {} 条，累计 {} 条，hasNextPage={}",
                    titleId, totalPages, edges.size(), result.size(), hasNextPage);

            if (!hasNextPage || endCursor == null || endCursor.isEmpty()) {
                break;
            }

            cursor = endCursor;

            // 限频：请求间间隔 3 秒（Central Data 20 req/min）
            // InterruptedException 单独处理：线程中断时退出循环，不触发重试
            try {
                Thread.sleep(REQUEST_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.warn("GridAdapter(gameId={}) 同步被中断", gameId);
                break;
            }
        }

        log.info("GridAdapter(gameId={}, titleId={}) 同步完成，共 {} 场赛事（共 {} 页）",
                gameId, titleId, result.size(), totalPages);
        return result;
    }

    /**
     * 执行 GRID GraphQL 查询（单页）
     * <p>网络异常（RestClientException）和 JSON 解析异常（包装为 RuntimeException）均向上抛出，
     * 由 fetchMatches 的 @Retryable 触发整体重试。
     *
     * @param after   cursor（首页传 null）
     * @param fromUtc 起始时间（UTC ISO 8601，如 2026-06-24T00:00:00Z）
     * @param toUtc   截止时间（UTC ISO 8601）
     * @return 响应 JsonNode（根节点）；GraphQL 返回 errors 字段时返回 null（业务错误不重试）
     */
    private JsonNode executeGraphQLQuery(String after, String fromUtc, String toUtc) {
        // 构建 GraphQL request body: {"query": "...", "variables": {...}}
        Map<String, Object> variables = new HashMap<>();
        variables.put("after", after);
        variables.put("gte", fromUtc);
        variables.put("lte", toUtc);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("query", seriesQuery);
        requestBody.put("variables", variables);

        // writeValueAsString 抛 JsonProcessingException（检查异常），包装为 RuntimeException 向上抛出
        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            log.error("GRID(titleId={}) 序列化请求体失败: {}", titleId, e.getMessage(), e);
            throw new RuntimeException("序列化 GRID GraphQL 请求体失败", e);
        }

        // 构建请求头：x-api-key 认证 + JSON Content-Type
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        log.info("请求 GRID(titleId={}): after={}, from={}, to={}", titleId, after, fromUtc, toUtc);

        // restTemplate.exchange 抛出 RestClientException（运行时异常），直接向上传播触发重试
        ResponseEntity<String> response = restTemplate.exchange(
                centralDataUrl, HttpMethod.POST, entity, String.class);

        // readTree 抛 JsonProcessingException（检查异常），包装为 RuntimeException 向上抛出
        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            log.error("GRID(titleId={}) 解析响应 JSON 失败: {}", titleId, e.getMessage(), e);
            throw new RuntimeException("解析 GRID GraphQL 响应 JSON 失败", e);
        }

        // 检查 GraphQL errors 字段：业务级错误（如变量不合法）不触发重试，返回 null 退出循环
        JsonNode errors = root.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            log.error("GRID(titleId={}) GraphQL 返回错误: {}", titleId, errors.toString());
            return null;
        }

        return root;
    }

    /**
     * 将 GRID series node 映射为 RawMatch
     * <p>安全取值（.path() 避免 NPE），跳过 teams 不足 2 个的无效数据
     *
     * @param node GRID series node（edges[].node）
     * @param now  当前时间（用于启发式状态推断）
     * @return RawMatch；无效数据返回 null
     */
    private RawMatch mapToRawMatch(JsonNode node, LocalDateTime now) {
        // externalId = node.id
        String externalId = node.path("id").asText("");
        if (externalId.isEmpty()) {
            log.warn("GRID 赛事缺少 id，跳过");
            return null;
        }

        // teams 数组校验（GRID teams[] 顺序固定，teams[0]=A, teams[1]=B）
        JsonNode teams = node.path("teams");
        if (!teams.isArray() || teams.size() < 2) {
            log.warn("GRID 赛事 {} teams 不足 2 个，跳过", externalId);
            return null;
        }

        JsonNode team0Base = teams.get(0).path("baseInfo");
        JsonNode team1Base = teams.get(1).path("baseInfo");

        // teamA / teamB = teams[*].baseInfo.name
        String teamA = team0Base.path("name").asText("");
        String teamB = team1Base.path("name").asText("");

        if (teamA.isEmpty() || teamB.isEmpty()) {
            log.warn("GRID 赛事 {} 队伍名称为空，跳过 (teamA={}, teamB={})", externalId, teamA, teamB);
            return null;
        }

        // 队伍图标 = teams[*].baseInfo.logoUrl，强制 HTTPS 防止 Mixed Content
        String logoA = team0Base.path("logoUrl").asText(null);
        String logoB = team1Base.path("logoUrl").asText(null);

        // title = "teamA vs teamB"（不依赖 GRID title 字段，自行拼接）
        String title = teamA + " vs " + teamB;

        // tournament = node.tournament.name
        String tournament = node.path("tournament").path("name").asText("");

        // matchTime = node.startTimeScheduled (ISO 8601 UTC → LocalDateTime Asia/Shanghai)
        String startTimeStr = node.path("startTimeScheduled").asText("");
        if (startTimeStr.isEmpty()) {
            log.warn("GRID 赛事 {} 缺少 startTimeScheduled，跳过", externalId);
            return null;
        }

        LocalDateTime matchTime;
        try {
            matchTime = OffsetDateTime.parse(startTimeStr)
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("GRID 赛事 {} startTimeScheduled 解析失败: {}", externalId, startTimeStr);
            return null;
        }

        // 启发式状态推断（GRID allSeries 不返回状态字段）
        int status = inferStatus(matchTime, now);

        // 构建 RawMatch（不填 result/scoreA/scoreB，v2 保持启发式状态；logo 来自 GRID logoUrl）
        RawMatch raw = new RawMatch();
        raw.setExternalId(externalId);
        raw.setGameId(gameId);
        raw.setTitle(title);
        raw.setTeamA(teamA);
        raw.setTeamB(teamB);
        raw.setTeamALogo(toHttps(logoA));
        raw.setTeamBLogo(toHttps(logoB));
        raw.setTournament(tournament);
        raw.setMatchTime(matchTime);
        raw.setScoreA(null);
        raw.setScoreB(null);
        raw.setStatus(status);
        raw.setResult(null);

        return raw;
    }

    /**
     * 强制将 http:// 替换为 https://，并过滤 GRID 通用占位图
     * <p>防止浏览器 Mixed Content 警告（HTTPS 页面加载 HTTP 图片）
     * <p>null、空字符串、或 GRID 的 generic 占位图统一返回 null（前端用首字母 placeholder 替代）
     */
    private String toHttps(String url) {
        if (url == null || url.isEmpty() || url.contains("/generic")) {
            return null;
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    /**
     * 启发式状态推断（基于 startTimeScheduled）
     * <ul>
     *   <li>matchTime 在未来 → 0（未开始）</li>
     *   <li>matchTime 早于 now-3h → 2（已结束）</li>
     *   <li>其他（now-3h ~ now）→ 1（进行中）</li>
     * </ul>
     * <p>后续可通过 Series State API 获取精确状态和比分
     */
    private int inferStatus(LocalDateTime matchTime, LocalDateTime now) {
        if (matchTime.isAfter(now)) {
            return 0;  // 未开始
        }
        if (matchTime.isBefore(now.minusHours(ONGOING_WINDOW_HOURS))) {
            return 2;  // 已结束
        }
        return 1;  // 进行中
    }
}
