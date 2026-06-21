package com.egg.esports.module.user;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息VO（不含密码、邮箱、贷款、锁定等敏感字段）
 */
@Data
public class MeVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private Integer points;

    private Integer totalBets;

    private Integer correctBets;

    private Integer winStreak;

    private Integer maxWinStreak;

    private Integer role;

    private LocalDateTime createTime;

    /** 排行榜积分（非数据库字段，由 LoanService 计算） */
    private Integer rankingScore;
}
