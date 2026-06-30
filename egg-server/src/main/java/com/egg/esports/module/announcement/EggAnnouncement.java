package com.egg.esports.module.announcement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("egg_announcement")
public class EggAnnouncement {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private Long gameId;

    private Integer status;

    /** 展示类型: 1=横幅 2=弹窗，默认1 */
    private Integer displayType;

    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    private LocalDateTime createTime;
}