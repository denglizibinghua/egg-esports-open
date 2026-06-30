package com.egg.esports.module.comment;

import com.egg.esports.common.RateLimit;
import com.egg.esports.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/comment")
public class EggCommentController {

    @Autowired
    private EggCommentService commentService;

    /**
     * 评论列表（顶级评论 + 前3条回复）
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Long matchId,
                                             @RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(commentService.listTopLevel(matchId, page, size));
    }

    /**
     * 加载更多回复
     */
    @GetMapping("/replies")
    public Result<Map<String, Object>> replies(@RequestParam Long parentId,
                                                @RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(commentService.loadMoreReplies(parentId, page, size));
    }

    /**
     * 发表评论
     */
    @RateLimit(window = 5, maxRequests = 3, message = "评论过于频繁，请5秒后再试")
    @PostMapping("/post")
    public Result<EggComment> post(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        EggComment comment = commentService.postComment(userId,
                Long.valueOf(body.get("matchId").toString()),
                toLongOrNull(body.get("parentId")),
                toLongOrNull(body.get("replyToId")),
                toLongOrNull(body.get("replyToUid")),
                (String) body.get("content"));
        return Result.success(comment);
    }

    /**
     * 删除评论（作者本人）
     */
    @DeleteMapping("/{id:\\d+}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        commentService.deleteComment(id, userId);
        return Result.success();
    }

    /**
     * 隐藏评论（管理员）
     */
    @PutMapping("/hide/{id:\\d+}")
    public Result<Void> hide(@PathVariable Long id) {
        commentService.hideComment(id);
        return Result.success();
    }

    /**
     * 赛事评论数
     */
    @GetMapping("/count/{matchId:\\d+}")
    public Result<Integer> count(@PathVariable Long matchId) {
        return Result.success(commentService.countByMatch(matchId));
    }

    private Long toLongOrNull(Object value) {
        if (value == null) {
            return null;
        }
        return Long.valueOf(value.toString());
    }
}
