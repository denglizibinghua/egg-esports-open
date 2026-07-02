package com.egg.esports.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.egg.esports.sync.GameSourceAdapter;
import com.egg.esports.sync.GridAdapter;
import com.egg.esports.sync.LiquipediaAdapter;
import com.egg.esports.sync.OpenDotaAdapter;
import com.egg.esports.sync.PandaScoreAdapter;
import com.egg.esports.sync.RiotAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 同步 Adapter Bean 配置（上线版 v29）
 * <p>上线游戏：瓦/LOL/Dota 2/CS2
 * <ul>
 *   <li>riotValorantAdapter — 无畏契约（gameId=1，Riot 官方 API）</li>
 *   <li>riotLolAdapter — 英雄联盟（gameId=2，Riot 官方 API）</li>
 *   <li>openDotaAdapter — Dota 2（gameId=3，OpenDota API）</li>
 *   <li>gridCs2Adapter — CS2（gameId=4，GRID Open Access）</li>
 * </ul>
 * <p>以下 adapter 已禁用（yml enabled=false），Bean 不创建：
 * <ul>
 *   <li>pandaScoreCs2Adapter — CS2 PandaScore（API 被墙）</li>
 *   <li>gridDota2Adapter — Dota 2 GRID（已被 OpenDota 替代）</li>
 *   <li>liquipediaHokAdapter — 王者荣耀（服务器不可达）</li>
 *   <li>liquipediaIdvAdapter — 第五人格（无 stable 数据源）</li>
 * </ul>
 */
@Slf4j
@Configuration
public class SyncAdapterConfig {

    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.riot-valorant", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GameSourceAdapter riotValorantAdapter(
            @Value("${egg.sync.adapters.riot-valorant.game-id:1}") Long gameId,
            @Value("${egg.sync.adapters.riot-valorant.base-url:https://esports-api.service.valorantesports.com/persisted/val/getSchedule}") String baseUrl,
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

    // ========== GRID: CS2 (gameId=4, titleId=28) ==========
    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.grid-cs2", name = "enabled", havingValue = "true", matchIfMissing = true)
    public GameSourceAdapter gridCs2Adapter(
            @Value("${egg.sync.adapters.grid-cs2.game-id:4}") Long gameId,
            @Value("${egg.sync.adapters.grid-cs2.title-id:28}") String titleId,
            @Value("${egg.sync.adapters.grid-cs2.api-key:${egg.grid.api-key}}") String apiKey,
            @Value("${egg.grid.central-data-url}") String centralDataUrl,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 GridCs2Adapter: gameId={}, titleId={}", gameId, titleId);
        return new GridAdapter(gameId, titleId, apiKey, centralDataUrl, restTemplate, objectMapper);
    }

    // ========== GRID: Dota 2 (gameId=3, titleId=2) ==========
    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.grid-dota2", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter gridDota2Adapter(
            @Value("${egg.sync.adapters.grid-dota2.game-id:3}") Long gameId,
            @Value("${egg.sync.adapters.grid-dota2.title-id:2}") String titleId,
            @Value("${egg.sync.adapters.grid-dota2.api-key:${egg.grid.api-key}}") String apiKey,
            @Value("${egg.grid.central-data-url}") String centralDataUrl,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 GridDota2Adapter: gameId={}, titleId={}", gameId, titleId);
        return new GridAdapter(gameId, titleId, apiKey, centralDataUrl, restTemplate, objectMapper);
    }

    // ========== PandaScore: CS2 (gameId=4) ==========
    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.pandascore-cs2", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter pandaScoreCs2Adapter(
            @Value("${egg.sync.adapters.pandascore-cs2.game-id:4}") Long gameId,
            @Value("${egg.sync.adapters.pandascore-cs2.base-url:https://api.pandascore.com}") String baseUrl,
            @Value("${egg.sync.adapters.pandascore-cs2.api-key:}") String apiKey,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 PandaScoreCs2Adapter: gameId={}, baseUrl={}", gameId, baseUrl);
        return new PandaScoreAdapter(gameId, baseUrl, apiKey, restTemplate, objectMapper);
    }

    // ========== Liquipedia: 王者荣耀 (gameId=5) ==========
    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.liquipedia-hok", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter liquipediaHokAdapter(
            @Value("${egg.sync.adapters.liquipedia-hok.game-id:5}") Long gameId,
            @Value("${egg.sync.adapters.liquipedia-hok.game-name:honorofkings}") String gameName,
            @Value("${egg.sync.adapters.liquipedia-hok.base-url:https://liquipedia.net}") String baseUrl,
            @Value("${egg.sync.adapters.liquipedia-hok.tournaments:}") String tournaments,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 LiquipediaHokAdapter: gameId={}, tournaments={}", gameId, tournaments);
        return new LiquipediaAdapter(gameId, gameName, "王者荣耀", baseUrl, tournaments, restTemplate, objectMapper);
    }

    // ========== Liquipedia: 第五人格 (gameId=6) ==========
    @Bean
    @ConditionalOnProperty(prefix = "egg.sync.adapters.liquipedia-idv", name = "enabled", havingValue = "true", matchIfMissing = false)
    public GameSourceAdapter liquipediaIdvAdapter(
            @Value("${egg.sync.adapters.liquipedia-idv.game-id:6}") Long gameId,
            @Value("${egg.sync.adapters.liquipedia-idv.game-name:identityv}") String gameName,
            @Value("${egg.sync.adapters.liquipedia-idv.base-url:https://liquipedia.net}") String baseUrl,
            @Value("${egg.sync.adapters.liquipedia-idv.tournaments:}") String tournaments,
            RestTemplate restTemplate, ObjectMapper objectMapper) {
        log.info("注册 LiquipediaIdvAdapter: gameId={}, tournaments={}", gameId, tournaments);
        return new LiquipediaAdapter(gameId, gameName, "第五人格", baseUrl, tournaments, restTemplate, objectMapper);
    }
}
