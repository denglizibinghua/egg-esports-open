package com.egg.esports.module.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.egg.esports.common.BusinessException;
import com.egg.esports.common.annotation.OperationLog;
import com.egg.esports.module.match.EggMatch;
import com.egg.esports.module.match.EggMatchMapper;
import com.egg.esports.module.notification.EggNotification;
import com.egg.esports.module.notification.EggNotificationMapper;
import com.egg.esports.module.user.EggUser;
import com.egg.esports.module.user.EggUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EggCommentService {

    @Autowired
    private EggCommentMapper commentMapper;

    @Autowired
    private EggUserMapper userMapper;

    @Autowired
    private EggNotificationMapper notificationMapper;

    @Autowired
    private EggMatchMapper matchMapper;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\S+)");

    /**
     * 发表评论
     */
    public EggComment postComment(Long userId, Long matchId, Long parentId,
                                   Long replyToId, Long replyToUid, String content) {
        // 校验内容
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException("评论内容不能为空");
        }
        if (content.length() > 500) {
            throw new BusinessException("评论内容不能超过500字");
        }

        // 校验赛事存在
        EggMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new BusinessException("赛事不存在");
        }

        // 校验父评论
        if (parentId != null) {
            EggComment parent = commentMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("父评论不存在");
            }
            if (!parent.getMatchId().equals(matchId)) {
                throw new BusinessException("父评论与赛事不匹配");
            }
        }

        EggComment comment = new EggComment();
        comment.setUserId(userId);
        comment.setMatchId(matchId);
        comment.setParentId(parentId);
        comment.setReplyToId(replyToId);
        comment.setReplyToUid(replyToUid);
        // XSS 防御：剥掉所有 HTML 标签（@mention 的 span 在前端 DOMPurify 后再加）
        comment.setContent(Jsoup.clean(content.trim(), Safelist.none()));
        comment.setStatus(1);
        comment.setCreateTime(LocalDateTime.now());

        commentMapper.insert(comment);
        log.info("用户{}在赛事{}发表评论{}", userId, matchId, comment.getId());

        // 构建 CommentVO（含用户昵称/头像，前端 WebSocket 渲染需要）
        CommentVO vo = buildCommentVO(comment);

        // WebSocket 推送到 /topic/match/{matchId}
        try {
            messagingTemplate.convertAndSend("/topic/match/" + matchId, vo);
        } catch (Exception e) {
            log.warn("WebSocket推送失败: {}", e.getMessage());
        }

        // 回复通知（type=3）
        if (replyToUid != null && !replyToUid.equals(userId)) {
            EggUser replier = userMapper.selectById(userId);
            String replierName = replier != null ? replier.getNickname() : "用户";
            log.info("创建回复通知: replyToUid={}, userId={}, commentId={}", replyToUid, userId, comment.getId());
            sendNotification(replyToUid, 3, "评论被回复",
                    replierName + " 回复了你的评论", matchId);
        } else {
            log.info("跳过回复通知: replyToUid={}, userId={}", replyToUid, userId);
        }

        // @提及通知（type=4）
        Set<Long> mentionedUserIds = extractMentions(content);
        mentionedUserIds.remove(userId);
        if (replyToUid != null) {
            mentionedUserIds.remove(replyToUid);
        }
        if (!mentionedUserIds.isEmpty()) {
            EggUser mentioner = userMapper.selectById(userId);
            String mentionerName = mentioner != null ? mentioner.getNickname() : "用户";
            for (Long mentionedId : mentionedUserIds) {
                sendNotification(mentionedId, 4, "评论被@提及",
                        mentionerName + " 在评论中@了你", matchId);
            }
        }

        return comment;
    }

    /**
     * 提取@提及的用户ID
     */
    private Set<Long> extractMentions(String content) {
        Set<Long> userIds = new HashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        List<String> nicknames = new ArrayList<>();
        while (matcher.find()) {
            nicknames.add(matcher.group(1));
        }
        if (nicknames.isEmpty()) {
            return userIds;
        }
        LambdaQueryWrapper<EggUser> wrapper = new LambdaQueryWrapper<EggUser>()
                .select(EggUser::getId, EggUser::getNickname)
                .in(EggUser::getNickname, nicknames);
        List<EggUser> users = userMapper.selectList(wrapper);
        for (EggUser user : users) {
            userIds.add(user.getId());
        }
        return userIds;
    }

    /**
     * 创建通知
     */
    private void sendNotification(Long userId, Integer type, String title, String content, Long relatedId) {
        try {
            EggNotification notification = new EggNotification();
            notification.setUserId(userId);
            notification.setType(type);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setRelatedId(relatedId);
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationMapper.insert(notification);
            log.info("通知创建成功: userId={}, type={}, title={}", userId, type, title);
        } catch (Exception e) {
            log.error("创建通知失败: userId={}, type={}, error={}", userId, type, e.getMessage(), e);
        }
    }

    /**
     * 顶级评论列表（每条带前3条回复）
     */
    public Map<String, Object> listTopLevel(Long matchId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;

        List<CommentVO> all = commentMapper.selectTopLevel(matchId);
        int total = all.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentVO> pageList = fromIndex < total ? all.subList(fromIndex, toIndex) : new ArrayList<>();

        for (CommentVO vo : pageList) {
            List<CommentVO> replies = commentMapper.selectReplies(vo.getId());
            for (CommentVO reply : replies) {
                fillReplyToNickname(reply);
            }
            vo.setReplies(replies.size() > 3 ? replies.subList(0, 3) : replies);
            vo.setReplyTotal(replies.size());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        return result;
    }

    /**
     * 加载更多回复（分页）
     */
    public Map<String, Object> loadMoreReplies(Long parentId, Integer page, Integer size) {
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 10;

        List<CommentVO> all = commentMapper.selectReplies(parentId);
        int total = all.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<CommentVO> pageList = fromIndex < total ? all.subList(fromIndex, toIndex) : new ArrayList<>();
        for (CommentVO reply : pageList) {
            fillReplyToNickname(reply);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", pageList);
        result.put("total", total);
        return result;
    }

    /**
     * 构建 CommentVO（含用户信息，供 WebSocket 推送和列表返回）
     */
    private CommentVO buildCommentVO(EggComment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setUserId(comment.getUserId());
        vo.setMatchId(comment.getMatchId());
        vo.setParentId(comment.getParentId());
        vo.setReplyToId(comment.getReplyToId());
        vo.setReplyToUid(comment.getReplyToUid());
        vo.setContent(comment.getContent());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());

        // 填充评论者信息
        EggUser commenter = userMapper.selectById(comment.getUserId());
        if (commenter != null) {
            vo.setUserNickname(commenter.getNickname());
            vo.setUserAvatar(commenter.getAvatar());
        }

        // 填充被回复者昵称
        if (comment.getReplyToUid() != null) {
            EggUser replyTo = userMapper.selectById(comment.getReplyToUid());
            if (replyTo != null) {
                vo.setReplyToNickname(replyTo.getNickname());
            }
        }

        // 新评论默认无回复
        vo.setReplies(new ArrayList<>());
        vo.setReplyTotal(0);

        return vo;
    }

    /**
     * 填充被回复用户昵称
     */
    private void fillReplyToNickname(CommentVO vo) {
        if (vo.getReplyToUid() != null) {
            EggUser user = userMapper.selectById(vo.getReplyToUid());
            if (user != null) {
                vo.setReplyToNickname(user.getNickname());
            }
        }
    }

    /**
     * 删除评论（作者本人，软删除 status=0）
     */
    @OperationLog(module = "评论", value = "删除评论")
    public void deleteComment(Long id, Long userId) {
        EggComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException("无权删除他人评论");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
        log.info("用户{}删除评论{}", userId, id);
    }

    /**
     * 隐藏评论（管理员，status=0）
     */
    public void hideComment(Long id) {
        EggComment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException("评论不存在");
        }
        comment.setStatus(0);
        commentMapper.updateById(comment);
        log.info("管理员隐藏评论{}", id);
    }

    /**
     * 赛事评论数
     */
    public int countByMatch(Long matchId) {
        return commentMapper.countByMatchId(matchId);
    }
}
