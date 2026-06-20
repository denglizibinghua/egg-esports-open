-- v15: 登录失败限制 — 同一账号5次密码错误锁15分钟
-- 字段说明：
--   login_attempts: 连续登录失败次数，登录成功时重置为0
--   locked_until:   锁定截止时间，NULL表示未锁定；达到5次失败时设为 now+15min

ALTER TABLE egg_user
    ADD COLUMN login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until DATETIME NULL;
