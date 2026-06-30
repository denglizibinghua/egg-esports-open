package com.egg.esports.module.bet;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下注历史VO（含赛事信息和盈亏计算）
 */
@Data
public class BetHistoryVO {

    private Long id;

    private Long matchId;

    private String matchTitle;

    private String teamA;

    private String teamB;

    /** 下注时快照队名，优先于 teamA/teamB 展示 */
    private String teamASnapshot;

    private String teamBSnapshot;

    private Integer betSide;

    private Integer betAmount;

    /**
     * 返还金额：猜中=betAmount*winMultiplier, 平局=betAmount, 未中=0
     */
    private Integer returnedAmount;

    /**
     * 净变化：猜中=betAmount*(winMultiplier-1), 平局=0, 未中=-betAmount
     */
    private Integer netChange;

    /**
     * 状态: 0=待结算, 1=猜中, 2=未中
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}