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

    /**
     * 胜率排行榜（仅 ≥10 场用户上榜）
     */
    @GetMapping("/accuracy")
    public Result<Page<LeaderboardVO>> accuracy(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long gameId) {

        if (gameId != null) {
            int offset = (page - 1) * size;
            List<LeaderboardVO> records = leaderboardMapper.selectAccuracyLeaderboardLimit(gameId, offset, size);
            int total = leaderboardMapper.countAccuracyLeaderboard(gameId);
            // accuracy already calculated in SQL, no need fillAccuracy
            Page<LeaderboardVO> voPage = new Page<>(page, size, total);
            voPage.setRecords(records);
            return Result.success(voPage);
        }

        // Global accuracy: query all users with totalBets >= 10
        Page<EggUser> userPage = new Page<>(page, size);
        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak, EggUser::getNetProfit)
                .ge(EggUser::getTotalBets, 10)
                .orderByDesc(EggUser::getCorrectBets);

        Page<EggUser> result = eggUserMapper.selectPage(userPage, wrapper);
        // Sort by accuracy in Java (since accuracy = correctBets/totalBets)
        List<LeaderboardVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .sorted((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy()))
                .toList();

        Page<LeaderboardVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return Result.success(voPage);
    }

    /**
     * 净收益最低排行榜（娱乐性质，≥1 场上榜，ASC = 亏损最多在前）
     */
    @GetMapping("/worst-profit")
    public Result<Page<LeaderboardVO>> worstProfit(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long gameId) {

        if (gameId != null) {
            int offset = (page - 1) * size;
            List<LeaderboardVO> records = leaderboardMapper.selectWorstProfitLeaderboardLimit(gameId, offset, size);
            int total = leaderboardMapper.countWorstProfitLeaderboard(gameId);
            records.forEach(this::fillAccuracy);
            Page<LeaderboardVO> voPage = new Page<>(page, size, total);
            voPage.setRecords(records);
            return Result.success(voPage);
        }

        // Global worst-profit: sort by net_profit ASC
        Page<EggUser> userPage = new Page<>(page, size);
        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak, EggUser::getNetProfit)
                .ge(EggUser::getTotalBets, 1)
                .orderByAsc(EggUser::getNetProfit);

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
     * 胜率排行榜 Top N（仅 ≥10 场用户上榜）
     */
    @GetMapping("/accuracy/top/{n}")
    public Result<List<LeaderboardVO>> accuracyTop(@PathVariable int n,
                                                    @RequestParam(required = false) Long gameId) {
        if (n < 1) {
            n = 10;
        }
        if (n > 100) {
            n = 100;
        }

        if (gameId != null) {
            List<LeaderboardVO> records = leaderboardMapper.selectAccuracyLeaderboardTop(gameId, n);
            return Result.success(records);
        }

        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak, EggUser::getNetProfit)
                .ge(EggUser::getTotalBets, 10)
                .last("LIMIT " + n);

        List<EggUser> users = eggUserMapper.selectList(wrapper);
        List<LeaderboardVO> voList = users.stream().map(this::toVO)
                .sorted((a, b) -> Double.compare(b.getAccuracy(), a.getAccuracy()))
                .limit(n)
                .toList();
        return Result.success(voList);
    }

    /**
     * 净收益最低排行榜 Top N（娱乐，≥1 场，ASC = 亏损最多在前）
     */
    @GetMapping("/worst-profit/top/{n}")
    public Result<List<LeaderboardVO>> worstProfitTop(@PathVariable int n,
                                                       @RequestParam(required = false) Long gameId) {
        if (n < 1) {
            n = 10;
        }
        if (n > 100) {
            n = 100;
        }

        if (gameId != null) {
            List<LeaderboardVO> records = leaderboardMapper.selectWorstProfitLeaderboardTop(gameId, n);
            records.forEach(this::fillAccuracy);
            return Result.success(records);
        }

        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname, EggUser::getAvatar,
                        EggUser::getPoints, EggUser::getCorrectBets, EggUser::getTotalBets,
                        EggUser::getWinStreak, EggUser::getMaxWinStreak, EggUser::getNetProfit)
                .ge(EggUser::getTotalBets, 1)
                .orderByAsc(EggUser::getNetProfit)
                .last("LIMIT " + n);

        List<EggUser> users = eggUserMapper.selectList(wrapper);
        return Result.success(users.stream().map(this::toVO).toList());
    }

    /**
     * 我的排名（支持多维度）
     * @param dimension: points(默认/积分榜), accuracy(胜率榜), worst-profit(亏损榜)
     */
    @GetMapping("/my-rank")
    public Result<Map<String, Object>> myRank(HttpServletRequest request,
                                              @RequestParam(required = false) Long gameId,
                                              @RequestParam(defaultValue = "points") String dimension) {
        Long userId = (Long) request.getAttribute("userId");

        Map<String, Object> result = new HashMap<>();

        if ("accuracy".equals(dimension)) {
            if (gameId != null) {
                List<LeaderboardVO> all = leaderboardMapper.selectAccuracyLeaderboardAll(gameId);
                int rank = 0;
                int total = all.size();
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getUserId().equals(userId)) {
                        rank = i + 1;
                        break;
                    }
                }
                result.put("rank", rank);
                result.put("total", total);
                return Result.success(result);
            }
            // Global accuracy: find user position among ≥10 totalBets users
            List<EggUser> allUsers = eggUserMapper.selectList(
                    new LambdaQueryWrapper<EggUser>()
                            .select(EggUser::getId, EggUser::getCorrectBets, EggUser::getTotalBets)
                            .ge(EggUser::getTotalBets, 10)
            );
            // Sort by accuracy DESC
            allUsers.sort((a, b) -> {
                double accA = a.getTotalBets() > 0 ? a.getCorrectBets() * 1.0 / a.getTotalBets() : 0;
                double accB = b.getTotalBets() > 0 ? b.getCorrectBets() * 1.0 / b.getTotalBets() : 0;
                return Double.compare(accB, accA);
            });
            int rank = 0;
            int total = allUsers.size();
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getId().equals(userId)) {
                    rank = i + 1;
                    break;
                }
            }
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        if ("worst-profit".equals(dimension)) {
            if (gameId != null) {
                List<LeaderboardVO> all = leaderboardMapper.selectWorstProfitLeaderboardAll(gameId);
                int rank = 0;
                int total = all.size();
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getUserId().equals(userId)) {
                        rank = i + 1;
                        break;
                    }
                }
                result.put("rank", rank);
                result.put("total", total);
                return Result.success(result);
            }
            // Global worst-profit
            List<EggUser> allUsers = eggUserMapper.selectList(
                    new LambdaQueryWrapper<EggUser>()
                            .select(EggUser::getId, EggUser::getNetProfit)
                            .ge(EggUser::getTotalBets, 1)
                            .orderByAsc(EggUser::getNetProfit)
            );
            int rank = 0;
            int total = allUsers.size();
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getId().equals(userId)) {
                    rank = i + 1;
                    break;
                }
            }
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        // Default: points ranking (existing logic)
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
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        List<EggUser> allUsers = eggUserMapper.selectList(
                new LambdaQueryWrapper<EggUser>()
                        .select(EggUser::getId, EggUser::getPoints)
                        .orderByDesc(EggUser::getPoints)
        );
        int rank = 0;
        int total = allUsers.size();
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }
        result.put("rank", rank);
        result.put("total", total);
        return Result.success(result);
    }

    /**
     * 查询指定用户的排名（公开接口，用于 PublicProfile 等页面）
     * @param userId 目标用户ID
     * @param dimension: points(默认/积分榜), accuracy(胜率榜), worst-profit(亏损榜)
     */
    @GetMapping("/user-rank/{userId}")
    public Result<Map<String, Object>> userRank(@PathVariable Long userId,
                                                 @RequestParam(required = false) Long gameId,
                                                 @RequestParam(defaultValue = "points") String dimension) {
        Map<String, Object> result = new HashMap<>();

        if ("accuracy".equals(dimension)) {
            if (gameId != null) {
                List<LeaderboardVO> all = leaderboardMapper.selectAccuracyLeaderboardAll(gameId);
                int rank = 0;
                int total = all.size();
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getUserId().equals(userId)) {
                        rank = i + 1;
                        break;
                    }
                }
                result.put("rank", rank);
                result.put("total", total);
                return Result.success(result);
            }
            List<EggUser> allUsers = eggUserMapper.selectList(
                    new LambdaQueryWrapper<EggUser>()
                            .select(EggUser::getId, EggUser::getCorrectBets, EggUser::getTotalBets)
                            .ge(EggUser::getTotalBets, 10)
            );
            allUsers.sort((a, b) -> {
                double accA = a.getTotalBets() > 0 ? a.getCorrectBets() * 1.0 / a.getTotalBets() : 0;
                double accB = b.getTotalBets() > 0 ? b.getCorrectBets() * 1.0 / b.getTotalBets() : 0;
                return Double.compare(accB, accA);
            });
            int rank = 0;
            int total = allUsers.size();
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getId().equals(userId)) {
                    rank = i + 1;
                    break;
                }
            }
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        if ("worst-profit".equals(dimension)) {
            if (gameId != null) {
                List<LeaderboardVO> all = leaderboardMapper.selectWorstProfitLeaderboardAll(gameId);
                int rank = 0;
                int total = all.size();
                for (int i = 0; i < all.size(); i++) {
                    if (all.get(i).getUserId().equals(userId)) {
                        rank = i + 1;
                        break;
                    }
                }
                result.put("rank", rank);
                result.put("total", total);
                return Result.success(result);
            }
            List<EggUser> allUsers = eggUserMapper.selectList(
                    new LambdaQueryWrapper<EggUser>()
                            .select(EggUser::getId, EggUser::getNetProfit)
                            .ge(EggUser::getTotalBets, 1)
                            .orderByAsc(EggUser::getNetProfit)
            );
            int rank = 0;
            int total = allUsers.size();
            for (int i = 0; i < allUsers.size(); i++) {
                if (allUsers.get(i).getId().equals(userId)) {
                    rank = i + 1;
                    break;
                }
            }
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        // Default: points ranking
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
            result.put("rank", rank);
            result.put("total", total);
            return Result.success(result);
        }

        List<EggUser> allUsers = eggUserMapper.selectList(
                new LambdaQueryWrapper<EggUser>()
                        .select(EggUser::getId, EggUser::getPoints)
                        .orderByDesc(EggUser::getPoints)
        );
        int rank = 0;
        int total = allUsers.size();
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(userId)) {
                rank = i + 1;
                break;
            }
        }
        result.put("rank", rank);
        result.put("total", total);
        return Result.success(result);
    }

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
        vo.setNetProfit(user.getNetProfit());
        fillAccuracy(vo);
        return vo;
    }

    /**
     * 填充猜中率（correctBets / totalBets * 100）
     * 用于游戏排行榜 VO（SQL 未计算 accuracy 字段）
     */
    private void fillAccuracy(LeaderboardVO vo) {
        if (vo.getTotalBets() != null && vo.getTotalBets() > 0 && vo.getCorrectBets() != null) {
            vo.setAccuracy(Math.min(100.0, vo.getCorrectBets() * 100.0 / vo.getTotalBets()));
        } else {
            vo.setAccuracy(0.0);
        }
    }
}
