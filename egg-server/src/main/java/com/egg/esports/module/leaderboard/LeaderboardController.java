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

    @GetMapping("/points")
    public Result<Page<LeaderboardVO>> points(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

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
    public Result<List<LeaderboardVO>> top(@PathVariable int n) {
        if (n < 1) {
            n = 10;
        }

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
    public Result<Map<String, Object>> myRank(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // 查询所有用户按积分降序
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

        Map<String, Object> result = new HashMap<>();
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
        if (user.getTotalBets() != null && user.getTotalBets() > 0 && user.getCorrectBets() != null) {
            vo.setAccuracy(user.getCorrectBets() * 100.0 / user.getTotalBets());
        } else {
            vo.setAccuracy(0.0);
        }
        return vo;
    }
}