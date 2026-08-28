package com.egg.esports.module.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 机器人定时调度：每7分钟扫 status=0 且 6h 内开赛且 (pool<600 或 betCount<6) 的场次，限5场/轮
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BotScheduler {

    private final EggMatchMapper eggMatchMapper;
    private final EggBetMapper eggBetMapper;
    private final BotBetService botBetService;
    private final BotUserService botUserService;

    @Value("${egg.bot.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "0 */7 * * * ?")
    public void scheduleBotBets() {
        if (!enabled) {
            log.debug("Bot scheduler disabled (egg.bot.enabled=false)");
            return;
        }
        try {
            // 确保机器人用户存在
            botUserService.ensureBotUsers();

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime end = now.plusHours(6);

            LambdaQueryWrapper<EggMatch> wrapper = new LambdaQueryWrapper<EggMatch>()
                    .eq(EggMatch::getStatus, 0)
                    .between(EggMatch::getMatchTime, now, end)
                    .orderByAsc(EggMatch::getMatchTime)
                    .last("LIMIT 20");

            List<EggMatch> candidates = eggMatchMapper.selectList(wrapper);
            List<EggMatch> toProcess = new ArrayList<>();
            for (EggMatch m : candidates) {
                long poolA = m.getPoolA() != null ? m.getPoolA() : 0L;
                long poolB = m.getPoolB() != null ? m.getPoolB() : 0L;
                long poolSum = poolA + poolB;
                Long betCount = eggBetMapper.selectCount(
                        new LambdaQueryWrapper<com.egg.esports.module.bet.EggBet>()
                                .eq(com.egg.esports.module.bet.EggBet::getMatchId, m.getId()));
                long bc = betCount != null ? betCount : 0L;
                if (poolSum < 600 || bc < 6) {
                    toProcess.add(m);
                }
                if (toProcess.size() >= 5) break;
            }

            if (toProcess.isEmpty()) {
                log.info("Bot scheduler: no matches need filling (candidates={})", candidates.size());
                return;
            }

            int totalPlaced = 0;
            int matchCount = 0;
            for (EggMatch m : toProcess) {
                int placed = botBetService.placeBotBetsForMatch(m.getId());
                totalPlaced += placed;
                if (placed > 0) matchCount++;
            }
            log.info("Bot placed {} bets in {} matches", totalPlaced, matchCount);
        } catch (Exception e) {
            log.error("Bot scheduler error", e);
        }
    }
}
