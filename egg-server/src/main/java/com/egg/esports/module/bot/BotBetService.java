package com.egg.esports.module.bot;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.module.bet.BetPlaceService;
import com.egg.esports.module.bet.EggBet;
import com.egg.esports.module.bet.EggBetMapper;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.user.EggUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 机器人下注服务：反向补池策略，走真实下注路径
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BotBetService {

    private final EggMatchMapper eggMatchMapper;
    private final EggBetMapper eggBetMapper;
    private final BetPlaceService betPlaceService;
    private final BotUserService botUserService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${egg.bot.per-match-limit:500}")
    private int perMatchLimit;

    @Value("${egg.bot.daily-limit:80}")
    private int dailyLimit;

    /**
     * 为单场赛事执行机器人托底下注
     * 策略：按池差反向补，poolA<poolB则65%押A否则65%押B，每笔20-120积分，分3-8笔
     */
    public int placeBotBetsForMatch(Long matchId) {
        EggMatch match = eggMatchMapper.selectById(matchId);
        if (match == null || match.getStatus() == null || match.getStatus() != 0) {
            return 0;
        }

        // 风控：单场机器人总额 ≤ perMatchLimit
        long botSumForMatch = getBotSumForMatch(matchId);
        if (botSumForMatch >= perMatchLimit) {
            log.info("Bot skip match {}: per-match bot sum {} >= limit {}", matchId, botSumForMatch, perMatchLimit);
            return 0;
        }

        // 风控：每日总笔数 ≤ dailyLimit
        long todayBotCount = getTodayBotBetCount();
        if (todayBotCount >= dailyLimit) {
            log.info("Bot skip match {}: daily bot bets {} >= limit {}", matchId, todayBotCount, dailyLimit);
            return 0;
        }

        List<EggUser> bots = botUserService.getBotUsers();
        if (bots.isEmpty()) {
            botUserService.ensureBotUsers();
            bots = botUserService.getBotUsers();
        }
        if (bots.isEmpty()) {
            log.warn("No bot users available");
            return 0;
        }

        // 筛选未对该场下注过的机器人（单机器人单场 ≤1 笔）
        List<EggUser> availableBots = new ArrayList<>();
        for (EggUser bot : bots) {
            Long cnt = eggBetMapper.selectCount(
                    new LambdaQueryWrapper<EggBet>()
                            .eq(EggBet::getUserId, bot.getId())
                            .eq(EggBet::getMatchId, matchId));
            if (cnt == 0) {
                availableBots.add(bot);
            }
        }
        if (availableBots.isEmpty()) {
            log.info("Bot skip match {}: no available bots (all already bet)", matchId);
            return 0;
        }
        Collections.shuffle(availableBots);

        long poolA = match.getPoolA() != null ? match.getPoolA() : 0L;
        long poolB = match.getPoolB() != null ? match.getPoolB() : 0L;

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int betNum = rnd.nextInt(3, 9); // 3-8
        betNum = Math.min(betNum, availableBots.size());

        // 考虑每日限额剩余
        int dailyRemaining = (int) (dailyLimit - todayBotCount);
        betNum = Math.min(betNum, dailyRemaining);
        if (betNum <= 0) return 0;

        int placed = 0;
        long currentBotSum = botSumForMatch;
        for (int i = 0; i < betNum; i++) {
            // 风控：检查单场总额
            if (currentBotSum >= perMatchLimit) break;

            int amount = rnd.nextInt(20, 121); // 20-120
            // 若加上这笔会超限，则调整金额
            if (currentBotSum + amount > perMatchLimit) {
                amount = (int) (perMatchLimit - currentBotSum);
                if (amount < 20) break;
            }
            // 每日限额检查
            if (todayBotCount + placed >= dailyLimit) break;

            // 反向补池： poolA < poolB 则 65%押A，否则65%押B
            boolean favorA = poolA < poolB;
            int side;
            double r = rnd.nextDouble();
            if (favorA) {
                side = r < 0.65 ? 0 : 1;
            } else {
                side = r < 0.65 ? 1 : 0;
            }

            EggUser bot = availableBots.get(i);
            try {
                betPlaceService.placeBet(bot.getId(), matchId, side, amount);
                placed++;
                currentBotSum += amount;
                // 更新 pool 值用于下一笔的判断（近似，不要求强一致）
                if (side == 0) poolA += amount; else poolB += amount;
            } catch (Exception e) {
                log.warn("Bot bet failed: botId={}, matchId={}, side={}, amount={}, err={}",
                        bot.getId(), matchId, side, amount, e.getMessage());
            }
        }
        if (placed > 0) {
            log.info("Bot placed {} bets for match {} (poolA={}, poolB={})", placed, matchId, poolA, poolB);
        }
        return placed;
    }

    private long getBotSumForMatch(Long matchId) {
        try {
            Long sum = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(b.bet_amount),0) FROM egg_bet b JOIN egg_user u ON b.user_id=u.id WHERE b.match_id=? AND u.username LIKE 'bot\\_%' ESCAPE '\\'",
                    Long.class, matchId);
            return sum != null ? sum : 0L;
        } catch (Exception e) {
            // 兜底：用 likeRight 方式（H2 MySQL模式 ESCAPE 兼容）
            try {
                Long sum2 = jdbcTemplate.queryForObject(
                        "SELECT COALESCE(SUM(b.bet_amount),0) FROM egg_bet b JOIN egg_user u ON b.user_id=u.id WHERE b.match_id=? AND u.username LIKE 'bot_%'",
                        Long.class, matchId);
                return sum2 != null ? sum2 : 0L;
            } catch (Exception ex) {
                return 0L;
            }
        }
    }

    private long getTodayBotBetCount() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        try {
            Long cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM egg_bet b JOIN egg_user u ON b.user_id=u.id WHERE b.create_time >= ? AND u.username LIKE 'bot\\_%' ESCAPE '\\'",
                    Long.class, startOfDay);
            return cnt != null ? cnt : 0L;
        } catch (Exception e) {
            try {
                Long cnt2 = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM egg_bet b JOIN egg_user u ON b.user_id=u.id WHERE b.create_time >= ? AND u.username LIKE 'bot_%'",
                        Long.class, startOfDay);
                return cnt2 != null ? cnt2 : 0L;
            } catch (Exception ex) {
                return 0L;
            }
        }
    }
}
