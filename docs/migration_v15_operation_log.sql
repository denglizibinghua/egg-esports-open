-- v15: 操作日志表 — AOP 切面自动记录关键操作
-- 用于审计关键写操作（贷款/下注/结算/删评论等），JVM 重启不丢

CREATE TABLE egg_operation_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(50),
    role        TINYINT COMMENT '0=用户 1=管理员',
    module      VARCHAR(50) COMMENT '操作模块',
    action      VARCHAR(50) COMMENT '操作类型',
    description VARCHAR(500) COMMENT '操作描述',
    method      VARCHAR(200) COMMENT '方法全名',
    request_url VARCHAR(200),
    ip          VARCHAR(50),
    status      TINYINT DEFAULT 1 COMMENT '0=失败 1=成功',
    error_msg   VARCHAR(1000),
    cost_ms     BIGINT COMMENT '耗时毫秒',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 建议索引：按用户查 / 按时间倒序
CREATE INDEX idx_op_log_user_id ON egg_operation_log(user_id);
CREATE INDEX idx_op_log_create_time ON egg_operation_log(create_time);
