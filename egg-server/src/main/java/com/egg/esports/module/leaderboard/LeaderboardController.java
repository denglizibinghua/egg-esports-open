package com.egg.esports.module.leaderboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.Result;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final EggUserMapper eggUserMapper;
    private final LeaderboardMapper leaderboardMapper;

    @GetMapping("/points")
    public Result<Page<LeaderboardVO>> points(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long gameId) {

        // 按游戏过滤：按「猜中次数」排名
        if (gameId != null) {
            int offset = (page - 1) * size;
            List<LeaderboardVO> records = leaderboardMapper.selectGameLeaderboardLimit(gameId, offset, size);
            int total = leaderboardMapper.countGameLeaderboard(gameId);
            records.forEach(this::fillAccuracy);
            Page<LeaderboardVO> voPage = new Page<>(page, size, total);
            voPage.setRecords(records);
            return Result.success(voPage);
        }

        // 默认：全局积分榜
        Page<EggUser> userPage = new Page<>(page, size);
        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak)
                .orderByDesc(EggUser::getPoints);

        Page<EggUser> result = eggUserMapper.selectPage(userPage, wrapper);

        Page<LeaderboardVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return Result.success(voPage);
    }

    @GetMapping("/top/{n}")
    public Result<List<LeaderboardVO>> top(@PathVariable int n,
                                           @RequestParam(required = false) Long gameId) {
        if (n < 1) {
            n = 10;
        }
        if (n > 100) {
            n = 100;
        }

        // 按游戏过滤：按「猜中次数」排名 Top N
        if (gameId != null) {
            List<LeaderboardVO> records = leaderboardMapper.selectGameLeaderboardTop(gameId, n);
            records.forEach(this::fillAccuracy);
            return Result.success(records);
        }

        // 默认：全局积分榜 Top N
        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak)
                .orderByDesc(EggUser::getPoints)
                .last("LIMIT " + n);

        List<EggUser> users = eggUserMapper.selectList(wrapper);
        return Result.success(users.stream().map(this::toVO).toList());
    }

    /**
     * 我的排名
     */
    @GetMapping("/my-rank")
    public Result<Map<String, Object>> myRank(HttpServletRequest request,
                                              @RequestParam(required = false) Long gameId) {
        Long userId = (Long) request.getAttribute("userId");

        // 按游戏过滤：在游戏排行榜中定位
        if (gameId != null) {
            List<LeaderboardVO> all = leaderboardMapper.selectGameLeaderboardAll(gameId);
            int rank = 0;
            int total = all.size();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).getUserId().equals(userId)) {
                    rank = i + 1;
                    break;
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        // 默认：全局积分榜排名（SQL 计数取代全表加载 + 内存遍历）
        EggUser me = eggUserMapper.selectById(userId);
        Map<String, Object> result = new HashMap<>();
        if (me == null) {
            result.put("rank", 0);
            result.put("total", 0);
            return Result.success(result);
        }
        // 排名 = 比我积分高的人数 + 1（同分并列时排名一致，语义比内存遍历更准确）
        Long rank = eggUserMapper.selectCount(
                new LambdaQueryWrapper<EggUser>().gt(EggUser::getPoints, me.getPoints())
        ) + 1;
        Long total = eggUserMapper.selectCount(null);
        result.put("rank", rank);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== 胜率榜 ====================

    @GetMapping("/accuracy")
    public Result<Page<LeaderboardVO>> accuracy(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long gameId) {
        int offset = (page - 1) * size;
        List<LeaderboardVO> records;
        int total;
        if (gameId != null) {
            records = leaderboardMapper.selectAccuracyLeaderboardLimit(gameId, offset, size);
            total = leaderboardMapper.countAccuracyLeaderboard(gameId);
        } else {
            records = leaderboardMapper.selectAccuracyLeaderboardLimitAll(offset, size);
            total = leaderboardMapper.countAccuracyLeaderboardAll();
        }
        Page<LeaderboardVO> voPage = new Page<>(page, size, total);
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @GetMapping("/accuracy/top/{n}")
    public Result<List<LeaderboardVO>> accuracyTop(@PathVariable int n,
                                                    @RequestParam(required = false) Long gameId) {
        if (n < 1) n = 10;
        if (n > 100) n = 100;
        List<LeaderboardVO> records = gameId != null
                ? leaderboardMapper.selectAccuracyLeaderboardTop(gameId, n)
                : leaderboardMapper.selectAccuracyLeaderboardTopAll(n);
        return Result.success(records);
    }

    // ==================== 亏损榜 ====================

    @GetMapping("/worst-profit")
    public Result<Page<LeaderboardVO>> worstProfit(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long gameId) {
        int offset = (page - 1) * size;
        List<LeaderboardVO> records;
        int total;
        if (gameId != null) {
            records = leaderboardMapper.selectWorstProfitLeaderboardLimit(gameId, offset, size);
            total = leaderboardMapper.countWorstProfitLeaderboard(gameId);
        } else {
            records = leaderboardMapper.selectWorstProfitLeaderboardLimitAll(offset, size);
            total = leaderboardMapper.countWorstProfitLeaderboardAll();
        }
        Page<LeaderboardVO> voPage = new Page<>(page, size, total);
        voPage.setRecords(records);
        return Result.success(voPage);
    }

    @GetMapping("/worst-profit/top/{n}")
    public Result<List<LeaderboardVO>> worstProfitTop(@PathVariable int n,
                                                       @RequestParam(required = false) Long gameId) {
        if (n < 1) n = 10;
        if (n > 100) n = 100;
        List<LeaderboardVO> records = gameId != null
                ? leaderboardMapper.selectWorstProfitLeaderboardTop(gameId, n)
                : leaderboardMapper.selectWorstProfitLeaderboardTopAll(n);
        return Result.success(records);
    }

    // ==================== 内部方法 ====================

    private LeaderboardVO toVO(EggUser user) {
        LeaderboardVO vo = new LeaderboardVO();
        vo.setUserId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPoints(user.getPoints());
        vo.setCorrectBets(user.getCorrectBets());
        vo.setTotalBets(user.getTotalBets());
        vo.setWinStreak(user.getWinStreak());
        vo.setMaxWinStreak(user.getMaxWinStreak());
        fillAccuracy(vo);
        return vo;
    }

    /**
     * 填充猜中率（correctBets / totalBets * 100）
     * 用于游戏排行榜 VO（SQL 未计算 accuracy 字段）
     */
    private void fillAccuracy(LeaderboardVO vo) {
        if (vo.getTotalBets() != null && vo.getTotalBets() > 0 && vo.getCorrectBets() != null) {
            vo.setAccuracy(vo.getCorrectBets() * 100.0 / vo.getTotalBets());
        } else {
            vo.setAccuracy(0.0);
        }
    }
}
