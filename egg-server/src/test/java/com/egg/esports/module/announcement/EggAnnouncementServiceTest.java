package com.egg.esports.module.announcement;

import com.egg.esports.BaseH2Test;
import com.egg.esports.module.notification.EggNotification;
import com.egg.esports.module.notification.EggNotificationService;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.anyList;

/**
 * EggAnnouncementService 测试（创建公告 + 查活跃公告 + 全员推送）
 * <p>覆盖任务书 G3。
 * <p>notificationService 在 BaseH2Test 中已 @MockBean，全员推送调用 batchCreate 时可验证。
 */
@DisplayName("EggAnnouncementService 公告功能")
class EggAnnouncementServiceTest extends BaseH2Test {

    @Autowired
    private EggAnnouncementService announcementService;

    @Autowired
    private EggAnnouncementMapper announcementMapper;

    @Autowired
    private EggUserMapper userMapper;

    @Autowired
    private EggNotificationService notificationService;

    /** 每个测试前清空公告表（BaseH2Test 只清了 bet/match/user） */
    @BeforeEach
    void cleanAnnouncementTable() {
        jdbcTemplate.execute("DELETE FROM egg_announcement");
    }

    // ==================== 创建公告测试 ====================

    @Test
    @DisplayName("创建公告成功：数据入库，title/content 经过 XSS 清洗")
    void testCreate_announcement() {
        EggAnnouncement announcement = new EggAnnouncement();
        announcement.setTitle("测试公告");
        announcement.setContent("公告内容");
        announcement.setStatus(1);
        announcement.setDisplayType(1);

        EggAnnouncement created = announcementService.create(announcement);

        assertNotNull(created.getId(), "创建后应有 ID");
        assertEquals("测试公告", created.getTitle());
        assertEquals("公告内容", created.getContent());

        // DB 中应有 1 条记录
        assertEquals(1L, announcementMapper.selectCount(null), "公告表应有 1 条记录");
    }

    @Test
    @DisplayName("创建公告：XSS 清洗掉 HTML 标签")
    void testCreate_xssSanitize() {
        EggAnnouncement announcement = new EggAnnouncement();
        announcement.setTitle("<script>alert('xss')</script>公告");
        announcement.setContent("<b>加粗</b>内容");
        announcement.setStatus(0); // 禁用，避免触发推送
        announcement.setDisplayType(1);

        EggAnnouncement created = announcementService.create(announcement);

        // Jsoup.clean(Safelist.none()) 剥掉所有 HTML 标签
        assertEquals("公告", created.getTitle(), "title 中的 script 标签应被清除");
        assertEquals("加粗内容", created.getContent(), "content 中的 b 标签应被清除");
    }

    // ==================== 查活跃公告测试 ====================

    @Test
    @DisplayName("查活跃公告：只返回 status=1 且在有效期内")
    void testGetActive_active() {
        // 公告1：启用，无时间限制 → 应返回
        EggAnnouncement a1 = new EggAnnouncement();
        a1.setTitle("启用公告");
        a1.setContent("内容1");
        a1.setStatus(1);
        a1.setDisplayType(1);
        announcementService.create(a1);

        // 公告2：禁用 → 不应返回
        EggAnnouncement a2 = new EggAnnouncement();
        a2.setTitle("禁用公告");
        a2.setContent("内容2");
        a2.setStatus(0);
        a2.setDisplayType(1);
        announcementService.create(a2);

        // 公告3：启用但未到开始时间 → 不应返回
        EggAnnouncement a3 = new EggAnnouncement();
        a3.setTitle("未来公告");
        a3.setContent("内容3");
        a3.setStatus(1);
        a3.setStartTime(LocalDateTime.now().plusDays(1));
        announcementService.create(a3);

        List<EggAnnouncement> active = announcementService.getActive(null);
        assertEquals(1, active.size(), "应只返回 1 条活跃公告");
        assertEquals("启用公告", active.get(0).getTitle());
    }

    // ==================== 全员推送测试 ====================

    @Test
    @DisplayName("创建启用公告：触发全员推送，batchCreate 被调用")
    void testCreate_enabledSendsNotification() {
        // 准备 2 个用户
        createUser(1L, "user1");
        createUser(2L, "user2");

        EggAnnouncement announcement = new EggAnnouncement();
        announcement.setTitle("全员通知");
        announcement.setContent("这是一条全员通知");
        announcement.setStatus(1); // 启用 → 触发推送
        announcement.setDisplayType(1);

        announcementService.create(announcement);

        // 验证 notificationService.batchCreate 被调用，且通知数 = 用户数
        ArgumentCaptor<List<EggNotification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationService).batchCreate(captor.capture());
        List<EggNotification> notifications = captor.getValue();
        assertEquals(2, notifications.size(), "应给 2 个用户各发一条通知");
        assertEquals("全员通知", notifications.get(0).getTitle());
        assertEquals(5, notifications.get(0).getType(), "公告通知 type 应为 5");
    }

    @Test
    @DisplayName("创建禁用公告：不触发全员推送")
    void testCreate_disabledNoNotification() {
        createUser(1L, "user1");

        EggAnnouncement announcement = new EggAnnouncement();
        announcement.setTitle("禁用公告");
        announcement.setContent("内容");
        announcement.setStatus(0); // 禁用 → 不推送
        announcement.setDisplayType(1);

        announcementService.create(announcement);

        verify(notificationService, never()).batchCreate(anyList());
    }

    // ==================== 辅助方法 ====================

    private void createUser(long id, String username) {
        EggUser user = new EggUser();
        user.setId(id);
        user.setUsername(username);
        user.setPassword("pwd");
        user.setNickname(username);
        user.setPoints(1000);
        user.setNetProfit(0);
        user.setTotalBets(0);
        user.setCorrectBets(0);
        user.setWinStreak(0);
        user.setMaxWinStreak(0);
        user.setRole(0);
        user.setLoanAmount(0);
        user.setLoanLimit(2000);
        userMapper.insert(user);
    }
}
