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

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
