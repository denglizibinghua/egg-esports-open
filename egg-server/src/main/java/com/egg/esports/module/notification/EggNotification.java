package com.egg.esports.module.notification;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("egg_notification")
public class EggNotification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer type; // 1=比赛即将开始 2=押注已结算 3=评论被回复 4=评论被@提及

    private String title;

    private String content;

    private Long relatedId;

    private Integer isRead;

    private LocalDateTime createTime;
}