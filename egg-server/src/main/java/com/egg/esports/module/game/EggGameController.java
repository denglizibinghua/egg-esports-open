package com.egg.esports.module.game;

import com.egg.esports.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 游戏接口
 */
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class EggGameController {

    private final EggGameService gameService;
    private final EggGameMapper gameMapper;

    /**
     * 游戏列表（公开）
     */
    @GetMapping("/list")
    public Result<List<EggGame>> list() {
        return Result.success(gameService.listActive());
    }

    /**
     * 游戏详情（公开）
     */
    @GetMapping("/{id}")
    public Result<EggGame> detail(@PathVariable Long id) {
        return Result.success(gameService.getById(id));
    }

    /**
     * 创建游戏（管理员）
     */
    @PostMapping
    public Result<EggGame> create(@RequestBody EggGame game,
                                  HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        game.setCreateTime(LocalDateTime.now());
        gameMapper.insert(game);
        return Result.success(game);
    }

    /**
     * 更新游戏（管理员）
     */
    @PutMapping("/{id}")
    public Result<EggGame> update(@PathVariable Long id,
                                  @RequestBody EggGame game,
                                  HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        game.setId(id);
        gameMapper.updateById(game);
        return Result.success(gameMapper.selectById(id));
    }

    /**
     * 删除游戏（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id,
                                  HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        return Result.success(gameMapper.deleteById(id) > 0);
    }
}
