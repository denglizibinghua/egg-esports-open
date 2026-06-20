package com.egg.esports.module.comment;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论VO（含用户信息和回复列表）
 */
@Data
public class CommentVO {

    private Long id;

    private Long userId;

    private Long matchId;

    private Long parentId;

    private Long replyToId;

    private Long replyToUid;

    private String content;

    private Integer status;

    private LocalDateTime createTime;

    private String userNickname;

    private String userAvatar;

    private String replyToNickname;

    private List<CommentVO> replies;

    /** 回复总数（用于判断是否显示"展开更多"） */
    private Integer replyTotal;
}
