package com.egg.esports.module.bet;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("egg_bet")
public class EggBet {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long matchId;

    /** 押注方: 0=A队胜, 1=B队胜 */
    private Integer betSide;

    /** 下注时快照：A队名（防止同步翻转后历史记录错乱） */
    @TableField("team_a_snapshot")
    private String teamASnapshot;

    /** 下注时快照：B队名（防止同步翻转后历史记录错乱） */
    @TableField("team_b_snapshot")
    private String teamBSnapshot;

    private Integer betAmount;

    /** 状态: 0=待结算, 1=猜中, 2=未中 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}