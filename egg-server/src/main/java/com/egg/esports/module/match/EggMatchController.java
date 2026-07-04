package com.egg.esports.module.match;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import com.egg.esports.common.annotation.RequireAdmin;
import com.egg.esports.sync.MatchSyncService;
import com.egg.esports.sync.SyncResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 赛事接口
 */
@RestController
@RequestMapping("/api/match")
@RequiredArgsConstructor
public class EggMatchController {

    private final EggMatchService matchService;
    private final MatchSyncService matchSyncService;

    /**
     * 按联赛分组查询赛事（公开）
     */
    @GetMapping("/tournaments")
    public Result<Map<String, List<EggMatch>>> tournaments() {
        return Result.success(matchService.getMatchesByTournament());
    }

    /**
     * 赛事列表（公开）
     */
@GetMapping("/list")
    public Result<IPage<EggMatch>> list(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) Long gameId,
                                        @RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(matchService.getMatchList(status, gameId, page, size));
    }

    /**
     * 赛事详情（公开）
     */
    @GetMapping("/{id}")
    public Result<EggMatch> detail(@PathVariable Long id) {
        return Result.success(matchService.getById(id));
    }

    /**
     * 创建赛事（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @RequireAdmin
    @PostMapping("/create")
    public Result<EggMatch> create(@Valid @RequestBody EggMatch match) {
        return Result.success(matchService.create(match));
    }

    /**
     * 更新赛事信息（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @RequireAdmin
    @PutMapping("/update")
    public Result<EggMatch> update(@RequestBody EggMatch match) {
        return Result.success(matchService.update(match));
    }

    /**
     * 结算赛事结果（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @RequireAdmin
    @PutMapping("/settle")
    public Result<EggMatch> settle(@RequestBody Map<String, Object> params) {
        Long matchId = Long.valueOf(params.get("matchId").toString());
        Integer result = Integer.valueOf(params.get("result").toString());
        return Result.success(matchService.settle(matchId, result));
    }

    /**
     * 热门赛事（即将开始 + 进行中）
     */
    @GetMapping("/hot")
    public Result<List<EggMatch>> hot(@RequestParam(defaultValue = "6") int limit,
                                      @RequestParam(required = false) Long gameId) {
        return Result.success(matchService.getHotMatches(limit, gameId));
    }

    /**
     * 批量导入赛事（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @RequireAdmin
    @PostMapping("/batch-import")
    public Result<Map<String, Object>> batchImport(@RequestBody List<EggMatch> matches) {
        return Result.success(matchService.batchImport(matches));
    }

    /**
     * 手动触发数据同步（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @RequireAdmin
    @PostMapping("/sync")
    public Result<SyncResult> sync() {
        return Result.success(matchSyncService.syncAll());
    }
}