package com.egg.esports.module.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息VO（不含密码、登录锁定等敏感字段）
 */
@Data
public class MeVO {

    private Long id;

    private String username;

    private String nickname;

    private String email;

    private String avatar;

    private Integer points;

    /** 净收益（赢分-输分，不含初始积分），对应 DB net_profit 列 */
    private Integer netProfit;

    private Integer totalBets;

    private Integer correctBets;

    private Integer winStreak;

    private Integer maxWinStreak;

    private Integer role;

    private LocalDateTime createTime;

    /** 贷款金额（0 或负数，表示欠款） */
    private Integer loanAmount;

    /** 贷款额度上限 */
    private Integer loanLimit;

    /** 排行榜积分（非数据库字段，由 /me 接口返回） */
    private Integer rankingScore;
}
