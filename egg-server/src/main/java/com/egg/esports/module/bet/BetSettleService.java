package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.config.OddsConfig;
import com.egg.esports.module.loan.LoanService;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.notification.EggNotificationService;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 下注结算服务（拆分自 EggBetService）
 * <p>负责结算下注相关逻辑。
 */
@Service
@RequiredArgsConstructor
public class BetSettleService {

    private final EggBetMapper eggBetMapper;

    private final EggMatchMapper eggMatchMapper;

    private final EggUserMapper eggUserMapper;

    private final EggNotificationService notificationService;

    private final OddsConfig oddsConfig;

    private final LoanService loanService;

    /**
     * 结算下注（赛事结算时调用）
     */
    @Transactional
    @OperationLog(module = "押注", value = "结算")
    public void settleBet(Long matchId, Integer matchResult) {
        // 查找该赛事所有待结算的下注
        List<EggBet> pendingBets = eggBetMapper.selectList(
                new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getMatchId, matchId)
                        .eq(EggBet::getStatus, 0)
        );

        // 提前加载赛事信息和奖池（避免 N+1 查询）
        EggMatch match = eggMatchMapper.selectById(matchId);
        String matchTitle = match != null ? (match.getTeamA() + " vs " + match.getTeamB()) : "未知赛事";
        long poolA = match != null && match.getPoolA() != null ? match.getPoolA() : 0L;
        long poolB = match != null && match.getPoolB() != null ? match.getPoolB() : 0L;
        long totalPool = poolA + poolB;
        long winningPool = (matchResult == 0) ? poolA : poolB;
        if (winningPool == 0) winningPool = 1;
        double commission = oddsConfig.getCommissionRate().doubleValue();
        long netPool = (long)(totalPool * (1 - commission));

        for (EggBet bet : pendingBets) {
            if (matchResult == 2) {
                // 平局：退还所有下注
                bet.setStatus(1);
                // 乐观锁：status=0 才能结算，防止并发双重派彩
                int betUpdated = eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .eq(EggBet::getStatus, 0)
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));
                if (betUpdated == 0) continue;  // 已被并发结算，跳过
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + bet.getBetAmount())
                );
                notificationService.create(bet.getUserId(), 2, "押注结算",
                        matchTitle + "：平局，积分已退还", matchId);
            } else if (bet.getBetSide().equals(matchResult)) {
                // 猜中 — 使用最终奖池派彩
                bet.setStatus(1);
                // 乐观锁：status=0 才能结算，防止并发双重派彩
                int betUpdated = eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .eq(EggBet::getStatus, 0)
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));
                if (betUpdated == 0) continue;  // 已被并发结算，跳过

                // 用 long 计算防止溢出，并校验上限避免强转 int 时截断为负数
                long winAmountLong = (long)bet.getBetAmount() * netPool / winningPool;
                if (winAmountLong > Integer.MAX_VALUE) {
                    throw new BusinessException("单场派彩金额超过上限，请联系管理员");
                }
                int winAmount = (int) winAmountLong;

                // 自动还贷：50%利润用于还贷
                int profit = winAmount - bet.getBetAmount();
                int actualRepay = 0;
                if (profit > 0) {
                    int[] repayResult = loanService.autoRepay(bet.getUserId(), profit);
                    actualRepay = profit - repayResult[0];
                }

                int netPoints = winAmount - actualRepay;
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + netPoints)
                                .setSql("correct_bets = correct_bets + 1")
                                .setSql("win_streak = win_streak + 1")
                                // BUG #6 修复：累加净收益（赢分-输分，不扣还贷，因为还贷只是积分转债务，净资产不变）
                                .setSql("net_profit = net_profit + " + profit)
                );
                // 更新最大连胜
                EggUser user = eggUserMapper.selectById(bet.getUserId());
                if (user != null && user.getWinStreak() > user.getMaxWinStreak()) {
                    eggUserMapper.update(null,
                            new LambdaUpdateWrapper<EggUser>()
                                    .eq(EggUser::getId, bet.getUserId())
                                    .set(EggUser::getMaxWinStreak, user.getWinStreak())
                    );
                }
                notificationService.create(bet.getUserId(), 2, "押注结算",
                        matchTitle + "：恭喜猜中！获得 " + netPoints + " 积分", matchId);
            } else {
                // 未中
                bet.setStatus(2);
                // 乐观锁：status=0 才能结算，防止并发双重派彩
                int betUpdated = eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .eq(EggBet::getStatus, 0)
                                .set(EggBet::getStatus, 2)
                                .setSql("update_time = NOW()"));
                if (betUpdated == 0) continue;  // 已被并发结算，跳过
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .set(EggUser::getWinStreak, 0)
                                // BUG #6 修复：净收益扣减下注本金
                                .setSql("net_profit = net_profit - " + bet.getBetAmount())
                );
                notificationService.create(bet.getUserId(), 2, "押注结算",
                        matchTitle + "：很遗憾未猜中", matchId);
            }
        }
    }
}
