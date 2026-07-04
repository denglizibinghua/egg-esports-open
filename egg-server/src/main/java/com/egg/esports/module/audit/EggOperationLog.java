package com.egg.esports.module.audit;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("egg_operation_log")
public class EggOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String username;

    /** 0=用户 1=管理员 */
    private Integer role;

    private String module;

    private String action;

    private String description;

    private String method;

    private String requestUrl;

    private String ip;

    /** 0=失败 1=成功 */
    private Integer status;

    private String errorMsg;

    private Long costMs;

    /** 业务关联 ID：押注ID（资金操作涉及押注时填充，用于资金追踪） */
    private Long betId;

    /** 业务关联 ID：赛事ID（资金操作涉及赛事时填充，用于资金追踪） */
    private Long matchId;

    /** 操作前积分（资金操作时填充，用于资金对账） */
    private Integer beforePoints;

    /** 操作后积分（资金操作时填充，用于资金对账） */
    private Integer afterPoints;

    /** 积分变动金额（正数为增加，负数为减少，用于资金对账） */
    private Integer deltaAmount;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
