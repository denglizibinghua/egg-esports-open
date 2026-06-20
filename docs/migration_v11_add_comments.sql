-- ============================================
-- Egg 赛事竞猜 - 评论系统迁移
-- 新增: egg_comment 表
-- 日期: 2026-06-18
-- ============================================

CREATE TABLE IF NOT EXISTS egg_comment (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    match_id    BIGINT       NOT NULL COMMENT '赛事ID',
    parent_id   BIGINT       DEFAULT NULL COMMENT '父评论ID(NULL=顶级评论)',
    reply_to_id BIGINT       DEFAULT NULL COMMENT '被回复的评论ID',
    reply_to_uid BIGINT      DEFAULT NULL COMMENT '被回复的用户ID',
    content     VARCHAR(500) NOT NULL COMMENT '评论内容',
    status      TINYINT      DEFAULT 1 COMMENT '0=管理员隐藏 1=正常',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_match_create (match_id, create_time DESC),
    INDEX idx_parent_create (parent_id, create_time ASC),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';
