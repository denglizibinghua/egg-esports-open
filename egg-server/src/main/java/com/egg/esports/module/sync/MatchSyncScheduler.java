package com.egg.esports.module.sync;

import com.egg.esports.module.match.EggMatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 赛事数据定时同步调度器
 *
 * <p>包含三个定时任务：
 * <ol>
 *   <li>Riot API 数据同步（每天 10:00 / 22:00）</li>
 *   <li>自动结算检查（每 2 分钟）— 处理已结束且有结果但押注未结算的赛事</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSyncScheduler {

    private final MatchSyncService matchSyncService;
    private final EggMatchService eggMatchService;

    @Value("${egg.sync.enabled:false}")
    private boolean enabled;

    /**
     * 每天10:00和22:00执行Riot API同步
     * 同步时会自动结算新获得结果的赛事（在 upsertByExternalId 中处理）
     */
    @Scheduled(cron = "0 0 10,22 * * ?")
    public void scheduledSync() {
        if (!enabled) {
            log.debug("定时同步未启用 (egg.sync.enabled=false)");
            return;
        }

        log.info("开始执行定时赛事数据同步...");
        try {
            SyncResult result = matchSyncService.syncFromRiot();
            log.info("定时同步完成: 获取={}, 跳过={}, CN新增={}, CN更新={}, INTL新增={}, INTL更新={}, 错误={}",
                    result.getTotalFetched(), result.getTotalSkipped(),
                    result.getCnCreated(), result.getCnUpdated(),
                    result.getIntlCreated(), result.getIntlUpdated(),
                    result.getErrors().size());
        } catch (Exception e) {
            log.error("定时赛事数据同步失败", e);
        }
    }

    /**
     * 每2分钟检查一次：已结束且有结果但押注未结算的赛事
     *
     * <p>覆盖场景：
     * <ul>
     *   <li>Riot API 同步间隙期间，赛事状态/结果被更新但 bets 未结算</li>
     *   <li>同步时 upsertByExternalId 中的自动结算因异常未执行</li>
     *   <li>通过管理端手动更新了赛事结果但忘记结算的情况</li>
     * </ul>
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void autoSettleCheck() {
        if (!enabled) {
            return;
        }

        try {
            int settled = eggMatchService.autoSettlePending();
            if (settled > 0) {
                log.info("自动结算检查完成: 结算 {} 场赛事", settled);
            }
        } catch (Exception e) {
            log.error("自动结算检查失败", e);
        }
    }
}