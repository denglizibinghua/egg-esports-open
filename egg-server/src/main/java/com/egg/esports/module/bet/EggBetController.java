package com.egg.esports.module.bet;

import com.egg.esports.common.BusinessException;
import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.odds.OddsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bet")
public class EggBetController {

    @Autowired
    private EggBetService eggBetService;

    @Autowired
    private EggBetMapper eggBetMapper;

    @Autowired
    private EggMatchMapper matchMapper;

    @Autowired
    private OddsService oddsService;

    /**
     * 下注
     */
    @RateLimit(window = 5, maxRequests = 3, message = "下注过于频繁，请5秒后再试")
    @PostMapping("/place")
    public Result<EggBet> placeBet(@RequestBody PlaceBetRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggBet bet = eggBetService.placeBet(userId, req.getMatchId(), req.getBetSide(), req.getBetAmount());
        return Result.success(bet);
    }

    /**
     * 查询我的下注记录（增强版，含赛事信息和盈亏计算）
     */
    @GetMapping("/my")
    public Result<List<BetHistoryVO>> getMyBets(@RequestParam(required = false) Integer status,
                                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<BetHistoryVO> bets = eggBetService.getMyBetsEnhanced(userId, status);
        return Result.success(bets);
    }

    /**
     * 查询指定时间后已结算的下注记录
     */
    @GetMapping("/settled-since")
    public Result<List<BetHistoryVO>> getSettledSince(
            @RequestParam(value = "since", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 不传 since 时返回所有已结算记录（用于收益曲线等）
        LocalDateTime sinceTime = since != null ? since : LocalDateTime.of(1970, 1, 1, 0, 0);
        List<BetHistoryVO> bets = eggBetService.getSettledSince(userId, sinceTime);
        return Result.success(bets);
    }

    /**
     * 获取赛事下注数
     */
    @GetMapping("/count/{matchId}")
    public Result<Map<String, Integer>> getBetCount(@PathVariable Long matchId) {
        Map<String, Integer> counts = new java.util.HashMap<>();
        counts.put("teamA", eggBetMapper.countBySideA(matchId));
        counts.put("teamB", eggBetMapper.countBySideB(matchId));
        return Result.success(counts);
    }

    /**
     * 近期战绩（含赛事信息）
     */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecentBets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(eggBetService.getRecentBets(userId));
    }

    /**
     * 修改下注
     */
    @RateLimit(window = 5, maxRequests = 3, message = "修改过于频繁，请5秒后再试")
    @PutMapping("/{id}")
    public Result<EggBet> modifyBet(@PathVariable Long id, @RequestBody ModifyBetRequest request, HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        EggBet bet = eggBetService.modifyBet(userId, id, request.getBetSide(), request.getBetAmount());
        return Result.success(bet);
    }

    /**
     * 获取赛事实时赔率
     */
    @GetMapping("/odds/{matchId}")
    public Result<Map<String, BigDecimal>> getOdds(@PathVariable Long matchId) {
        EggMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }
        Map<String, BigDecimal> odds = oddsService.calculate(
                match.getPoolA() != null ? match.getPoolA() : 0L,
                match.getPoolB() != null ? match.getPoolB() : 0L
        );
        return Result.success(odds);
    }

    @lombok.Data
    public static class ModifyBetRequest {
        private Integer betSide;
        private Integer betAmount;
    }

    @lombok.Data
    public static class PlaceBetRequest {
        private Long matchId;
        private Integer betSide;
        private Integer betAmount;
    }
}