package com.egg.esports.module.announcement;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告接口
 *
 * 注意：/api/announcement/active 是公开接口，需要在 WebConfig 中将其加入
 * LoginInterceptor 的 excludePathPatterns 才能真正免登录访问。
 * 当前实现中，所有 /api/** 均经过 LoginInterceptor，因此 /active 接口
 * 仍需携带有效 token。如需完全公开，请更新 WebConfig：
 * .excludePathPatterns("/api/user/login", "/api/user/register", "/api/announcement/active")
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
                                               @RequestParam(defaultValue = "10") Integer size,
                                               HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        Page<EggAnnouncement> pageParam = new Page<>(page, size);
        return Result.success(announcementService.listByPage(pageParam));
    }

    /**
     * 创建公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @PostMapping
    public Result<EggAnnouncement> create(@RequestBody EggAnnouncement announcement,
                                          HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        return Result.success(announcementService.create(announcement));
    }

    /**
     * 更新公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @PutMapping("/{id}")
    public Result<EggAnnouncement> update(@PathVariable Long id,
                                           @RequestBody EggAnnouncement announcement,
                                           HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        announcement.setId(id);
        return Result.success(announcementService.update(announcement));
    }

    /**
     * 删除公告（管理员）
     */
    @RateLimit(window = 10, maxRequests = 5, message = "操作过于频繁，请10秒后再试")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id,
                                   HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role != 1) {
            return Result.error(403, "无管理权限");
        }
        return Result.success(announcementService.delete(id));
    }
}