package com.egg.esports.module.user;

import com.egg.esports.common.Result;
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

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<EggUser> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String nickname = body.get("nickname");

        EggUser user = userService.register(username, password, nickname);
        return Result.success(user);
    }

    /**
     * 登录
     */
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
    public Result<EggUser> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggUser user = userService.getById(userId);
        return Result.success(user);
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
     * 上传头像
     */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String avatarUrl = userService.uploadAvatar(userId, file);
        return Result.success(Map.of("avatar", avatarUrl));
    }
}