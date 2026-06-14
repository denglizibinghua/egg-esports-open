package com.egg.esports.module.sync;

import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 赛事数据同步服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {

    private final RiotEsportsClient riotEsportsClient;
    private final EggMatchService eggMatchService;

    private static final LocalDateTime CUTOFF_DATE = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

    /**
     * 从Riot API同步赛事数据
     *
     * @return 同步结果统计
     */
    public SyncResult syncFromRiot() {
        SyncResult result = new SyncResult();
        log.info("开始从Riot API同步赛事数据...");

        List<com.fasterxml.jackson.databind.JsonNode> events = riotEsportsClient.fetchAllLeagues();
        result.setTotalFetched(events.size());

        for (com.fasterxml.jackson.databind.JsonNode event : events) {
            try {
                // 解析startTime，过滤早于2026-01-01的赛事
                String startTimeStr = event.path("startTime").asText("");
                if (startTimeStr.isEmpty()) {
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                LocalDateTime matchTime = OffsetDateTime.parse(startTimeStr)
                        .atZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"))
                        .toLocalDateTime();
                if (matchTime.isBefore(CUTOFF_DATE)) {
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                // 映射为EggMatch
                EggMatch match = mapToMatch(event);
                if (match == null) {
                    result.setTotalSkipped(result.getTotalSkipped() + 1);
                    continue;
                }

                // 执行upsert
                EggMatch upserted = eggMatchService.upsertByExternalId(match);

                // 根据联赛slug区分CN/INTL
                String leagueSlug = event.at("/league/slug").asText("");
                boolean isCn = "vct_china".equals(leagueSlug);

                if (upserted.getCreateTime() != null && upserted.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(1))) {
                    // 刚创建的记录（createTime在1分钟内）
                    if (isCn) {
                        result.setCnCreated(result.getCnCreated() + 1);
                    } else {
                        result.setIntlCreated(result.getIntlCreated() + 1);
                    }
                } else {
                    // 已存在的记录被更新
                    if (isCn) {
                        result.setCnUpdated(result.getCnUpdated() + 1);
                    } else {
                        result.setIntlUpdated(result.getIntlUpdated() + 1);
                    }
                }

            } catch (Exception e) {
                log.error("处理赛事事件失败: {}", e.getMessage(), e);
                result.getErrors().add(e.getMessage());
            }
        }

        result.setSyncTime(LocalDateTime.now());
        log.info("同步完成: 获取={}, 跳过={}, CN新增={}, CN更新={}, INTL新增={}, INTL更新={}, 错误={}",
                result.getTotalFetched(), result.getTotalSkipped(),
                result.getCnCreated(), result.getCnUpdated(),
                result.getIntlCreated(), result.getIntlUpdated(),
                result.getErrors().size());

        return result;
    }

    /**
     * 将Riot API事件映射为EggMatch
     */
    private EggMatch mapToMatch(com.fasterxml.jackson.databind.JsonNode event) {
        try {
            com.fasterxml.jackson.databind.JsonNode matchNode = event.path("match");
            if (matchNode.isMissingNode() || matchNode.isNull()) {
                log.warn("事件缺少match节点，跳过");
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode teams = matchNode.path("teams");
            if (!teams.isArray() || teams.size() < 2) {
                log.warn("事件match.teams不足2个队伍，跳过");
                return null;
            }

            com.fasterxml.jackson.databind.JsonNode team0 = teams.get(0);
            com.fasterxml.jackson.databind.JsonNode team1 = teams.get(1);

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
            // 美化tournament名称
            if (!seasonName.isEmpty()) {
                tournament = seasonName.replace("valorant_champions_tour_", "VCT ").replace("_", " ");
                if (!splitName.isEmpty()) {
                    tournament = tournament + " " + splitName;
                }
            }

            // matchTime
            String startTimeStr = event.path("startTime").asText("");
            LocalDateTime matchTime = OffsetDateTime.parse(startTimeStr)
                    .atZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"))
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
                com.fasterxml.jackson.databind.JsonNode result0 = team0.path("result");
                com.fasterxml.jackson.databind.JsonNode result1 = team1.path("result");
                if (!result0.isMissingNode() && !result1.isMissingNode()) {
                    String outcome0 = result0.path("outcome").asText("");
                    String outcome1 = result1.path("outcome").asText("");
                    if ("win".equals(outcome0)) {
                        result = 0; // A队胜
                    } else if ("win".equals(outcome1)) {
                        result = 1; // B队胜
                    } else {
                        result = 2; // 平局
                    }
                }
                // 如果result节点缺失，result保持null（已完成但无结果数据）
            }

            // score (game wins in BO3/BO5 format)
            Integer scoreA = null;
            Integer scoreB = null;
            com.fasterxml.jackson.databind.JsonNode result0 = team0.path("result");
            com.fasterxml.jackson.databind.JsonNode result1 = team1.path("result");
            if (!result0.isMissingNode() && !result1.isMissingNode()) {
                if (result0.has("gameWins")) {
                    scoreA = result0.path("gameWins").asInt();
                }
                if (result1.has("gameWins")) {
                    scoreB = result1.path("gameWins").asInt();
                }
            }

            // team logos
            String teamALogo = team0.path("image").asText(null);
            String teamBLogo = team1.path("image").asText(null);

            // 构建EggMatch
            EggMatch match = new EggMatch();
            match.setExternalId(externalId);
            match.setGameId(1L); // 无畏契约
            match.setTitle(title);
            match.setTeamA(team0Code);
            match.setTeamB(team1Code);
            match.setTeamALogo(teamALogo);
            match.setTeamBLogo(teamBLogo);
            match.setTournament(tournament);
            match.setMatchTime(matchTime);
            match.setScoreA(scoreA);
            match.setScoreB(scoreB);
            match.setStatus(status);
            match.setResult(result);

            return match;

        } catch (Exception e) {
            log.error("映射赛事事件失败: {}", e.getMessage(), e);
            return null;
        }
    }
}