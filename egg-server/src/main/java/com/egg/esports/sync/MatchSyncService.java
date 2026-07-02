package com.egg.esports.sync;

import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 赛事数据同步服务
 * <p>遍历所有 GameSourceAdapter，拉取数据并 upsert 入库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchSyncService {

    private final List<GameSourceAdapter> adapters;
    private final EggMatchService eggMatchService;

    private static final LocalDateTime CUTOFF_DATE = LocalDateTime.of(2024, 1, 1, 0, 0, 0);

    /**
     * 同步所有 Adapter 的赛事数据
     *
     * @return 同步结果统计
     */
    public SyncResult syncAll() {
        SyncResult result = new SyncResult();
        log.info("开始同步所有数据源，共 {} 个 adapter", adapters.size());

        for (GameSourceAdapter adapter : adapters) {
            Long gameId = adapter.getGameId();
            String adapterName = adapter.getClass().getSimpleName();
            log.info("=== 开始同步 adapter: {} (gameId={}) ===", adapterName, gameId);

            try {
                List<RawMatch> rawMatches = adapter.fetchMatches();
                result.setTotalFetched(result.getTotalFetched() + rawMatches.size());

                int created = 0;
                int updated = 0;
                int skipped = 0;

                for (RawMatch raw : rawMatches) {
                    try {
                        // 过滤早于 CUTOFF_DATE 的赛事
                        if (raw.getMatchTime() == null || raw.getMatchTime().isBefore(CUTOFF_DATE)) {
                            // 排查用：记录被 CUTOFF_DATE 过滤掉的赛事（debug 级别，生产关闭）
                            log.debug("跳过赛事: externalId={}, matchTime={}", raw.getExternalId(), raw.getMatchTime());
                            skipped++;
                            continue;
                        }

                        EggMatch match = toEggMatch(raw);
                        EggMatch upserted = eggMatchService.upsertByExternalId(match);

                        // 判断是新增还是更新
                        if (upserted.getCreateTime() != null
                                && upserted.getCreateTime().isAfter(LocalDateTime.now().minusMinutes(1))) {
                            created++;
                        } else {
                            updated++;
                        }
                    } catch (Exception e) {
                        log.error("处理赛事失败: externalId={}, error={}", raw.getExternalId(), e.getMessage(), e);
                        result.getErrors().add(e.getMessage());
                    }
                }

                result.setTotalCreated(result.getTotalCreated() + created);
                result.setTotalUpdated(result.getTotalUpdated() + updated);
                result.setTotalSkipped(result.getTotalSkipped() + skipped);
                result.getPerGameStats().put("game-" + gameId, new int[]{created, updated, skipped});

                log.info("=== adapter {} 完成: 获取={}, 新增={}, 更新={}, 跳过={} ===",
                        adapterName, rawMatches.size(), created, updated, skipped);

            } catch (Exception e) {
                log.error("adapter {} 同步失败: {}", adapterName, e.getMessage(), e);
                result.getErrors().add(adapterName + ": " + e.getMessage());
            }
        }

        result.setSyncTime(LocalDateTime.now());
        log.info("全部同步完成: 获取={}, 新增={}, 更新={}, 跳过={}, 错误={}",
                result.getTotalFetched(), result.getTotalCreated(),
                result.getTotalUpdated(), result.getTotalSkipped(),
                result.getErrors().size());

        // 错误逐条输出，便于排查（errors 非空时才打；warn 级别，异常本不应静默）
        if (!result.getErrors().isEmpty()) {
            log.warn("同步过程发生 {} 个错误:", result.getErrors().size());
            result.getErrors().forEach(err -> log.warn("  - {}", err));
        }

        return result;
    }

    /**
     * RawMatch → EggMatch
     */
    private EggMatch toEggMatch(RawMatch raw) {
        EggMatch match = new EggMatch();
        match.setExternalId(raw.getExternalId());
        match.setGameId(raw.getGameId());
        match.setTitle(raw.getTitle());
        match.setTeamA(raw.getTeamA());
        match.setTeamB(raw.getTeamB());
        match.setTeamALogo(raw.getTeamALogo());
        match.setTeamBLogo(raw.getTeamBLogo());
        match.setTournament(raw.getTournament());
        match.setMatchTime(raw.getMatchTime());
        match.setScoreA(raw.getScoreA());
        match.setScoreB(raw.getScoreB());
        match.setStatus(raw.getStatus());
        match.setResult(raw.getResult());
        return match;
    }
}
