-- v9: 贷款系统 — 救济贷款 + 自动还贷 + 每日补贴
ALTER TABLE egg_user
  ADD COLUMN loan_amount INT DEFAULT 0 COMMENT '当前贷款余额(负数表示欠款)',
  ADD COLUMN loan_limit INT DEFAULT 2000 COMMENT '贷款上限',
  ADD COLUMN repaid_count INT DEFAULT 0 COMMENT '累计还清次数',
  ADD COLUMN last_stipend_date DATE DEFAULT NULL COMMENT '上次领取每日补贴日期';
