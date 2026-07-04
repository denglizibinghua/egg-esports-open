package com.egg.esports.sync;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenDota Adapter（Dota 2 职业赛事）
 * <p>免费 API，无需 key。GET https://api.opendota.com/api/proMatches
 * <p>注意：该接口返回的是已完成的比赛，status=2
 */
@Slf4j
public class OpenDotaAdapter implements GameSourceAdapter {

    private static final String OPENDOTA_URL = "https://api.opendota.com/api/proMatches";

    private final Long gameId;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenDotaAdapter(Long gameId, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.gameId = gameId;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Long getGameId() {
        return gameId;
    }

    @Override
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public List<RawMatch> fetchMatches() {
        List<RawMatch> result = new ArrayList<>();
        log.info("正在从 OpenDota 获取 Dota2 职业赛事数据: {}", OPENDOTA_URL);
        // restTemplate.getForEntity 抛出 RestClientException（运行时异常），直接向上传播触发 @Retryable 重试
        ResponseEntity<String> response = restTemplate.getForEntity(OPENDOTA_URL, String.class);

        // JsonProcessingException 为检查异常，包装为 RuntimeException 向上抛出
        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            log.error("解析 OpenDota 响应 JSON 失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析 OpenDota 响应 JSON 失败", e);
        }

        if (!root.isArray()) {
            log.warn("OpenDota 响应不是数组");
            return result;
        }

        for (JsonNode node : root) {
            try {
                RawMatch raw = mapToRawMatch(node);
                if (raw != null) {
                    result.add(raw);
                }
            } catch (Exception e) {
                // 单条数据映射异常为业务异常，不影响整体同步
                log.error("映射 OpenDota 赛事失败: {}", e.getMessage(), e);
            }
        }

        log.info("OpenDota 同步完成，共 {} 场赛事", result.size());
        return result;
    }

    /**
     * 将 OpenDota proMatches 元素映射为 RawMatch
     * <p>部分近期比赛只有 team_id 没有 team_name，使用 team_id 兜底
     */
    private RawMatch mapToRawMatch(JsonNode node) {
        String externalId = node.path("match_id").asText("");
        if (externalId.isEmpty()) {
            return null;
        }

        String radiantName = node.path("radiant_name").asText("");
        String direName = node.path("dire_name").asText("");

        // 兜底：用 team_id 作为队名
        if (radiantName.isEmpty()) {
            var radiantTeamId = node.path("radiant_team_id").asText("");
            if (!radiantTeamId.isEmpty()) {
                radiantName = "Team " + radiantTeamId;
            }
        }
        if (direName.isEmpty()) {
            var direTeamId = node.path("dire_team_id").asText("");
            if (!direTeamId.isEmpty()) {
                direName = "Team " + direTeamId;
            }
        }

        // 两个名字都拿不到才跳过
        if (radiantName.isEmpty() || direName.isEmpty()) {
            log.warn("OpenDota 赛事 {}: radiant_name 和 dire_name 均为空，跳过", externalId);
            return null;
        }

        String leagueName = node.path("league_name").asText("");

        // title: [league_name] — radiant vs dire
        String title = (!leagueName.isEmpty() ? leagueName + " — " : "") + radiantName + " vs " + direName;

        // matchTime: start_time (Unix seconds) → Beijing time
        long startTime = node.path("start_time").asLong(0);
        LocalDateTime matchTime = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(startTime), ZoneId.of("Asia/Shanghai"));

        // status: OpenDota proMatches 都是已完成的比赛
        int status = 2;

        // result: radiant_win → 0=A(Radiant)胜, 1=B(Dire)胜
        boolean radiantWin = node.path("radiant_win").asBoolean(false);
        int result = radiantWin ? 0 : 1;

        // score
        Integer scoreA = node.has("radiant_score") ? node.path("radiant_score").asInt() : null;
        Integer scoreB = node.has("dire_score") ? node.path("dire_score").asInt() : null;

        RawMatch raw = new RawMatch();
        raw.setExternalId(externalId);
        raw.setGameId(gameId);
        raw.setTitle(title);
        raw.setTeamA(radiantName);
        raw.setTeamB(direName);
        raw.setTournament(leagueName);
        raw.setMatchTime(matchTime);
        raw.setScoreA(scoreA);
        raw.setScoreB(scoreB);
        raw.setStatus(status);
        raw.setResult(result);

        return raw;
    }
}
