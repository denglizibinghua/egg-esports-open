package com.egg.esports.module.notification;

import com.egg.esports.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class EggNotificationController {

    @Autowired
    private EggNotificationService notificationService;

    /**
     * 获取通知列表
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(HttpServletRequest request,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size,
                                             @RequestParam(required = false) Boolean unreadOnly) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> data = notificationService.getNotifications(userId, page, size, unreadOnly);
        return Result.success(data);
    }

    /**
     * 获取未读通知数量
     */
    @GetMapping("/unread-count")
    public Result<Integer> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }

    /**
     * 标记单条通知为已读
     */
    @PutMapping("/read/{id:\\d+}")
    public Result<Void> markAsRead(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    /**
     * 标记所有通知为已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAllAsRead(userId);
        return Result.success();
    }
}