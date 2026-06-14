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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EggBetService {

    @Autowired
    private EggBetMapper eggBetMapper;

    @Autowired
    private EggMatchMapper eggMatchMapper;

    @Autowired
    private EggUserMapper eggUserMapper;

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

        // 3. 校验下注金额（仅需 > 0）
        if (betAmount == null || betAmount <= 0) {
            throw new BusinessException("下注金额必须大于0");
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
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));
                // 退还积分
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + bet.getBetAmount())
                );
            } else if (bet.getBetSide().equals(matchResult)) {
                // 猜中
                bet.setStatus(1);
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));
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
                if (user != null && user.getWinStreak() > user.getMaxWinStreak()) {
                    eggUserMapper.update(null,
                            new LambdaUpdateWrapper<EggUser>()
                                    .eq(EggUser::getId, bet.getUserId())
                                    .set(EggUser::getMaxWinStreak, user.getWinStreak())
                    );
                }
            } else {
                // 未中
                bet.setStatus(2);
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 2)
                                .setSql("update_time = NOW()"));
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

    /**
     * 近期战绩（最近10条含赛事信息）
     */
    public List<Map<String, Object>> getRecentBets(Long userId) {
        List<EggBet> bets = eggBetMapper.selectList(
                new LambdaQueryWrapper<EggBet>()
                        .eq(EggBet::getUserId, userId)
                        .orderByDesc(EggBet::getCreateTime)
                        .last("LIMIT 10")
        );

        return bets.stream().map(bet -> {
            EggMatch match = eggMatchMapper.selectById(bet.getMatchId());
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", bet.getId());
            item.put("matchId", bet.getMatchId());
            item.put("matchTitle", match != null ? match.getTitle() : "未知赛事");
            item.put("teamA", match != null ? match.getTeamA() : "A队");
            item.put("teamB", match != null ? match.getTeamB() : "B队");
            item.put("betSide", bet.getBetSide());
            item.put("betAmount", bet.getBetAmount());
            item.put("status", bet.getStatus());
            item.put("createTime", bet.getCreateTime());
            return item;
        }).toList();
    }

    /**
     * 查询我的下注记录（增强版，含赛事信息和盈亏计算）
     */
    public List<BetHistoryVO> getMyBetsEnhanced(Long userId, Integer status) {
        LambdaQueryWrapper<EggBet> wrapper = new LambdaQueryWrapper<EggBet>()
                .eq(EggBet::getUserId, userId);
        if (status != null) {
            wrapper.eq(EggBet::getStatus, status);
        }
        wrapper.orderByDesc(EggBet::getCreateTime);
        List<EggBet> bets = eggBetMapper.selectList(wrapper);

        List<BetHistoryVO> result = new ArrayList<>();
        for (EggBet bet : bets) {
            EggMatch match = eggMatchMapper.selectById(bet.getMatchId());
            BetHistoryVO vo = new BetHistoryVO();
            vo.setId(bet.getId());
            vo.setMatchId(bet.getMatchId());
            vo.setMatchTitle(match != null ? match.getTitle() : "未知赛事");
            vo.setTeamA(match != null ? match.getTeamA() : "A队");
            vo.setTeamB(match != null ? match.getTeamB() : "B队");
            vo.setBetSide(bet.getBetSide());
            vo.setBetAmount(bet.getBetAmount());
            vo.setStatus(bet.getStatus());
            vo.setCreateTime(bet.getCreateTime());

            // 计算返还金额和净变化
            int returnedAmount = calculateReturnedAmount(bet, match);
            vo.setReturnedAmount(returnedAmount);
            vo.setNetChange(returnedAmount - bet.getBetAmount());

            result.add(vo);
        }
        return result;
    }

    /**
     * 查询指定时间后已结算的下注记录
     *
     * @param userId 用户ID
     * @param since  起始时间（ISO格式）
     * @return 已结算的下注记录列表
     */
    public List<BetHistoryVO> getSettledSince(Long userId, LocalDateTime since) {
        List<EggBet> bets = eggBetMapper.selectSettledSince(userId, since);

        List<BetHistoryVO> result = new ArrayList<>();
        for (EggBet bet : bets) {
            EggMatch match = eggMatchMapper.selectById(bet.getMatchId());
            BetHistoryVO vo = new BetHistoryVO();
            vo.setId(bet.getId());
            vo.setMatchId(bet.getMatchId());
            vo.setMatchTitle(match != null ? match.getTitle() : "未知赛事");
            vo.setTeamA(match != null ? match.getTeamA() : "A队");
            vo.setTeamB(match != null ? match.getTeamB() : "B队");
            vo.setBetSide(bet.getBetSide());
            vo.setBetAmount(bet.getBetAmount());
            vo.setStatus(bet.getStatus());
            vo.setCreateTime(bet.getCreateTime());

            int returnedAmount = calculateReturnedAmount(bet, match);
            vo.setReturnedAmount(returnedAmount);
            vo.setNetChange(returnedAmount - bet.getBetAmount());

            result.add(vo);
        }
        return result;
    }

    /**
     * 计算返还金额
     * 猜中(status=1): betAmount * winMultiplier
     * 平局(status=1, betSide与result不匹配但result=2): betAmount（退还）
     * 未中(status=2): 0
     */
    private int calculateReturnedAmount(EggBet bet, EggMatch match) {
        if (bet.getStatus() == null || bet.getStatus() == 0) {
            return 0; // 待结算
        }
        if (bet.getStatus() == 2) {
            return 0; // 未中
        }
        // status == 1 (猜中或平局退还)
        if (match != null && match.getResult() != null && match.getResult() == 2) {
            // 平局：退还下注金额
            return bet.getBetAmount();
        }
        // 猜中：下注金额 * 倍数
        return bet.getBetAmount() * winMultiplier;
    }
}