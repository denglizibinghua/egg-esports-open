package com.egg.esports.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Riot 电竞赛事 Adapter（瓦 + LoL 共用）
 * <p>通过 yml 配置不同的 gameId / baseUrl / sport / leagueIds 创建多个实例
 */
@Slf4j
public class RiotAdapter implements GameSourceAdapter {

    private final Long gameId;
    private final String baseUrl;
    private final String sport;
    private final String apiKey;
    private final List<String> leagueIds;
    private final List<String> leagueNames;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RiotAdapter(Long gameId, String baseUrl, String sport, String apiKey,
                       String leagueIdsCsv, String leagueNamesCsv,
                       RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.baseUrl = baseUrl;
        this.sport = sport;
        this.apiKey = apiKey;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;

        this.leagueIds = new ArrayList<>();
        this.leagueNames = new ArrayList<>();
        if (leagueIdsCsv != null && !leagueIdsCsv.trim().isEmpty()) {
            for (String id : leagueIdsCsv.split(",")) {
                String trimmed = id.trim();
                if (!trimmed.isEmpty()) this.leagueIds.add(trimmed);
            }
        }
        if (leagueNamesCsv != null && !leagueNamesCsv.trim().isEmpty()) {
            for (String name : leagueNamesCsv.split(",")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) this.leagueNames.add(trimmed);
            }
        }
    }

    @Override
    public Long getGameId() {
        return gameId;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<RawMatch> fetchMatches() {
        List<RawMatch> result = new ArrayList<>();
        // 循环内不再 catch 网络异常：单个联赛网络失败会向上抛出，触发 @Retryable 整体重试
        // 已成功的联赛在重试时会被重复拉取，这是可接受的代价（保证最终一致性）
        for (int i = 0; i < leagueIds.size(); i++) {
            String leagueId = leagueIds.get(i);
            String leagueName = i < leagueNames.size() ? leagueNames.get(i) : leagueId;
            log.info("开始同步联赛: {} ({})", leagueName, leagueId);
            List<JsonNode> events = fetchLeagueEvents(leagueId);
            for (JsonNode event : events) {
                try {
                    RawMatch raw = mapToRawMatch(event);
                    if (raw != null) {
                        result.add(raw);
                    }
                } catch (Exception e) {
                    // 单条数据映射异常为业务异常，不影响整体同步
                    log.error("映射赛事事件失败: {}", e.getMessage(), e);
                }
            }
            log.info("联赛 {} 同步完成，获取 {} 个事件", leagueName, events.size());
        }
        log.info("RiotAdapter(gameId={}) 同步完成，共 {} 个事件", gameId, result.size());
        return result;
    }

    /**
     * 获取指定联赛的赛程事件
     * <p>网络异常（RestClientException）直接向上抛出，触发 fetchMatches 的 @Retryable 重试
     * <p>JSON 解析异常（JsonProcessingException）包装为 RuntimeException 向上抛出
     */
    private List<JsonNode> fetchLeagueEvents(String leagueId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        String url = baseUrl + "?hl=en-US&sport=" + sport + "&leagueId=" + leagueId;
        log.info("正在获取联赛 {} 的赛程数据: {}", leagueId, url);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        // restTemplate.exchange 抛出 RestClientException（运行时异常），直接向上传播触发重试
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        // JsonProcessingException 为检查异常，包装为 RuntimeException 向上抛出
        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            log.error("解析联赛 {} 响应 JSON 失败: {}", leagueId, e.getMessage(), e);
            throw new RuntimeException("解析 Riot 联赛 " + leagueId + " 响应 JSON 失败", e);
        }

        JsonNode events = root.at("/data/schedule/events");

        if (events.isMissingNode() || !events.isArray()) {
            log.warn("联赛 {} 响应中未找到 events 数组", leagueId);
            return List.of();
        }

        List<JsonNode> result = new ArrayList<>();
        for (JsonNode event : events) {
            result.add(event);
        }
        return result;
    }

    /**
     * 将 Riot API 事件映射为 RawMatch（从原 MatchSyncService.mapToMatch 迁移）
     */
    private RawMatch mapToRawMatch(JsonNode event) {
        try {
            JsonNode matchNode = event.path("match");
            if (matchNode.isMissingNode() || matchNode.isNull()) {
                log.warn("事件缺少match节点，跳过");
                return null;
            }

            JsonNode teams = matchNode.path("teams");
            if (!teams.isArray() || teams.size() < 2) {
                log.warn("事件match.teams不足2个队伍，跳过");
                return null;
            }

            JsonNode team0 = teams.get(0);
            JsonNode team1 = teams.get(1);

            // externalId
            String externalId = matchNode.path("id").asText("");
            if (externalId.isEmpty()) {
                log.warn("事件match.id为空，跳过");
                return null;
            }

            // title: [league.name] [blockName] — [team1.code] vs [team2.code]
            String leagueName = event.at("/league/name").asText("");
            String blockName = event.path("blockName").asText("");
            String team0Code = team0.path("code").asText("");
            String team1Code = team1.path("code").asText("");

            String title;
            if (!leagueName.isEmpty() && !blockName.isEmpty()) {
                title = leagueName + " " + blockName + " — " + team0Code + " vs " + team1Code;
            } else if (!leagueName.isEmpty()) {
                title = leagueName + " — " + team0Code + " vs " + team1Code;
            } else {
                title = team0Code + " vs " + team1Code;
            }

            // tournament: [season.name] [split.name]
            String seasonName = event.at("/tournament/season/name").asText("");
            String splitName = event.at("/tournament/split/name").asText("");
            String tournament = (seasonName + " " + splitName).trim();
            if (!seasonName.isEmpty()) {
                tournament = seasonName.replace("valorant_champions_tour_", "VCT ").replace("_", " ");
                if (!splitName.isEmpty()) {
                    tournament = tournament + " " + splitName;
                }
            }

            // matchTime
            String startTimeStr = event.path("startTime").asText("");
            LocalDateTime matchTime = OffsetDateTime.parse(startTimeStr)
                    .atZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();

            // status
            String state = event.path("state").asText("");
            int status;
            switch (state) {
                case "completed":
                    status = 2;
                    break;
                case "inProgress":
                    status = 1;
                    break;
                default:
                    status = 0;
                    break;
            }

            // result
            Integer result = null;
            if ("completed".equals(state)) {
                JsonNode result0 = team0.path("result");
                JsonNode result1 = team1.path("result");
                if (!result0.isMissingNode() && !result1.isMissingNode()) {
                    String outcome0 = result0.path("outcome").asText("");
                    String outcome1 = result1.path("outcome").asText("");
                    if ("win".equals(outcome0)) {
                        result = 0;
                    } else if ("win".equals(outcome1)) {
                        result = 1;
                    } else {
                        result = 2;
                    }
                }
            }

            // score (game wins in BO3/BO5 format)
            Integer scoreA = null;
            Integer scoreB = null;
            JsonNode result0 = team0.path("result");
            JsonNode result1 = team1.path("result");
            if (!result0.isMissingNode() && !result1.isMissingNode()) {
                if (result0.has("gameWins")) {
                    scoreA = result0.path("gameWins").asInt();
                }
                if (result1.has("gameWins")) {
                    scoreB = result1.path("gameWins").asInt();
                }
            }

            // team logos - force HTTPS to avoid Mixed Content on https://eggesports.com
            String teamALogo = toHttps(team0.path("image").asText(null));
            String teamBLogo = toHttps(team1.path("image").asText(null));

            // 构建 RawMatch
            RawMatch raw = new RawMatch();
            raw.setExternalId(externalId);
            raw.setGameId(gameId);
            raw.setTitle(title);
            raw.setTeamA(team0Code);
            raw.setTeamB(team1Code);
            raw.setTeamALogo(teamALogo);
            raw.setTeamBLogo(teamBLogo);
            raw.setTournament(tournament);
            raw.setMatchTime(matchTime);
            raw.setScoreA(scoreA);
            raw.setScoreB(scoreB);
            raw.setStatus(status);
            raw.setResult(result);

            return raw;

        } catch (Exception e) {
            log.error("映射赛事事件失败: {}", e.getMessage(), e);
            return null;
        }
    }

    private String toHttps(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring(7);
        }
        return url;
    }
}
