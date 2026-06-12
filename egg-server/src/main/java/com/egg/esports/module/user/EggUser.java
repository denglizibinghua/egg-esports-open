package com.egg.esports.module.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("egg_user")
public class EggUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String nickname;

    private String avatar;

    private Integer points;

    private Integer totalBets;

    private Integer correctBets;

    private Integer winStreak;

    private Integer maxWinStreak;

    private Integer role;

    private LocalDateTime createTime;
}