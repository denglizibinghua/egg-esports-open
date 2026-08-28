package com.egg.esports.module.game;

import com.egg.esports.common.Result;
import com.egg.esports.common.annotation.RequireAdmin;
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
     * @param frontend 瓦聚焦：为 true 时只返回瓦洛兰特（gameId=1），后台/seed 传 false 或不传则返回全量
     */
    @GetMapping("/list")
    public Result<List<EggGame>> list(@RequestParam(required = false, defaultValue = "false") boolean frontend) {
        if (frontend) {
            return Result.success(gameService.listActiveForFrontend());
        }
        return Result.success(gameService.listActive());
    }

    /**
     * 游戏详情（公开）
     */
    @GetMapping("/{id:\\d+}")
    public Result<EggGame> detail(@PathVariable Long id) {
        return Result.success(gameService.getById(id));
    }

    /**
     * 创建游戏（管理员）
     */
    @RequireAdmin
    @PostMapping
    public Result<EggGame> create(@RequestBody EggGame game) {
        game.setCreateTime(LocalDateTime.now());
        gameMapper.insert(game);
        return Result.success(game);
    }

    /**
     * 更新游戏（管理员）
     */
    @RequireAdmin
    @PutMapping("/{id:\\d+}")
    public Result<EggGame> update(@PathVariable Long id,
                                  @RequestBody EggGame game) {
        game.setId(id);
        gameMapper.updateById(game);
        return Result.success(gameMapper.selectById(id));
    }

    /**
     * 删除游戏（管理员）
     */
    @RequireAdmin
    @DeleteMapping("/{id:\\d+}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(gameMapper.deleteById(id) > 0);
    }
}
