-- ============================================
-- v27: 操作日志表扩展资金追踪字段
-- 用于记录资金操作的上下文（betId/matchId/积分前后值/变动金额）
-- 便于从日志反向推算用户余额变化轨迹，支持资金对账
-- ============================================

ALTER TABLE egg_operation_log
    ADD COLUMN bet_id         BIGINT NULL COMMENT '业务关联ID：押注ID（资金操作涉及押注时填充）',
    ADD COLUMN match_id       BIGINT NULL COMMENT '业务关联ID：赛事ID（资金操作涉及赛事时填充）',
    ADD COLUMN before_points  INT    NULL COMMENT '操作前积分（资金操作时填充，用于资金对账）',
    ADD COLUMN after_points   INT    NULL COMMENT '操作后积分（资金操作时填充，用于资金对账）',
    ADD COLUMN delta_amount   INT    NULL COMMENT '积分变动金额（正数增加，负数减少）';

-- 资金追踪查询索引：按用户+时间查资金变动轨迹
ALTER TABLE egg_operation_log
    ADD INDEX idx_op_log_user_create (user_id, create_time DESC);

-- 按押注/赛事反查关联日志
ALTER TABLE egg_operation_log
    ADD INDEX idx_op_log_bet (bet_id),
    ADD INDEX idx_op_log_match (match_id);
