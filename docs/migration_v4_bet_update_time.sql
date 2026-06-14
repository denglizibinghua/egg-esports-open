-- ============================================
-- Egg 赛事竞猜 - 下注表增加 update_time 列
-- 用于支持已结算下注的时间查询
-- 日期: 2026-06-14
-- ============================================

ALTER TABLE egg_bet ADD COLUMN update_time DATETIME DEFAULT NULL COMMENT '结算时间' AFTER status;

-- 为已有已结算记录设置 update_time = create_time（历史数据兼容）
UPDATE egg_bet SET update_time = create_time WHERE status IN (1, 2) AND update_time IS NULL;