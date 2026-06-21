-- v19: 添加 net_profit 列，用于净收益排行榜
-- net_profit = 累计赢分 - 累计输分（不含初始1000积分）
ALTER TABLE egg_user ADD COLUMN net_profit INT DEFAULT 0 COMMENT '净收益（赢分-输分，不含初始积分）';

-- 回填现有用户：近似值 = points - 1000（忽略贷款的细微影响）
UPDATE egg_user SET net_profit = points - 1000 WHERE points IS NOT NULL AND net_profit = 0;
