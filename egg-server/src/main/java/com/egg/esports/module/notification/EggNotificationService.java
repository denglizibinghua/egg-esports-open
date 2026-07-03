package com.egg.esports.module.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.user.EggUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class EggNotificationService {

    @Autowired
    private EggNotificationMapper notificationMapper;

    @Autowired
    private EggUserMapper userMapper;

    /**
     * 获取通知列表（分页）
     */
    public Map<String, Object> getNotifications(Long userId, Integer page, Integer size, Boolean unreadOnly) {
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 20;
        }

        LambdaQueryWrapper<EggNotification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EggNotification::getUserId, userId);
        if (unreadOnly != null && unreadOnly) {
            wrapper.eq(EggNotification::getIsRead, 0);
        }
        wrapper.orderByDesc(EggNotification::getCreateTime);

        Page<EggNotification> pageResult = notificationMapper.selectPage(new Page<>(page, size), wrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageResult.getRecords());
        result.put("total", pageResult.getTotal());
        result.put("size", pageResult.getSize());
        result.put("current", pageResult.getCurrent());
        return result;
    }

    /**
     * 获取未读通知数量
     */
    public int getUnreadCount(Long userId) {
        return notificationMapper.selectUnreadCount(userId);
    }

    /**
     * 标记单条通知为已读
     */
    public void markAsRead(Long id, Long userId) {
        int rows = notificationMapper.markAsRead(id, userId);
        if (rows == 0) {
            throw new BusinessException("通知不存在或无权操作");
        }
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Long userId) {
        notificationMapper.markAllAsRead(userId);
    }

    /**
     * 创建通知（已内置去重：同一用户+同一类型+同一关联ID只保留一条）
     */
    public void create(Long userId, Integer type, String title, String content, Long relatedId) {
        // 去重：同一用户对同一赛事/事件只发一条同类型通知
        if (relatedId != null) {
            Long existing = notificationMapper.selectCount(
                    new LambdaQueryWrapper<EggNotification>()
                            .eq(EggNotification::getUserId, userId)
                            .eq(EggNotification::getType, type)
                            .eq(EggNotification::getRelatedId, relatedId)
            );
            if (existing != null && existing > 0) {
                return;
            }
        }

        EggNotification notification = new EggNotification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setRelatedId(relatedId);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());

        notificationMapper.insert(notification);
    }

    /**
     * 批量创建通知
     */
    public void batchCreate(List<EggNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        try {
            notificationMapper.batchInsert(notifications);
            log.info("批量创建通知成功: count={}", notifications.size());
        } catch (Exception e) {
            log.error("批量创建通知失败: {}", e.getMessage(), e);
        }
    }
}