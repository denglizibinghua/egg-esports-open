-- Migration V6: Add notification table
CREATE TABLE IF NOT EXISTS egg_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type TINYINT NOT NULL COMMENT '通知类型: 1=比赛即将开始, 2=押注已结算, 3=排名被超越',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    related_id BIGINT DEFAULT NULL COMMENT '关联ID(matchId/betId)',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读: 0=未读, 1=已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_read_time (user_id, is_read, create_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;