package com.egg.esports.module.comment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("egg_comment")
public class EggComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long matchId;

    private Long parentId;

    private Long replyToId;

    private Long replyToUid;

    private String content;

    private Integer status;

    private LocalDateTime createTime;
}
