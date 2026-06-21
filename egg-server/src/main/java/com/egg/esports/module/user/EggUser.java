package com.egg.esports.module.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("egg_user")
public class EggUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String email;

    private String avatar;

    private Integer points;

    private Integer totalBets;

    private Integer correctBets;

    private Integer winStreak;

    private Integer maxWinStreak;

    private Integer role;

    private LocalDateTime createTime;

    @TableField("loan_amount")
    private Integer loanAmount;  // 0 or negative

    @TableField("loan_limit")
    private Integer loanLimit;

    /** 净收益：累计赢分 - 累计输分（不含初始 1000 积分） */
    @TableField("net_profit")
    private Integer netProfit;

    @TableField("last_stipend_date")
    private LocalDate lastStipendDate;

    /** 连续登录失败次数，登录成功时重置为0 */
    @TableField("login_attempts")
    private Integer loginAttempts;

    /** 锁定截止时间，NULL表示未锁定 */
    @TableField("locked_until")
    private LocalDateTime lockedUntil;

    @TableField(exist = false)
    private Integer rankingScore;
}