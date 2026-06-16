package com.egg.esports.module.sync;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.module.bet.EggBet;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.match.EggMatchService;
import com.egg.esports.module.notification.EggNotificationMapper;
import com.egg.esports.module.notification.EggNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
    private final EggMatchMapper matchMapper;
    private final EggBetMapper betMapper;
    private final EggNotificationService notificationService;
    private final EggNotificationMapper notificationMapper;

    @Value("${egg.sync.enabled:false}")
    private boolean enabled;

    /**
     * 每10分钟执行Riot API同步
     * 同步时会自动结算新获得结果的赛事（在 upsertByExternalId 中处理）
     */
    @Scheduled(cron = "0 */10 * * * ?")
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
     * <p>先触发一次轻量同步（Riot API 去重，无变化时几乎零开销），再执行结算。
     * 确保结算前数据尽可能新鲜。
     */
    @Scheduled(cron = "0 */2 * * * ?")
    public void autoSettleCheck() {
        if (!enabled) {
            return;
        }

        try {
            // 先同步一次，确保结算前 result 数据是最新的
            matchSyncService.syncFromRiot();
            // 再结算
            int settled = eggMatchService.autoSettlePending();
            if (settled > 0) {
                log.info("自动结算检查完成: 结算 {} 场赛事", settled);
            }
        } catch (Exception e) {
            log.error("自动结算检查失败", e);
        }
    }

    /**
     * 每分钟检查即将开始的赛事，向已下注用户发送提醒
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void sendMatchStartReminders() {
        if (!enabled) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime fiveMinLater = now.plusMinutes(5);
            List<EggMatch> upcomingMatches = matchMapper.selectList(
                new LambdaQueryWrapper<EggMatch>()
                    .eq(EggMatch::getStatus, 0)
                    .ge(EggMatch::getMatchTime, now)
                    .le(EggMatch::getMatchTime, fiveMinLater)
            );

            int totalSent = 0;
            for (EggMatch match : upcomingMatches) {
                List<EggBet> bets = betMapper.selectList(
                    new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getMatchId, match.getId())
                );
                for (EggBet bet : bets) {
                    // 去重：已存在同类型通知则跳过
                    int exists = notificationMapper.countByUserAndTypeAndRelated(bet.getUserId(), 1, match.getId());
                    if (exists > 0) {
                        continue;
                    }
                    notificationService.create(
                        bet.getUserId(), 1,
                        "比赛即将开始",
                        match.getTeamA() + " vs " + match.getTeamB() + " 将在5分钟后开始，押注即将截止",
                        match.getId()
                    );
                    totalSent++;
                }
            }

            if (!upcomingMatches.isEmpty()) {
                log.info("比赛开始提醒完成: 发送 {} 条通知", totalSent);
            }
        } catch (Exception e) {
            log.error("比赛开始提醒失败", e);
        }
    }
}