-- v10: 评论系统 — 评论表 + 通知类型扩展
-- 在 Navicat 连 egg_esports 直接执行本脚本

-- 1. 评论表
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

-- 2. 扩展 egg_notification.type 注释（追加 3=评论被回复 4=评论被@提及）
ALTER TABLE egg_notification
  MODIFY COLUMN type INT DEFAULT NULL COMMENT '1=比赛即将开始 2=押注已结算 3=评论被回复 4=评论被@提及';
