package com.egg.esports.module.announcement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口 — /api/announcement/active 为公开接口（已通过 WebConfig 排除拦截），
 * 其余接口需管理员权限（由 LoginInterceptor 统一校验）
 */
@RestController
@RequestMapping("/api/announcement")
public class EggAnnouncementController {

    @Autowired
    private EggAnnouncementService announcementService;

    /**
     * 获取当前生效的公告（公开）
     */
    @GetMapping("/active")
    public Result<List<EggAnnouncement>> getActive(@RequestParam(required = false) Long gameId) {
        return Result.success(announcementService.getActive(gameId));
    }

    /**
     * 分页查询公告列表（管理员）
     */
    @GetMapping("/list")
    public Result<IPage<EggAnnouncement>> list(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size) {
        Page<EggAnnouncement> pageParam = new Page<>(page, size);
        return Result.success(announcementService.listByPage(pageParam));
    }

    /**
     * 创建公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @PostMapping
    public Result<EggAnnouncement> create(@RequestBody EggAnnouncement announcement) {
        return Result.success(announcementService.create(announcement));
    }

    /**
     * 更新公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @PutMapping("/{id}")
    public Result<EggAnnouncement> update(@PathVariable Long id,
                                           @RequestBody EggAnnouncement announcement) {
        announcement.setId(id);
        return Result.success(announcementService.update(announcement));
    }

    /**
     * 删除公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(announcementService.delete(id));
    }
}