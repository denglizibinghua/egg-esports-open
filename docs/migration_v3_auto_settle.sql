-- ============================================
-- Egg 赛事竞猜 - 自动结算升级迁移
-- 新增: score_a, score_b 列
-- 日期: 2026-06-14
-- ============================================

ALTER TABLE egg_match ADD COLUMN IF NOT EXISTS score_a INT DEFAULT NULL COMMENT 'A队比分（实时更新）' AFTER match_time;
ALTER TABLE egg_match ADD COLUMN IF NOT EXISTS score_b INT DEFAULT NULL COMMENT 'B队比分（实时更新）' AFTER score_a;
