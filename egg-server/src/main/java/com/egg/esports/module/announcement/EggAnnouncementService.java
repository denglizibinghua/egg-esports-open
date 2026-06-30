package com.egg.esports.module.announcement;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import com.egg.esports.module.notification.EggNotificationService;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class EggAnnouncementService {

    @Autowired
    private EggAnnouncementMapper announcementMapper;

    @Autowired
    private EggNotificationService notificationService;

    @Autowired
    private EggUserMapper userMapper;

    /**
     * 获取当前生效的公告列表（公开接口）
     *
     * @param gameId 游戏ID，null则只查全站公告
     */
    public List<EggAnnouncement> getActive(Long gameId) {
        return getActive(gameId, null);
    }

    /**
     * 获取当前生效的公告列表（公开接口），可按展示类型过滤
     *
     * @param gameId      游戏ID，null则只查全站公告
     * @param displayType 展示类型: 1=横幅 2=弹窗，null则不过滤
     */
    public List<EggAnnouncement> getActive(Long gameId, Integer displayType) {
        QueryWrapper<EggAnnouncement> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (gameId != null) {
            wrapper.and(w -> w.isNull("game_id").or().eq("game_id", gameId));
        } else {
            wrapper.isNull("game_id");
        }

        if (displayType != null) {
            wrapper.eq("display_type", displayType);
        }

        LocalDateTime now = LocalDateTime.now();
        wrapper.and(w -> w.isNull("start_time").or().le("start_time", now));
        wrapper.and(w -> w.isNull("end_time").or().ge("end_time", now));
        wrapper.orderByAsc("sort_order");

        return announcementMapper.selectList(wrapper);
    }

    /**
     * 分页查询公告列表（管理接口）
     */
    public IPage<EggAnnouncement> listByPage(Page<EggAnnouncement> page) {
        QueryWrapper<EggAnnouncement> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("create_time");
        return announcementMapper.selectPage(page, wrapper);
    }

    /**
     * 创建公告
     */
    public EggAnnouncement create(EggAnnouncement announcement) {
        sanitizeAnnouncement(announcement);
        announcement.setCreateTime(LocalDateTime.now());
        announcementMapper.insert(announcement);

        // 仅启用的公告发送通知
        if (announcement.getStatus() != null && announcement.getStatus() == 1) {
            sendAnnouncementNotification(announcement);
        }

        return announcement;
    }

    /**
     * 更新公告
     */
    public EggAnnouncement update(EggAnnouncement announcement) {
        sanitizeAnnouncement(announcement);
        announcementMapper.updateById(announcement);
        return announcementMapper.selectById(announcement.getId());
    }

    /**
     * XSS 防御：清洗 title 和 content
     */
    private void sanitizeAnnouncement(EggAnnouncement announcement) {
        if (announcement.getTitle() != null) {
            announcement.setTitle(Jsoup.clean(announcement.getTitle(), Safelist.none()));
        }
        if (announcement.getContent() != null) {
            announcement.setContent(Jsoup.clean(announcement.getContent(), Safelist.none()));
        }
    }

    /**
     * 删除公告
     */
    public boolean delete(Long id) {
        return announcementMapper.deleteById(id) > 0;
    }

    /**
     * 公告发布后，给所有用户发送通知（type=5）
     */
    private void sendAnnouncementNotification(EggAnnouncement announcement) {
        List<EggUser> users = userMapper.selectList(new LambdaQueryWrapper<>());
        String content = announcement.getContent();
        if (content != null && content.length() > 50) {
            content = content.substring(0, 50) + "...";
        }
        for (EggUser user : users) {
            notificationService.create(user.getId(), 5, announcement.getTitle(), content, announcement.getId());
        }
    }
}