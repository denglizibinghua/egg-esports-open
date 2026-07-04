package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.config.OddsConfig;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 下注查询服务（拆分自 EggBetService）
 * <p>负责下注记录查询相关逻辑。
 */
@Service
@RequiredArgsConstructor
public class BetQueryService {

    private final EggBetMapper eggBetMapper;

    private final EggMatchMapper eggMatchMapper;

    private final OddsConfig oddsConfig;

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

        // 批量查询赛事信息，避免 N+1（一次 selectBatchIds 取代逐条 selectById）
        Set<Long> matchIds = bets.stream().map(EggBet::getMatchId).collect(Collectors.toSet());
        Map<Long, EggMatch> matchMap = matchIds.isEmpty() ? Collections.emptyMap() :
                eggMatchMapper.selectBatchIds(matchIds).stream()
                        .collect(Collectors.toMap(EggMatch::getId, Function.identity()));

        return bets.stream().map(bet -> {
            EggMatch match = matchMap.get(bet.getMatchId());
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", bet.getId());
            item.put("matchId", bet.getMatchId());
            item.put("matchTitle", match != null ? match.getTitle() : "未知赛事");
            // BUG #5 修复：优先用 bet 中的队伍名快照，避免同步翻转后历史记录错乱
            item.put("teamA", bet.getTeamASnapshot() != null ? bet.getTeamASnapshot()
                    : (match != null ? match.getTeamA() : "A队"));
            item.put("teamB", bet.getTeamBSnapshot() != null ? bet.getTeamBSnapshot()
                    : (match != null ? match.getTeamB() : "B队"));
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

        // 批量查询赛事信息，避免 N+1
        Set<Long> matchIds = bets.stream().map(EggBet::getMatchId).collect(Collectors.toSet());
        Map<Long, EggMatch> matchMap = matchIds.isEmpty() ? Collections.emptyMap() :
                eggMatchMapper.selectBatchIds(matchIds).stream()
                        .collect(Collectors.toMap(EggMatch::getId, Function.identity()));

        List<BetHistoryVO> result = new ArrayList<>();
        for (EggBet bet : bets) {
            EggMatch match = matchMap.get(bet.getMatchId());
            BetHistoryVO vo = new BetHistoryVO();
            vo.setId(bet.getId());
            vo.setMatchId(bet.getMatchId());
            vo.setMatchTitle(match != null ? match.getTitle() : "未知赛事");
            // BUG #5 修复：优先用 bet 中的队伍名快照，避免同步翻转后历史记录错乱
            vo.setTeamA(bet.getTeamASnapshot() != null ? bet.getTeamASnapshot()
                    : (match != null ? match.getTeamA() : "A队"));
            vo.setTeamB(bet.getTeamBSnapshot() != null ? bet.getTeamBSnapshot()
                    : (match != null ? match.getTeamB() : "B队"));
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

        // 批量查询赛事信息，避免 N+1
        Set<Long> matchIds = bets.stream().map(EggBet::getMatchId).collect(Collectors.toSet());
        Map<Long, EggMatch> matchMap = matchIds.isEmpty() ? Collections.emptyMap() :
                eggMatchMapper.selectBatchIds(matchIds).stream()
                        .collect(Collectors.toMap(EggMatch::getId, Function.identity()));

        List<BetHistoryVO> result = new ArrayList<>();
        for (EggBet bet : bets) {
            EggMatch match = matchMap.get(bet.getMatchId());
            BetHistoryVO vo = new BetHistoryVO();
            vo.setId(bet.getId());
            vo.setMatchId(bet.getMatchId());
            vo.setMatchTitle(match != null ? match.getTitle() : "未知赛事");
            // BUG #5 修复：优先用 bet 中的队伍名快照，避免同步翻转后历史记录错乱
            vo.setTeamA(bet.getTeamASnapshot() != null ? bet.getTeamASnapshot()
                    : (match != null ? match.getTeamA() : "A队"));
            vo.setTeamB(bet.getTeamBSnapshot() != null ? bet.getTeamBSnapshot()
                    : (match != null ? match.getTeamB() : "B队"));
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
