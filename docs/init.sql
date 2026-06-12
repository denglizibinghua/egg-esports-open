-- ============================================
-- Egg 赛事竞猜 数据库初始化脚本
-- 数据库: egg_esports
-- ============================================

CREATE DATABASE IF NOT EXISTS egg_esports
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;
USE egg_esports;

-- ----------------------------
-- 游戏表 (多游戏扩展预留)
-- ----------------------------
DROP TABLE IF EXISTS egg_game;
CREATE TABLE egg_game (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL COMMENT '游戏名称',
    icon        VARCHAR(255) DEFAULT '' COMMENT '图标URL',
    sort_order  INT          DEFAULT 0 COMMENT '排序',
    status      TINYINT      DEFAULT 1 COMMENT '0=停用 1=启用',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏表';

-- 初始数据: 瓦
INSERT INTO egg_game (name, sort_order) VALUES ('无畏契约', 1);

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS egg_user;
CREATE TABLE egg_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL COMMENT '用户名',
    password      VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    nickname      VARCHAR(50)  DEFAULT '' COMMENT '昵称',
    avatar        VARCHAR(255) DEFAULT '' COMMENT '头像URL',
    points        INT          DEFAULT 1000 COMMENT '积分',
    total_bets    INT          DEFAULT 0 COMMENT '总押注次数',
    correct_bets  INT          DEFAULT 0 COMMENT '猜中次数',
    win_streak    INT          DEFAULT 0 COMMENT '当前连胜',
    max_win_streak INT         DEFAULT 0 COMMENT '历史最大连胜',
    role          TINYINT      DEFAULT 0 COMMENT '0=普通用户 1=管理员',
    create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 赛事表
-- ----------------------------
DROP TABLE IF EXISTS egg_match;
CREATE TABLE egg_match (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    game_id      BIGINT       NOT NULL COMMENT '游戏ID',
    title        VARCHAR(200) NOT NULL COMMENT '赛事标题',
    team_a       VARCHAR(100) NOT NULL COMMENT 'A队名称',
    team_b       VARCHAR(100) NOT NULL COMMENT 'B队名称',
    team_a_logo  VARCHAR(255) DEFAULT '' COMMENT 'A队Logo',
    team_b_logo  VARCHAR(255) DEFAULT '' COMMENT 'B队Logo',
    tournament   VARCHAR(100) DEFAULT '' COMMENT '联赛/杯赛名称',
    match_time   DATETIME     NOT NULL COMMENT '比赛时间',
    status       TINYINT      DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束',
    result       TINYINT      DEFAULT NULL COMMENT 'NULL=未结算 0=A胜 1=B胜 2=平局',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赛事表';

-- ----------------------------
-- 押注表
-- ----------------------------
DROP TABLE IF EXISTS egg_bet;
CREATE TABLE egg_bet (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    match_id    BIGINT   NOT NULL COMMENT '赛事ID',
    bet_side    TINYINT  NOT NULL COMMENT '0=押A队胜 1=押B队胜',
    bet_amount  INT      NOT NULL COMMENT '押注积分',
    status      TINYINT  DEFAULT 0 COMMENT '0=待结算 1=猜中 2=未中',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '押注时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='押注表';

-- ----------------------------
-- 索引
-- ----------------------------
CREATE INDEX idx_user_username ON egg_user(username);
CREATE INDEX idx_match_game_id ON egg_match(game_id);
CREATE INDEX idx_match_status ON egg_match(status);
CREATE INDEX idx_match_time ON egg_match(match_time);
CREATE INDEX idx_bet_user_id ON egg_bet(user_id);
CREATE INDEX idx_bet_match_id ON egg_bet(match_id);
CREATE INDEX idx_bet_user_match ON egg_bet(user_id, match_id);
