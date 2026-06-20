-- v15: 押注接口幂等性 — 防止并发重复下注
-- 通过 (user_id, match_id) 唯一约束保证同一用户对同一比赛只能有一条 bet 记录
--
-- 执行前注意：
--   如果 egg_bet 表里已存在同一用户对同一比赛的多条记录，ADD UNIQUE KEY 会失败。
--   需先清理重复数据，保留每对 (user_id, match_id) 最早的一条：
--
--   DELETE b1 FROM egg_bet b1
--   INNER JOIN egg_bet b2
--   ON b1.user_id = b2.user_id
--      AND b1.match_id = b2.match_id
--      AND b1.id > b2.id;
--
-- 然后再执行下面的 ALTER：

ALTER TABLE egg_bet ADD UNIQUE KEY uk_user_match (user_id, match_id);
