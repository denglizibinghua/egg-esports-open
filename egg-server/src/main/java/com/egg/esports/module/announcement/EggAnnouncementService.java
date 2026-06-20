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

@Service
public class EggAnnouncementService {

    @Autowired
    private EggAnnouncementMapper announcementMapper;

    /**
     * 获取当前生效的公告列表（公开接口）
     *
     * @param gameId 游戏ID，null则只查全站公告
     */
    public List<EggAnnouncement> getActive(Long gameId) {
        QueryWrapper<EggAnnouncement> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (gameId != null) {
            wrapper.and(w -> w.isNull("game_id").or().eq("game_id", gameId));
        } else {
            wrapper.isNull("game_id");
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
}