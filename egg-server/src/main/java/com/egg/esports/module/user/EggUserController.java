package com.egg.esports.module.user;

import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import com.egg.esports.module.loan.LoanService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class EggUserController {

    @Autowired
    private EggUserService userService;

    @Autowired
    private LoanService loanService;

    /**
     * 注册
     */
    @RateLimit(window = 60, maxRequests = 3, message = "注册过于频繁，请1分钟后再试")
    @PostMapping("/register")
    public Result<EggUser> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");
        String email = body.get("email");

        EggUser user = userService.register(username, password, nickname, email);
        return Result.success(user);
    }

    /**
     * 登录
     */
    @RateLimit(window = 60, maxRequests = 10, message = "登录尝试过于频繁，请1分钟后再试")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        Map<String, Object> data = userService.login(username, password);
        return Result.success(data);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<MeVO> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = userService.getById(userId);
        MeVO vo = new MeVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPoints(user.getPoints());
        vo.setTotalBets(user.getTotalBets());
        vo.setCorrectBets(user.getCorrectBets());
        vo.setWinStreak(user.getWinStreak());
        vo.setMaxWinStreak(user.getMaxWinStreak());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        vo.setRankingScore(loanService.getRankingScore(user));
        return Result.success(vo);
    }

    /**
     * 获取公开用户信息（不含密码）
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getPublicInfo(@PathVariable Long id) {
        EggUser user = userService.getById(id);

        Map<String, Object> publicInfo = Map.of(
                "id", user.getId(),
                "nickname", user.getNickname() != null ? user.getNickname() : "",
                "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                "points", user.getPoints() != null ? user.getPoints() : 0,
                "winStreak", user.getWinStreak() != null ? user.getWinStreak() : 0,
                "correctBets", user.getCorrectBets() != null ? user.getCorrectBets() : 0,
                "totalBets", user.getTotalBets() != null ? user.getTotalBets() : 0
        );

        return Result.success(publicInfo);
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public Result<EggUser> updateProfile(@RequestBody Map<String, String> body,
                                          HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = userService.updateProfile(userId, body.get("nickname"));
        return Result.success(user);
    }

    /**
     * 修改密码
     */
    @RateLimit(window = 60, maxRequests = 5, message = "操作过于频繁，请1分钟后再试")
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body,
                                        HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        userService.changePassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    /**
     * 上传头像
     */
    @RateLimit(window = 30, maxRequests = 2, message = "上传过于频繁，请30秒后再试")
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success(Map.of("avatar", avatarUrl));
    }
}