-- Migration V6: Add email column to egg_user
ALTER TABLE egg_user ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER nickname;
CREATE INDEX idx_user_email ON egg_user(email);
