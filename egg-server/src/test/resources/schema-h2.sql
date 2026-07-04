-- ============================================
-- H2 测试库 Schema（合并 init.sql + 所有 migration 列）
-- 兼容 MODE=MySQL，DATETIME -> TIMESTAMP，去掉 ENGINE/COMMENT
-- 每次启动测试会重新创建（H2 内存库）
-- ============================================

-- ----------------------------
-- 用户表（含 v6 email / v9 贷款 / v15 登录锁定 / v19 净收益）
-- ----------------------------
DROP TABLE IF EXISTS egg_user;
CREATE TABLE egg_user (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    username          VARCHAR(50)  NOT NULL,
    password          VARCHAR(255) NOT NULL,
    nickname          VARCHAR(50)  DEFAULT '',
    email             VARCHAR(100) DEFAULT NULL,
    avatar            VARCHAR(255) DEFAULT '',
    points            INT          DEFAULT 1000,
    net_profit        INT          DEFAULT 0,
    total_bets        INT          DEFAULT 0,
    correct_bets      INT          DEFAULT 0,
    win_streak        INT          DEFAULT 0,
    max_win_streak    INT          DEFAULT 0,
    role              TINYINT      DEFAULT 0,
    create_time       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    loan_amount       INT          DEFAULT 0,
    loan_limit        INT          DEFAULT 2000,
    repaid_count      INT          DEFAULT 0,
    last_stipend_date DATE         DEFAULT NULL,
    login_attempts    INT          NOT NULL DEFAULT 0,
    locked_until      TIMESTAMP    DEFAULT NULL
);

CREATE INDEX idx_user_username ON egg_user(username);
CREATE INDEX idx_user_email ON egg_user(email);

-- ----------------------------
-- 赛事表（含 v2 external_id / v3 score / v8 奖池 / v18 settled_at / v20 bet_deadline）
-- ----------------------------
DROP TABLE IF EXISTS egg_match;
CREATE TABLE egg_match (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id   VARCHAR(100) DEFAULT NULL,
    game_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    team_a        VARCHAR(100) NOT NULL,
    team_b        VARCHAR(100) NOT NULL,
    team_a_logo   VARCHAR(255) DEFAULT '',
    team_b_logo   VARCHAR(255) DEFAULT '',
    tournament    VARCHAR(100) DEFAULT '',
    match_time    TIMESTAMP    NOT NULL,
    score_a       INT          DEFAULT NULL,
    score_b       INT          DEFAULT NULL,
    status        TINYINT      DEFAULT 0,
    result        TINYINT      DEFAULT NULL,
    create_time   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    settled_at    TIMESTAMP    DEFAULT NULL,
    bet_deadline  TIMESTAMP    DEFAULT NULL,
    pool_a        BIGINT       DEFAULT 0,
    pool_b        BIGINT       DEFAULT 0
);

CREATE INDEX idx_match_game_id ON egg_match(game_id);
CREATE INDEX idx_match_status ON egg_match(status);
CREATE INDEX idx_match_time ON egg_match(match_time);

-- ----------------------------
-- 押注表（含 v4 update_time / v8 odds_snapshot / v15 唯一约束 / v22 队伍快照）
-- ----------------------------
DROP TABLE IF EXISTS egg_bet;
CREATE TABLE egg_bet (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT   NOT NULL,
    match_id        BIGINT   NOT NULL,
    bet_side        TINYINT  NOT NULL,
    team_a_snapshot VARCHAR(100) DEFAULT NULL,
    team_b_snapshot VARCHAR(100) DEFAULT NULL,
    bet_amount      INT      NOT NULL,
    status          TINYINT  DEFAULT 0,
    update_time     TIMESTAMP DEFAULT NULL,
    create_time     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    odds_snapshot   DECIMAL(4,2) DEFAULT NULL,
    CONSTRAINT uk_user_match UNIQUE (user_id, match_id)
);

CREATE INDEX idx_bet_user_id ON egg_bet(user_id);
CREATE INDEX idx_bet_match_id ON egg_bet(match_id);

-- ----------------------------
-- 通知表（NotificationService 在测试中被 @MockBean 替换，此表实际不会被写入，
-- 但保留以便未来需要真实集成测试时可用）
-- ----------------------------
DROP TABLE IF EXISTS egg_notification;
CREATE TABLE egg_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    type        TINYINT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     VARCHAR(500) DEFAULT NULL,
    related_id  BIGINT DEFAULT NULL,
    is_read     TINYINT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 操作日志表（OperationLogService 在测试中被 @MockBean 替换，此表不会被写入）
-- ----------------------------
DROP TABLE IF EXISTS egg_operation_log;
CREATE TABLE egg_operation_log (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    username    VARCHAR(50),
    role        TINYINT,
    module      VARCHAR(50),
    action      VARCHAR(50),
    description VARCHAR(500),
    method      VARCHAR(200),
    request_url VARCHAR(200),
    ip          VARCHAR(50),
    status      TINYINT DEFAULT 1,
    error_msg   VARCHAR(1000),
    cost_ms     BIGINT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ----------------------------
-- 评论表（G2 测试需要）
-- ----------------------------
DROP TABLE IF EXISTS egg_comment;
CREATE TABLE egg_comment (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    match_id      BIGINT NOT NULL,
    parent_id     BIGINT DEFAULT NULL,
    reply_to_id   BIGINT DEFAULT NULL,
    reply_to_uid  BIGINT DEFAULT NULL,
    content       VARCHAR(500) NOT NULL,
    status        TINYINT DEFAULT 1,
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_comment_match_id ON egg_comment(match_id);
CREATE INDEX idx_comment_parent_id ON egg_comment(parent_id);
CREATE INDEX idx_comment_user_id ON egg_comment(user_id);

-- ----------------------------
-- 公告表（G3 测试需要）
-- ----------------------------
DROP TABLE IF EXISTS egg_announcement;
CREATE TABLE egg_announcement (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    content      VARCHAR(2000) DEFAULT NULL,
    game_id      BIGINT DEFAULT NULL,
    status       TINYINT DEFAULT 1,
    display_type TINYINT DEFAULT 1,
    sort_order   INT DEFAULT 0,
    start_time   TIMESTAMP DEFAULT NULL,
    end_time     TIMESTAMP DEFAULT NULL,
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_announcement_status ON egg_announcement(status);
CREATE INDEX idx_announcement_game_id ON egg_announcement(game_id);

-- ----------------------------
-- 游戏表（赛事外键依赖，G2/G3 测试间接受益）
-- ----------------------------
DROP TABLE IF EXISTS egg_game;
CREATE TABLE egg_game (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50) NOT NULL,
    icon        VARCHAR(255) DEFAULT '',
    sort_order  INT DEFAULT 0,
    status      TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
