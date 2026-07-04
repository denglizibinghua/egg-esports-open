package com.egg.esports.module.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("egg_user")
public class EggUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码哈希（BCrypt），序列化时永不输出，仅用于反序列化接收注册/登录入参 */
    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String avatar;

    private Integer points;

    /** 净收益（赢分-输分，不含初始积分），对应 DB net_profit 列，由 LeaderboardMapper 排行榜 SQL 引用 */
    @TableField("net_profit")
    private Integer netProfit;

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