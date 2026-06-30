-- migration_v22: egg_bet 加队伍名快照列，防止同步翻转导致押注历史错乱
-- 执行方式：sudo mysql -u root -padmin123 egg_esports < docs/migration_v22_bet_snapshot.sql

ALTER TABLE egg_bet
    ADD COLUMN team_a_snapshot VARCHAR(100) DEFAULT NULL COMMENT '下注时A队名快照' AFTER bet_side,
    ADD COLUMN team_b_snapshot VARCHAR(100) DEFAULT NULL COMMENT '下注时B队名快照' AFTER team_a_snapshot;
