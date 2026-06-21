package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.config.OddsConfig;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.notification.EggNotificationMapper;
import com.egg.esports.module.notification.EggNotificationService;
import com.egg.esports.module.odds.OddsService;
import com.egg.esports.module.loan.LoanService;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Autowired
    private EggNotificationService notificationService;

    @Autowired
    private EggNotificationMapper notificationMapper;

    @Autowired
    private OddsService oddsService;

    @Autowired
    private OddsConfig oddsConfig;

    @Autowired
    private LoanService loanService;

    @Value("${egg.bet.win-multiplier}")
    private int winMultiplier;

    /**
     * 下注
     */
    @Transactional
    @OperationLog(module = "押注", value = "下注")
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

        // 3.5 读取当前奖池值
        long poolA = match.getPoolA() != null ? match.getPoolA() : 0L;
        long poolB = match.getPoolB() != null ? match.getPoolB() : 0L;

        // 3.6 计算当前赔率
        Map<String, BigDecimal> currentOdds = oddsService.calculate(poolA, poolB);
        BigDecimal lockedOdds = (betSide == 0) ? currentOdds.get("oddsA") : currentOdds.get("oddsB");

        // 3.7 如果用户积分不足，尝试发放救济贷款
        EggUser user = eggUserMapper.selectById(userId);
        if (user.getPoints() < betAmount) {
            boolean gotLoan = loanService.tryReliefLoan(user);
            if (gotLoan) {
                // 重新读取用户积分
                user = eggUserMapper.selectById(userId);
                if (user.getPoints() < betAmount) {
                    throw new BusinessException("积分不足，已发放救济贷款但金额仍不够下注");
                }
            } else {
                // 检查原因：是否有活跃下注或已达贷款上限
                int activeBets = loanService.getActiveBetAmount(userId);
                if (activeBets > 0) {
                    throw new BusinessException("积分不足，您有 " + activeBets + " 积分押注中暂不可用");
                }
                throw new BusinessException("积分不足，已达贷款上限，请等待每日补贴");
            }
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

        // 5. 原子更新奖池（乐观锁）
        long deltaA = (betSide == 0) ? betAmount : 0L;
        long deltaB = (betSide == 1) ? betAmount : 0L;
        int poolUpdated = eggMatchMapper.updatePool(matchId, deltaA, deltaB, poolA, poolB);
        if (poolUpdated == 0) {
            throw new BusinessException("下注失败，奖池已变化，请重试");
        }

        // 6. 创建下注记录
        EggBet bet = new EggBet();
        bet.setUserId(userId);
        bet.setMatchId(matchId);
        bet.setBetSide(betSide);
        bet.setBetAmount(betAmount);
        bet.setStatus(0); // 待结算
        bet.setCreateTime(LocalDateTime.now());
        try {
            eggBetMapper.insert(bet);
        } catch (DuplicateKeyException e) {
            // 并发冲突：被 (user_id, match_id) 唯一约束挡下
            // 查已存在的记录返回（幂等），不抛异常
            log.warn("并发下注被唯一约束拦截: userId={}, matchId={}", userId, matchId);
            EggBet existing = eggBetMapper.selectOne(
                    new LambdaQueryWrapper<EggBet>()
                            .eq(EggBet::getUserId, userId)
                            .eq(EggBet::getMatchId, matchId)
            );
            return existing;
        }

        // 7. 增加用户总下注次数
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
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("points = points + " + bet.getBetAmount())
                                .setSql("win_streak = 0")
                );
                // net_profit: 平局不赚不亏，净收益变化为 0，无需更新
                notificationService.create(bet.getUserId(), 2, "押注结算",
                        matchTitle + "：平局，积分已退还", matchId);
            } else if (bet.getBetSide().equals(matchResult)) {
                // 猜中 — 使用最终奖池派彩
                bet.setStatus(1);
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 1)
                                .setSql("update_time = NOW()"));

                int winAmount = (int)((long)bet.getBetAmount() * netPool / winningPool);

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
                // net_profit: 赢分 - 本金 = 净赚
                int netProfitDelta = winAmount - bet.getBetAmount();
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("net_profit = net_profit + " + netProfitDelta)
                );
            } else {
                // 未中
                bet.setStatus(2);
                eggBetMapper.update(null,
                        new LambdaUpdateWrapper<EggBet>()
                                .eq(EggBet::getId, bet.getId())
                                .set(EggBet::getStatus, 2)
                                .setSql("update_time = NOW()"));
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .set(EggUser::getWinStreak, 0)
                );
                // net_profit: 输掉本金，净收益减少
                eggUserMapper.update(null,
                        new LambdaUpdateWrapper<EggUser>()
                                .eq(EggUser::getId, bet.getUserId())
                                .setSql("net_profit = net_profit - " + bet.getBetAmount())
                );
                notificationService.create(bet.getUserId(), 2, "押注结算",
                        matchTitle + "：很遗憾未猜中", matchId);
            }
        }

        // 标记赛事已结算，防止重复结算
        eggMatchMapper.update(null,
                new LambdaUpdateWrapper<EggMatch>()
                        .eq(EggMatch::getId, matchId)
                        .set(EggMatch::getSettledAt, LocalDateTime.now())
        );
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
            vo.setUpdateTime(bet.getUpdateTime());

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
            vo.setUpdateTime(bet.getUpdateTime());

            int returnedAmount = calculateReturnedAmount(bet, match);
            vo.setReturnedAmount(returnedAmount);
            vo.setNetChange(returnedAmount - bet.getBetAmount());

            result.add(vo);
        }
        return result;
    }

    /**
     * 修改下注
     */
    @Transactional
    public EggBet modifyBet(Long userId, Long betId, Integer newBetSide, Integer newBetAmount) {
        // 1. 加载下注记录
        EggBet bet = eggBetMapper.selectById(betId);
        if (bet == null) {
            throw new BusinessException("投注不存在");
        }

        // 2. 校验归属
        if (!bet.getUserId().equals(userId)) {
            throw new BusinessException("无权修改");
        }

        // 3. 校验状态
        if (bet.getStatus() != 0) {
            throw new BusinessException("投注已结算，无法修改");
        }

        // 4. 校验赛事未开始
        EggMatch match = eggMatchMapper.selectById(bet.getMatchId());
        if (match.getMatchTime() != null && LocalDateTime.now().isAfter(match.getMatchTime())) {
            throw new BusinessException("比赛已开始，无法修改");
        }

        // 4.5 读取当前奖池值
        long oldPoolA = match.getPoolA() != null ? match.getPoolA() : 0L;
        long oldPoolB = match.getPoolB() != null ? match.getPoolB() : 0L;

        // 4.6 原子更新奖池：先移除旧注单，再添加新注单（合并为一次原子操作）
        int oldBetSide = bet.getBetSide();
        int oldBetAmount = bet.getBetAmount();
        long netDeltaA = (newBetSide == 0 ? newBetAmount : 0L) - (oldBetSide == 0 ? oldBetAmount : 0L);
        long netDeltaB = (newBetSide == 1 ? newBetAmount : 0L) - (oldBetSide == 1 ? oldBetAmount : 0L);

        int poolUpdated = eggMatchMapper.updatePool(match.getId(), netDeltaA, netDeltaB, oldPoolA, oldPoolB);
        if (poolUpdated == 0) {
            throw new BusinessException("奖池已变化，请重试");
        }

        // 5. 计算积分差额
        int delta = newBetAmount - bet.getBetAmount();

        if (delta > 0) {
            // 扣除差额
            int updated = eggUserMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, userId)
                            .ge(EggUser::getPoints, delta)
                            .setSql("points = points - " + delta)
            );
            if (updated == 0) {
                throw new BusinessException("积分不足");
            }
        } else if (delta < 0) {
            // 退还差额
            eggUserMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, userId)
                            .setSql("points = points + " + (-delta))
            );
        }

        // 5.5 计算新赔率（基于更新后的奖池）
        long newPoolA = oldPoolA + netDeltaA;
        long newPoolB = oldPoolB + netDeltaB;
        Map<String, BigDecimal> newOdds = oddsService.calculate(newPoolA, newPoolB);
        BigDecimal newLockedOdds = (newBetSide == 0) ? newOdds.get("oddsA") : newOdds.get("oddsB");

        // 6. 更新下注记录
        eggBetMapper.update(null,
                new LambdaUpdateWrapper<EggBet>()
                        .eq(EggBet::getId, betId)
                        .set(EggBet::getBetSide, newBetSide)
                        .set(EggBet::getBetAmount, newBetAmount)
                        .set(EggBet::getUpdateTime, LocalDateTime.now())
        );

        return eggBetMapper.selectById(betId);
    }

    /**
     * 计算返还金额（传统彩池制：按最终奖池分账）
     * 猜中(status=1): betAmount × netPool / winningPool
     * 平局(status=1, result=2): betAmount（退还）
     * 未中(status=2): 0
     */
    private int calculateReturnedAmount(EggBet bet, EggMatch match) {
        if (bet.getStatus() == null || bet.getStatus() == 0) {
            return 0;
        }
        if (bet.getStatus() == 2) {
            return 0;
        }
        if (match != null && match.getResult() != null && match.getResult() == 2) {
            return bet.getBetAmount();
        }
        // 猜中：按最终奖池计算
        long poolA = match.getPoolA() != null ? match.getPoolA() : 0L;
        long poolB = match.getPoolB() != null ? match.getPoolB() : 0L;
        long totalPool = poolA + poolB;
        long winningPool = (match.getResult() == 0) ? poolA : poolB;
        if (winningPool == 0) return bet.getBetAmount();
        long netPool = (long)(totalPool * (1 - oddsConfig.getCommissionRate().doubleValue()));
        return (int)((long)bet.getBetAmount() * netPool / winningPool);
    }
}