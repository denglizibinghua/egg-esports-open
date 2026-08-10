package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.module.loan.LoanService;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.odds.OddsService;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 下注服务（拆分自 EggBetService）
 * <p>负责下注与修改下注相关逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetPlaceService {

    private final EggBetMapper eggBetMapper;

    private final EggMatchMapper eggMatchMapper;

    private final EggUserMapper eggUserMapper;

    private final OddsService oddsService;

    private final LoanService loanService;

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
        if (match.getStatus() != 0) {
            throw new BusinessException("赛事已开始或已结束，无法下注");
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

        // 4. 创建下注记录（先 INSERT，利用 (user_id, match_id) 唯一约束防并发重复下注）
        //    顺序调整原因：原顺序是 扣积分 → 改奖池 → INSERT，
        //    若 INSERT 因并发触发 DuplicateKeyException 被静默 catch，扣的积分和奖池更新不会回滚 → 用户被多扣钱
        //    新顺序：INSERT 先行，失败则直接返回（无任何副作用），成功后再扣积分/改奖池
        EggBet bet = new EggBet();
        bet.setUserId(userId);
        bet.setMatchId(matchId);
        bet.setBetSide(betSide);
        bet.setBetAmount(betAmount);
        bet.setStatus(0); // 待结算
        bet.setCreateTime(LocalDateTime.now());
        // BUG #5 修复：写入队伍名快照，防止后续同步翻转导致历史记录错乱
        bet.setTeamASnapshot(match.getTeamA());
        bet.setTeamBSnapshot(match.getTeamB());
        try {
            eggBetMapper.insert(bet);
        } catch (DuplicateKeyException e) {
            // 并发冲突：被 (user_id, match_id) 唯一约束挡下
            // 此时还没扣积分/改奖池，直接返回已存在记录（幂等），无副作用
            log.warn("并发下注被唯一约束拦截: userId={}, matchId={}", userId, matchId);
            EggBet existing = eggBetMapper.selectOne(
                    new LambdaQueryWrapper<EggBet>()
                            .eq(EggBet::getUserId, userId)
                            .eq(EggBet::getMatchId, matchId)
            );
            return existing;
        }

        // 5. INSERT 成功 → 扣除用户积分（乐观锁：只有积分 >= betAmount 时才更新）
        int updated = eggUserMapper.update(null,
                new LambdaUpdateWrapper<EggUser>()
                        .eq(EggUser::getId, userId)
                        .ge(EggUser::getPoints, betAmount)
                        .setSql("points = points - " + betAmount)
        );
        if (updated == 0) {
            // 积分不足 → 删除刚插入的 bet，抛异常触发事务回滚（双保险）
            eggBetMapper.deleteById(bet.getId());
            throw new BusinessException("积分不足");
        }

        // 6. 积分扣成功 → 原子更新奖池（乐观锁）
        long deltaA = (betSide == 0) ? betAmount : 0L;
        long deltaB = (betSide == 1) ? betAmount : 0L;
        int poolUpdated = eggMatchMapper.updatePool(matchId, deltaA, deltaB, poolA, poolB);
        if (poolUpdated == 0) {
            // 奖池冲突 → 退还积分 + 删除 bet，抛异常触发回滚
            eggUserMapper.update(null,
                    new LambdaUpdateWrapper<EggUser>()
                            .eq(EggUser::getId, userId)
                            .setSql("points = points + " + betAmount)
            );
            eggBetMapper.deleteById(bet.getId());
            throw new BusinessException("下注失败，奖池已变化，请重试");
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
     * 修改下注
     */
    @Transactional
    @OperationLog(module = "押注", value = "修改下注")
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

        // 6. 更新下注记录（乐观锁：status=0 才能修改，防止结算中修改造成数据不一致）
        int betUpdated = eggBetMapper.update(null,
                new LambdaUpdateWrapper<EggBet>()
                        .eq(EggBet::getId, betId)
                        .eq(EggBet::getStatus, 0)
                        .set(EggBet::getBetSide, newBetSide)
                        .set(EggBet::getBetAmount, newBetAmount)
                        .set(EggBet::getUpdateTime, LocalDateTime.now())
        );
        if (betUpdated == 0) {
            throw new BusinessException("投注已结算，无法修改");
        }

        return eggBetMapper.selectById(betId);
    }
}
