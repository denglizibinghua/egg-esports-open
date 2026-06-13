package com.egg.esports.module.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Riot Valorant Esports API 客户端
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiotEsportsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${egg.riot.api-key}")
    private String apiKey;

    @Value("${egg.riot.base-url}")
    private String baseUrl;

    /**
     * 获取指定联赛的赛程事件
     *
     * @param leagueId 联赛ID
     * @return 事件列表（JsonNode），失败返回空列表
     */
    public List<JsonNode> fetchMatches(String leagueId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);

            String url = baseUrl + "?hl=en-US&sport=val&leagueId=" + leagueId;
            log.info("正在获取联赛 {} 的赛程数据: {}", leagueId, url);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode events = root.at("/data/schedule/events");

            if (events.isMissingNode() || !events.isArray()) {
                log.warn("联赛 {} 响应中未找到 events 数组", leagueId);
                return List.of();
            }

            List<JsonNode> result = new ArrayList<>();
            for (JsonNode event : events) {
                result.add(event);
            }

            log.info("联赛 {} 获取到 {} 个事件", leagueId, result.size());
            return result;

        } catch (Exception e) {
            log.error("获取联赛 {} 赛程数据失败: {}", leagueId, e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 获取所有联赛的赛程事件
     *
     * @return 所有联赛事件的合并列表
     */
    public List<JsonNode> fetchAllLeagues() {
        List<JsonNode> allEvents = new ArrayList<>();

        String[][] leagues = {
                {"111691194187846945", "VCT CN"},
                {"109940824119741550", "Masters"},
                {"107254585505459304", "Champions"},
                {"109974795266458277", "VCT Americas"},
                {"106109559530232966", "VCT EMEA"},
                {"109974804058058602", "VCT Pacific"}
        };

        for (String[] league : leagues) {
            String leagueId = league[0];
            String leagueName = league[1];
            log.info("开始同步联赛: {} ({})", leagueName, leagueId);

            List<JsonNode> events = fetchMatches(leagueId);
            allEvents.addAll(events);

            log.info("联赛 {} 同步完成，获取 {} 个事件", leagueName, events.size());
        }

        log.info("所有联赛同步完成，共获取 {} 个事件", allEvents.size());
        return allEvents;
    }
}