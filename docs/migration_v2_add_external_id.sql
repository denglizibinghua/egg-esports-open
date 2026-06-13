-- ============================================================
-- egg-esports v2 迁移: 添加 external_id 支持 Riot API 同步
-- ============================================================
-- 执行方式:
--   mysql -u admin -p egg_esports < migration_v2_add_external_id.sql
-- ============================================================

USE egg_esports;

-- 添加 external_id 列（Riot API match.id，用于去重）
ALTER TABLE egg_match
    ADD COLUMN external_id VARCHAR(50) DEFAULT NULL COMMENT '外部来源ID(RiotAPI match.id)'
    AFTER id;

-- 添加唯一索引（防止重复同步）
CREATE UNIQUE INDEX idx_external_id ON egg_match(external_id);

-- 验证
SELECT COUNT(*) AS match_count FROM egg_match;
DESC egg_match;
