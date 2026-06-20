-- v8: 动态赔率 — 奖池 + 赔率快照
ALTER TABLE egg_match
  ADD COLUMN pool_a BIGINT DEFAULT 0 COMMENT 'A队奖池(积分)',
  ADD COLUMN pool_b BIGINT DEFAULT 0 COMMENT 'B队奖池(积分)';

ALTER TABLE egg_bet
  ADD COLUMN odds_snapshot DECIMAL(4,2) DEFAULT NULL COMMENT '下注时锁定赔率';
