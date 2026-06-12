package com.egg.esports.module.bet;

import com.egg.esports.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bet")
public class EggBetController {

    @Autowired
    private EggBetService eggBetService;

    /**
     * 下注
     */
    @PostMapping("/place")
    public Result<EggBet> placeBet(@RequestBody PlaceBetRequest req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggBet bet = eggBetService.placeBet(userId, req.getMatchId(), req.getBetSide(), req.getBetAmount());
        return Result.success(bet);
    }

    /**
     * 查询我的下注记录
     */
    @GetMapping("/my")
    public Result<List<EggBet>> getMyBets(@RequestParam(required = false) Integer status,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<EggBet> bets = eggBetService.getMyBets(userId, status);
        return Result.success(bets);
    }

    /**
     * 获取赛事下注数
     */
    @GetMapping("/count/{matchId}")
    public Result<Long> getBetCount(@PathVariable Long matchId) {
        Long count = eggBetService.getBetCountByMatch(matchId);
        return Result.success(count);
    }

    /**
     * 近期战绩（含赛事信息）
     */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecentBets(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(eggBetService.getRecentBets(userId));
    }

    @lombok.Data
    public static class PlaceBetRequest {
        private Long matchId;
        private Integer betSide;
        private Integer betAmount;
    }
}