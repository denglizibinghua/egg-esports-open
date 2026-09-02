-- ============================================
-- Egg Esports 热修收尾：3451/3452 队名重复一次性修复
-- 合并来源：fix-team-duplicate-by-external-id.sql + fix-team-duplicate-hotfix.sql
-- 原则：按 externalId 条件（兼容 id 漂移），不改 result/score/status/match_time/external_id
-- 目标库：egg_esports.egg_match
-- 目标行：3451 externalId 115581239610515780 (LEV vs G2) / 3452 externalId 115581239610515784 (MIBR vs LOUD)
-- 执行：mysql -h 127.0.0.1 -u egg -p"$DB_PASSWORD" egg_esports < deploy/sql/fix-3451-3452-dup.sql
-- 自愈兜底：执行后即使不立即生效，下次 MatchManageService.upsertByExternalId 同步也会自愈（标题一致性+翻转对调）
-- ============================================

-- 0. 执行前快照（留档对比）
SELECT id, external_id, team_a, team_b, title, status FROM egg_match WHERE external_id IN ('115581239610515780','115581239610515784') ORDER BY external_id;

-- 1. 修复 3452: MIBR vs LOUD (按 externalId，非 id)
UPDATE egg_match
SET team_a = 'MIBR',
    team_a_logo = 'https://static.lolesports.com/teams/1648844013086_mibr_white-logo.png',
    team_b = 'LOUD',
    team_b_logo = 'https://static.lolesports.com/teams/1644060140569_LOUDGreen.png',
    title = 'VCT Americas Playoffs — MIBR vs LOUD'
WHERE external_id = '115581239610515784' AND game_id = 1;

-- 2. 修复 3451: LEV vs G2 (按 externalId，非 id)
UPDATE egg_match
SET team_a = 'LEV',
    team_a_logo = 'https://static.lolesports.com/teams/1644148043034_Leviatncolor.png',
    team_b = 'G2',
    team_b_logo = 'https://static.lolesports.com/teams/1675329807714_G2-Esports-Logo1.png',
    title = 'VCT Americas Playoffs — LEV vs G2'
WHERE external_id = '115581239610515780' AND game_id = 1;

-- 3. 执行后验证：两行应分别显示 MIBR vs LOUD / LEV vs G2
SELECT external_id, team_a, team_b, title, status FROM egg_match WHERE external_id IN ('115581239610515780','115581239610515784') ORDER BY external_id;

-- 4. 通用自检：队名重复但非占位（应为 0 行）
SELECT id, external_id, team_a, team_b, team_a_logo, team_b_logo, title, status, match_time
FROM egg_match
WHERE team_a = team_b
  AND team_a NOT IN ('TBD','TBA','?')
  AND team_a_logo != team_b_logo;

-- 5. 通用自检：队名重复全量（含同 logo 异常），应为 0 行
SELECT COUNT(*) AS dup_cnt
FROM egg_match
WHERE team_a = team_b
  AND team_a NOT IN ('TBD','TBA','?');

-- 6. 通用自检：标题残留 TBD（应为 0 行）
SELECT id, team_a, team_b, title
FROM egg_match
WHERE title LIKE '%TBD vs%'
  AND team_a != 'TBD' AND team_b != 'TBD';

-- 7. 精确校验 3451/3452 修复正确性（各 1 行）
SELECT id, external_id, team_a, team_b, title FROM egg_match WHERE external_id='115581239610515784' AND team_a='MIBR' AND team_b='LOUD';
SELECT id, external_id, team_a, team_b, title FROM egg_match WHERE external_id='115581239610515780' AND team_a='LEV' AND team_b='G2';
