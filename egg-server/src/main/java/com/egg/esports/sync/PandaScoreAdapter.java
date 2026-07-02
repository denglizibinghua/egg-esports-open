package com.egg.esports.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PandaScore CS2 Adapter
 * <p>通过 PandaScore REST API 拉取 CS2 赛事（赛程 + 进行中 + 已结束）
 * <p>端点: https://api.pandascore.com/csgo/matches/{upcoming|running|past}
 * <p>特性：
 * <ul>
 *   <li>Bearer Token 认证（免费版 1,000 req/hr）</li>
 *   <li>三个端点聚合：upcoming(per_page=100) + running(per_page=50) + past(per_page=100)</li>
 *   <li>按 externalId("ps-"+id) 去重，避免三端点重叠</li>
 *   <li>请求间隔 ≥ 1 秒，429 超限时告警并跳过</li>
 *   <li>状态映射：not_started→0, running→1, finished→2</li>
 *   <li>仅 finished 状态提取 score 与 winner，队标强制 HTTPS</li>
 * </ul>
 */
@Slf4j
public class PandaScoreAdapter implements GameSourceAdapter {

    /** 请求间隔（毫秒），免费版限频宽松，保守取 1 秒 */
    private static final long REQUEST_INTERVAL_MS = 1000L;

    /** 429 超限后的等待时间（毫秒） */
    private static final long RATE_LIMIT_WAIT_MS = 5000L;

    private final Long gameId;
    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param gameId        游戏ID（CS2=4）
     * @param baseUrl       PandaScore API 根地址（https://api.pandascore.com）
     * @param apiKey        PandaScore API Key（Bearer Token）
     * @param restTemplate  HTTP 客户端（线程安全）
     * @param objectMapper  JSON 解析（线程安全）
     */
    public PandaScoreAdapter(Long gameId, String baseUrl, String apiKey,
                             RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long getGameId() {
        return gameId;
    }

    @Override
    public List<RawMatch> fetchMatches() {
        List<RawMatch> result = new ArrayList<>();
        // 按 externalId 去重（upcoming/running/past 可能存在重叠）
        Set<String> seen = new HashSet<>();

        // 三个端点聚合：即将开始、进行中、已结束
        // per_page 上限 100，running 数据量小用 50
        fetchFromEndpoint("/csgo/matches/upcoming?per_page=100", seen, result);
        sleep(REQUEST_INTERVAL_MS);
        fetchFromEndpoint("/csgo/matches/running?per_page=50", seen, result);
        sleep(REQUEST_INTERVAL_MS);
        fetchFromEndpoint("/csgo/matches/past?per_page=100", seen, result);

        log.info("PandaScore (CS2) 同步完成，共 {} 场赛事", result.size());
        return result;
    }

    /**
     * 拉取单个端点并映射为 RawMatch（追加到 result，按 externalId 去重）
     *
     * @param path   端点路径（含 query string）
     * @param seen   已处理的 externalId 集合（去重）
     * @param result 累积结果列表
     */
    private void fetchFromEndpoint(String path, Set<String> seen, List<RawMatch> result) {
        String url = baseUrl + path;
        try {
            JsonNode root = requestJson(url);
            if (root == null || !root.isArray()) {
                log.warn("PandaScore 端点 {} 响应非数组，跳过", path);
                return;
            }

            int mapped = 0;
            for (JsonNode node : root) {
                try {
                    RawMatch raw = mapToRawMatch(node);
                    if (raw == null) {
                        continue;
                    }
                    // 去重：三端点可能返回同一场比赛
                    if (seen.add(raw.getExternalId())) {
                        result.add(raw);
                        mapped++;
                    }
                } catch (Exception e) {
                    log.error("映射 PandaScore 赛事失败: {}", e.getMessage(), e);
                }
            }
            log.info("PandaScore 端点 {} 处理完成，本端点新增 {} 场，累计 {} 场",
                    path, mapped, result.size());
        } catch (Exception e) {
            log.error("PandaScore 端点 {} 拉取失败: {}", path, e.getMessage(), e);
        }
    }

    /**
     * 发送带 Bearer 认证的 GET 请求并返回 JSON 根节点
     * <p>HTTP 401（无效 Key）→ 记录错误并返回 null
     * <p>HTTP 429（超限）→ 等待后重试一次
     */
    private JsonNode requestJson(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 超限：等待后重试一次
            log.warn("PandaScore 触发限流(429)，等待 {}ms 后重试: {}", RATE_LIMIT_WAIT_MS, url);
            sleep(RATE_LIMIT_WAIT_MS);
            try {
                ResponseEntity<String> retry = restTemplate.exchange(
                        url, HttpMethod.GET, entity, String.class);
                return objectMapper.readTree(retry.getBody());
            } catch (Exception re) {
                log.error("PandaScore 重试仍失败: {}", re.getMessage(), re);
                return null;
            }
        } catch (HttpClientErrorException.Unauthorized e) {
            // 401 无效 Key：直接跳过，不重试
            log.error("PandaScore API Key 无效(401)，请检查配置 egg.sync.adapters.pandascore-cs2.api-key");
            return null;
        } catch (Exception e) {
            log.error("PandaScore 请求异常: {} - {}", url, e.getMessage());
            return null;
        }
    }

    /**
     * 将 PandaScore match 节点映射为 RawMatch
     * <p>opponents[*].opponent 存放队伍信息；results[*] 与 opponents 顺序对应
     */
    private RawMatch mapToRawMatch(JsonNode node) {
        // externalId = "ps-" + id
        long matchId = node.path("id").asLong(0);
        if (matchId == 0) {
            log.warn("PandaScore 赛事缺少 id，跳过");
            return null;
        }
        String externalId = "ps-" + matchId;

        // opponents 数组校验（PandaScore opponents[*].opponent 存放队伍）
        JsonNode opponents = node.path("opponents");
        if (!opponents.isArray() || opponents.size() < 2) {
            log.warn("PandaScore 赛事 {} opponents 不足 2 个，跳过", externalId);
            return null;
        }

        JsonNode team0 = opponents.get(0).path("opponent");
        JsonNode team1 = opponents.get(1).path("opponent");

        String teamA = team0.path("name").asText("");
        String teamB = team1.path("name").asText("");
        if (teamA.isEmpty() || teamB.isEmpty()) {
            log.warn("PandaScore 赛事 {} 队伍名称为空，跳过 (teamA={}, teamB={})", externalId, teamA, teamB);
            return null;
        }

        // 队标（可能为 null），强制 HTTPS
        String logoA = toHttps(team0.path("image_url").asText(null));
        String logoB = toHttps(team1.path("image_url").asText(null));

        // title = match.name（如 "TeamA vs TeamB"），兜底自行拼接
        String title = node.path("name").asText("");
        if (title.isEmpty()) {
            title = teamA + " vs " + teamB;
        }

        // tournament = league.name
        String tournament = node.path("league").path("name").asText("");

        // matchTime = begin_at (ISO 8601 UTC → Asia/Shanghai)
        String beginAt = node.path("begin_at").asText("");
        LocalDateTime matchTime;
        if (beginAt.isEmpty() || "null".equals(beginAt)) {
            // begin_at 可能为 null（未确定开赛时间），用 scheduled_at 兜底
            beginAt = node.path("scheduled_at").asText("");
            if (beginAt.isEmpty()) {
                log.warn("PandaScore 赛事 {} 缺少 begin_at/scheduled_at，跳过", externalId);
                return null;
            }
        }
        try {
            matchTime = OffsetDateTime.parse(beginAt)
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("PandaScore 赛事 {} 时间解析失败: {}", externalId, beginAt);
            return null;
        }

        // 状态映射：not_started→0, running→1, finished→2
        int status = mapStatus(node.path("status").asText(""));

        // 比分与结果：仅 finished 状态提取
        Integer scoreA = null;
        Integer scoreB = null;
        Integer result = null;
        if (status == 2) {
            JsonNode results = node.path("results");
            if (results.isArray() && results.size() >= 2) {
                scoreA = results.get(0).path("score").asInt(0);
                scoreB = results.get(1).path("score").asInt(0);
            }
            // winner_id → 结算判定：与 opponents[*].opponent.id 比较
            long winnerId = node.path("winner_id").asLong(0);
            long team0Id = team0.path("id").asLong(0);
            long team1Id = team1.path("id").asLong(0);
            if (winnerId != 0) {
                if (winnerId == team0Id) {
                    result = 0;  // A 胜
                } else if (winnerId == team1Id) {
                    result = 1;  // B 胜
                }
            }
        }

        RawMatch raw = new RawMatch();
        raw.setExternalId(externalId);
        raw.setGameId(gameId);
        raw.setTitle(title);
        raw.setTeamA(teamA);
        raw.setTeamB(teamB);
        raw.setTeamALogo(logoA);
        raw.setTeamBLogo(logoB);
        raw.setTournament(tournament);
        raw.setMatchTime(matchTime);
        raw.setScoreA(scoreA);
        raw.setScoreB(scoreB);
        raw.setStatus(status);
        raw.setResult(result);
        return raw;
    }

    /**
     * PandaScore status 字符串 → egg_match status 数值
     */
    private int mapStatus(String psStatus) {
        switch (psStatus) {
            case "not_started":
                return 0;
            case "running":
                return 1;
            case "finished":
                return 2;
            default:
                return 0;
        }
    }

    /**
     * 强制将 http:// 替换为 https://，防止浏览器 Mixed Content 警告
     * <p>null/空统一返回 null（前端用首字母 placeholder 替代）
     */
    private String toHttps(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }

    /** 线程睡眠工具，处理中断 */
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("PandaScoreAdapter 同步被中断");
        }
    }
}
