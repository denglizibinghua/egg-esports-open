package com.egg.esports.module.comment;

import com.egg.esports.BaseH2Test;
import com.egg.esports.common.BusinessException;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * EggCommentService 测试（发评论 + 删评论 + 查列表 + 加载更多回复）
 * <p>覆盖任务书 G2。
 * <p>SimpMessagingTemplate（WebSocket 推送）用 @MockBean 替换，避免实际 WebSocket 连接。
 */
@DisplayName("EggCommentService 评论功能")
class EggCommentServiceTest extends BaseH2Test {

    @Autowired
    private EggCommentService commentService;

    @Autowired
    private EggCommentMapper commentMapper;

    @Autowired
    private EggUserMapper userMapper;

    @Autowired
    private EggMatchMapper matchMapper;

    /** WebSocket 推送用 mock 替换，避免测试环境无 broker */
    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    /** 每个测试前清空评论表（BaseH2Test 只清了 bet/match/user） */
    @BeforeEach
    void cleanCommentTable() {
        jdbcTemplate.execute("DELETE FROM egg_comment");
    }

    // ==================== 辅助方法 ====================

    private EggUser createUser(long id, String nickname) {
        EggUser user = new EggUser();
        user.setId(id);
        user.setUsername("user" + id);
        user.setPassword("pwd");
        user.setNickname(nickname);
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
        return user;
    }

    private EggMatch createMatch(long id) {
        EggMatch match = new EggMatch();
        match.setId(id);
        match.setGameId(1L);
        match.setTitle("测试赛事" + id);
        match.setTeamA("A队");
        match.setTeamB("B队");
        match.setTournament("测试联赛");
        match.setMatchTime(LocalDateTime.now().plusDays(1));
        match.setStatus(0);
        match.setPoolA(0L);
        match.setPoolB(0L);
        matchMapper.insert(match);
        return match;
    }

    // ==================== postComment 测试 ====================

    @Test
    @DisplayName("发评论成功：内容入库，status=1")
    void testPostComment_success() {
        createUser(1L, "张三");
        createMatch(1L);

        EggComment comment = commentService.postComment(1L, 1L, null, null, null, "这是一条评论");

        assertNotNull(comment.getId(), "评论应有 ID");
        assertEquals("这是一条评论", comment.getContent());
        assertEquals(1, comment.getStatus(), "状态应为 1 正常");
        assertEquals(1L, comment.getMatchId());
        assertEquals(1L, comment.getUserId());

        // DB 中应有 1 条记录
        assertEquals(1L, commentMapper.selectCount(null), "评论表应有 1 条记录");
    }

    @Test
    @DisplayName("发评论失败：空内容")
    void testPostComment_emptyContent() {
        createUser(1L, "张三");
        createMatch(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                commentService.postComment(1L, 1L, null, null, null, "   "));
        assertEquals("评论内容不能为空", ex.getMessage());
    }

    @Test
    @DisplayName("发评论失败：赛事不存在")
    void testPostComment_matchNotFound() {
        createUser(1L, "张三");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                commentService.postComment(1L, 999L, null, null, null, "评论内容"));
        assertEquals("赛事不存在", ex.getMessage());
    }

    @Test
    @DisplayName("发评论失败：父评论不存在")
    void testPostComment_parentNotFound() {
        createUser(1L, "张三");
        createMatch(1L);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                commentService.postComment(1L, 1L, 999L, null, null, "回复内容"));
        assertEquals("父评论不存在", ex.getMessage());
    }

    // ==================== deleteComment 测试 ====================

    @Test
    @DisplayName("删除评论成功：作者本人删除，status 变为 0")
    void testDeleteComment_success() {
        createUser(1L, "张三");
        createMatch(1L);
        EggComment comment = commentService.postComment(1L, 1L, null, null, null, "待删除");

        commentService.deleteComment(comment.getId(), 1L);

        EggComment updated = commentMapper.selectById(comment.getId());
        assertEquals(0, updated.getStatus(), "删除后 status 应为 0");
    }

    @Test
    @DisplayName("删除评论失败：非作者无权删除")
    void testDeleteComment_notOwner() {
        createUser(1L, "张三");
        createUser(2L, "李四");
        createMatch(1L);
        EggComment comment = commentService.postComment(1L, 1L, null, null, null, "张三的评论");

        BusinessException ex = assertThrows(BusinessException.class, () ->
                commentService.deleteComment(comment.getId(), 2L));
        assertEquals("无权删除他人评论", ex.getMessage());
    }

    // ==================== listTopLevel 测试 ====================

    @Test
    @DisplayName("查顶级评论列表：按时间倒序，含分页")
    void testListTopLevel() {
        createUser(1L, "张三");
        createMatch(1L);

        // 发 3 条顶级评论
        commentService.postComment(1L, 1L, null, null, null, "评论1");
        commentService.postComment(1L, 1L, null, null, null, "评论2");
        commentService.postComment(1L, 1L, null, null, null, "评论3");

        Map<String, Object> result = commentService.listTopLevel(1L, 1, 10);

        assertEquals(3, result.get("total"), "总数应为 3");
        @SuppressWarnings("unchecked")
        java.util.List<CommentVO> records = (java.util.List<CommentVO>) result.get("records");
        assertEquals(3, records.size(), "应返回 3 条记录");
        // 按时间倒序，最新在前
        assertEquals("评论3", records.get(0).getContent(), "最新评论应排第一");
    }

    // ==================== loadMoreReplies 测试 ====================

    @Test
    @DisplayName("加载更多回复：返回指定父评论的回复列表")
    void testLoadMoreReplies() {
        createUser(1L, "张三");
        createUser(2L, "李四");
        createMatch(1L);

        // 先发一条顶级评论
        EggComment parent = commentService.postComment(1L, 1L, null, null, null, "顶级评论");

        // 发 2 条回复
        commentService.postComment(2L, 1L, parent.getId(), null, 1L, "回复1");
        commentService.postComment(2L, 1L, parent.getId(), null, 1L, "回复2");

        Map<String, Object> result = commentService.loadMoreReplies(parent.getId(), 1, 10);

        assertEquals(2, result.get("total"), "回复总数应为 2");
        @SuppressWarnings("unchecked")
        java.util.List<CommentVO> records = (java.util.List<CommentVO>) result.get("records");
        assertEquals(2, records.size(), "应返回 2 条回复");
        // 按时间正序，先发的在前
        assertEquals("回复1", records.get(0).getContent(), "回复1 应排第一");
    }
}
