package com.egg.esports.sync;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事中间 DTO，字段对齐 egg_match
 * 各 Adapter 将外部数据源归一化为 RawMatch，再由 MatchSyncService 入库
 */
@Data
public class RawMatch {

    /** 外部唯一ID（Riot match.id / OpenDota match_id） */
    private String externalId;

    /** 游戏ID（1=瓦 2=LoL 3=Dota2） */
    private Long gameId;

    private String title;

    private String teamA;

    private String teamB;

    private String teamALogo;

    private String teamBLogo;

    private String tournament;

    private LocalDateTime matchTime;

    /** A队比分 */
    private Integer scoreA;

    /** B队比分 */
    private Integer scoreB;

    /** 0=未开始, 1=进行中, 2=已结束 */
    private Integer status;

    /** null=未结算, 0=A胜, 1=B胜, 2=平局 */
    private Integer result;
}
