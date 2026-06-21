-- v18: 添加 settled_at 列，防止自动结算重复执行
-- autoSettlePending() 每2分钟扫描 status=2 AND result IS NOT NULL 的赛事
-- 结算后行仍匹配该条件 → 下次定时任务重复结算 → 双重派彩
-- 新增 settled_at 列，结算后置为 NOW()，查询过滤 settled_at IS NULL

ALTER TABLE egg_match ADD COLUMN settled_at DATETIME DEFAULT NULL COMMENT '结算时间';

-- 回填已结算的赛事（有 result 的视为已结算）
UPDATE egg_match SET settled_at = NOW() WHERE result IS NOT NULL AND settled_at IS NULL;
