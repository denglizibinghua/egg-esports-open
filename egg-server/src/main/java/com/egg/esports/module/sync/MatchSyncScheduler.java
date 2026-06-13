package com.egg.esports.module.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 赛事数据定时同步调度器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchSyncScheduler {

    private final MatchSyncService matchSyncService;

    @Value("${egg.sync.enabled:false}")
    private boolean enabled;

    /**
     * 每天10:00和22:00执行同步
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
}