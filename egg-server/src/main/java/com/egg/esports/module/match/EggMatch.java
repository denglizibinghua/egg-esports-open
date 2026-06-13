package com.egg.esports.module.match;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事实体
 */
@Data
@TableName("egg_match")
public class EggMatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** Riot API match.id, unique for dedup */
    private String externalId;

    private Long gameId;

    private String title;

    private String teamA;

    private String teamB;

    private String teamALogo;

    private String teamBLogo;

    private String tournament;

    private LocalDateTime matchTime;

    /**
     * 0=未开始, 1=进行中, 2=已结束
     */
    private Integer status;

    /**
     * null=未结算, 0=A胜, 1=B胜, 2=平局
     */
    private Integer result;

    private LocalDateTime createTime;
}