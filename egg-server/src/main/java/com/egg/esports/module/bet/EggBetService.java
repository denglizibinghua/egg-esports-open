package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EggBetService {

    @Autowired
    private EggBetMapper eggBetMapper;

    @Autowired
    private EggMatchMapper eggMatchMapper;

    @Autowired
    private EggUserMapper eggUserMapper;

    @Value("${egg.bet.min-amount}")
    private int minAmount;

    @Value("${egg.bet.max-amount}")
    private int maxAmount;

    @Value("${egg.bet.win-multiplier}")
    private int winMultiplier;

    /**
     * 下注
     */
    @Transactional
    public EggBet placeBet(Long userId, Long matchId, Integer betSide, Integer betAmount) {
        // 1. 校验赛事存在且未结束
        EggMatch match = eggMatchMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }
        if (match.getStatus() != 0 && match.getStatus() != 1) {
            throw new BusinessException("赛事已结束，无法下注");
        }
        if (match.getResult() != null) {
            throw new BusinessException("赛事已结算，无法下注");
        }

        // 2. 校验用户未重复下注
        Long existCount = eggBetMapper.selectCount(
                new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getUserId, userId)
                        .eq(EggBet::getMatchId, matchId)
        );
        if (existCount > 0) {
            throw new BusinessException("您已对该赛事下注");
        }

        // 3. 校验下注金额
        if (betAmount < minAmount || betAmount > maxAmount) {
            throw new BusinessException("下注金额需在" + minAmount + "~" + maxAmount + "之间");
        }

        // 4. 扣除用户积分（乐观锁方式：只有积分 >= betAmount 时才更新）
        int updated = eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, userId)
                        .ge(EggUser::getPoints, betAmount)
                        .setSql("points = points - " + betAmount)
        );
        if (updated == 0) {
            throw new BusinessException("积分不足");
        }

        // 5. 创建下注记录
        EggBet bet = new EggBet();
        bet.setUserId(userId);
        bet.setMatchId(matchId);
        bet.setBetSide(betSide);
        bet.setBetAmount(betAmount);
        bet.setStatus(0); // 待结算
        bet.setCreateTime(LocalDateTime.now());
        eggBetMapper.insert(bet);

        // 6. 增加用户总下注次数
        eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, userId)
                        .setSql("total_bets = total_bets + 1")
        );

        return bet;
    }

    /**
     * 查询我的下注记录
     */
    public List<EggBet> getMyBets(Long userId, Integer status) {
        LambdaQueryWrapper<EggBet> wrapper = new LambdaQueryWrapper<EggBet>()
                .eq(EggBet::getUserId, userId);
        if (status != null) {
            wrapper.eq(EggBet::getStatus, status);
        }
        wrapper.orderByDesc(EggBet::getCreateTime);
        return eggBetMapper.selectList(wrapper);
    }

    /**
     * 结算下注（赛事结算时调用）
     */
    @Transactional
    public void settleBet(Long matchId, Integer matchResult) {
        // 查找该赛事所有待结算的下注
        List<EggBet> pendingBets = eggBetMapper.selectList(
                new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getMatchId, matchId)
                        .eq(EggBet::getStatus, 0)
        );

        for (EggBet bet : pendingBets) {
            if (matchResult == 2) {
                // 平局：退还所有下注
                bet.setStatus(1); // 标记为猜中（退还）
                eggBetMapper.updateById(bet);
                // 退还积分
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + bet.getBetAmount())
                );
            } else if (bet.getBetSide().equals(matchResult)) {
                // 猜中
                bet.setStatus(1);
                eggBetMapper.updateById(bet);
                // 返还积分：下注金额 * 倍数
                int winAmount = bet.getBetAmount() * winMultiplier;
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + winAmount)
                                .setSql("correct_bets = correct_bets + 1")
                                .setSql("win_streak = win_streak + 1")
                );
                // 更新最大连胜
                EggUser user = eggUserMapper.selectById(bet.getUserId());
                if (user != null && user.getWinStreak() + 1 > user.getMaxWinStreak()) {
                    eggUserMapper.update(null,
                            new LambdaUpdateWrapper<EggUser>()
                                    .eq(EggUser::getId, bet.getUserId())
                                    .set(EggUser::getMaxWinStreak, user.getWinStreak() + 1)
                    );
                }
            } else {
                // 未中
                bet.setStatus(2);
                eggBetMapper.updateById(bet);
                // 重置连胜
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .set(EggUser::getWinStreak, 0)
                );
            }
        }
    }

    /**
     * 获取赛事下注数
     */
    public Long getBetCountByMatch(Long matchId) {
        return eggBetMapper.selectCount(
                new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getMatchId, matchId)
        );
    }
}