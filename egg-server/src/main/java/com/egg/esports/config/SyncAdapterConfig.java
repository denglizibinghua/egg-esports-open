package com.egg.esports.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.egg.esports.sync.GameSourceAdapter;
import com.egg.esports.sync.OpenDotaAdapter;
import com.egg.esports.sync.RiotAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 同步 Adapter Bean 配置
 * <p>根据 yml 配置创建多个 GameSourceAdapter 实例：
 * <ul>
 *   <li>riotValorantAdapter — 无畏契约（gameId=1）</li>
 *   <li>riotLolAdapter — 英雄联盟（gameId=2）</li>
 *   <li>openDotaAdapter — Dota 2（gameId=3）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SyncAdapterConfig {

    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.riot-valorant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GameSourceAdapter riotValorantAdapter(
            @Value("${egg.sync.adapters.riot-valorant.game-id:1}") Long gameId,
            @Value("${egg.sync.adapters.riot-valorant.base-url:${egg.riot.base-url}}") String baseUrl,
            @Value("${egg.sync.adapters.riot-valorant.sport:val}") String sport,
            @Value("${egg.riot.api-key}") String apiKey,
            @Value("${egg.sync.adapters.riot-valorant.league-ids:111691194187846945,109940824119741550,107254585505459304,109974795266458277,106109559530232966,109974804058058602}") String leagueIds,
            @Value("${egg.sync.adapters.riot-valorant.league-names:VCT CN,Masters,Champions,VCT Americas,VCT EMEA,VCT Pacific}") String leagueNames,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 RiotValorantAdapter: gameId={}, leagues={}", gameId, leagueIds);
        return new RiotAdapter(gameId, baseUrl, sport, apiKey, leagueIds, leagueNames, restTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.riot-lol", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter riotLolAdapter(
            @Value("${egg.sync.adapters.riot-lol.game-id:2}") Long gameId,
            @Value("${egg.sync.adapters.riot-lol.base-url:https://esports-api.service.lolesports.com/persisted/lol/getSchedule}") String baseUrl,
            @Value("${egg.sync.adapters.riot-lol.sport:lol}") String sport,
            @Value("${egg.riot.api-key}") String apiKey,
            @Value("${egg.sync.adapters.riot-lol.league-ids:}") String leagueIds,
            @Value("${egg.sync.adapters.riot-lol.league-names:}") String leagueNames,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 RiotLolAdapter: gameId={}, leagues={}", gameId, leagueIds);
        return new RiotAdapter(gameId, baseUrl, sport, apiKey, leagueIds, leagueNames, restTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.opendota", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter openDotaAdapter(
            @Value("${egg.sync.adapters.opendota.game-id:3}") Long gameId,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 OpenDotaAdapter: gameId={}", gameId);
        return new OpenDotaAdapter(gameId, restTemplate, objectMapper);
    }
}
