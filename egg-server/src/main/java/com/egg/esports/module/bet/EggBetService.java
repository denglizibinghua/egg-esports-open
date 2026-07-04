package com.egg.esports.module.bet;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 下注服务（门面）
 * <p>拆分为 BetPlaceService（下注/修改）、BetSettleService（结算）、BetQueryService（查询）三个子服务，
 * 本类作为门面保留所有原 public 方法签名，委托给子服务实现，保证 Controller 与其他调用方无需改动。
 */
@Service
@RequiredArgsConstructor
public class EggBetService {

    private final BetPlaceService betPlaceService;

    private final BetSettleService betSettleService;

    private final BetQueryService betQueryService;

    /**
     * 下注
     */
    public EggBet placeBet(Long userId, Long matchId, Integer betSide, Integer betAmount) {
        return betPlaceService.placeBet(userId, matchId, betSide, betAmount);
    }

    /**
     * 查询我的下注记录
     */
    public List<EggBet> getMyBets(Long userId, Integer status) {
        return betQueryService.getMyBets(userId, status);
    }

    /**
     * 结算下注（赛事结算时调用）
     */
    public void settleBet(Long matchId, Integer matchResult) {
        betSettleService.settleBet(matchId, matchResult);
    }

    /**
     * 获取赛事下注数
     */
    public Long getBetCountByMatch(Long matchId) {
        return betQueryService.getBetCountByMatch(matchId);
    }

    /**
     * 近期战绩（最近10条含赛事信息）
     */
    public List<Map<String, Object>> getRecentBets(Long userId) {
        return betQueryService.getRecentBets(userId);
    }

    /**
     * 查询我的下注记录（增强版，含赛事信息和盈亏计算）
     */
    public List<BetHistoryVO> getMyBetsEnhanced(Long userId, Integer status) {
        return betQueryService.getMyBetsEnhanced(userId, status);
    }

    /**
     * 查询指定时间后已结算的下注记录
     *
     * @param userId 用户ID
     * @param since  起始时间（ISO格式）
     * @return 已结算的下注记录列表
     */
    public List<BetHistoryVO> getSettledSince(Long userId, LocalDateTime since) {
        return betQueryService.getSettledSince(userId, since);
    }

    /**
     * 修改下注
     */
    public EggBet modifyBet(Long userId, Long betId, Integer newBetSide, Integer newBetAmount) {
        return betPlaceService.modifyBet(userId, betId, newBetSide, newBetAmount);
    }
}
