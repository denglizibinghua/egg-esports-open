-- ============================================
-- Egg Esports 紧急热修：修复队名重复 Bug
-- 问题：MIBR vs LOUD 显示为 LOUD vs LOUD，LEV vs G2 显示为 LEV vs LEV
-- 根因：MatchManageService 占位符翻转逻辑导致 TBD 占位被同队覆盖
-- 执行：生产库 /opt/egg-esports/.env 指向的 egg_esports 库
-- 时间：2026-08-31
-- ============================================

-- 1. 修复当前已脏数据（线上 3451/3452）
-- 3452: VCT Americas Playoffs LB Semi — 正确 MIBR vs LOUD（进行中 0-1）
UPDATE egg_match
SET team_a = 'MIBR',
    team_a_logo = 'https://static.lolesports.com/teams/1648844013086_mibr_white-logo.png',
    team_b = 'LOUD',
    team_b_logo = 'https://static.lolesports.com/teams/1644060140569_LOUDGreen.png',
    title = 'VCT Americas Playoffs — MIBR vs LOUD'
WHERE id = 3452 AND external_id = '115581239610515784';

-- 3451: VCT Americas Playoffs — 正确 LEV vs G2（已结束 1-2，G2胜）
UPDATE egg_match
SET team_a = 'LEV',
    team_a_logo = 'https://static.lolesports.com/teams/1644148043034_Leviatncolor.png',
    team_b = 'G2',
    team_b_logo = 'https://static.lolesports.com/teams/1675329807714_G2-Esports-Logo1.png',
    title = 'VCT Americas Playoffs — LEV vs G2'
WHERE id = 3451 AND external_id = '115581239610515780';

-- 2. 通用自检：找出所有队名重复但队标不同的脏数据（应为 0 行）
SELECT id, external_id, team_a, team_b, team_a_logo, team_b_logo, title, status, match_time
FROM egg_match
WHERE team_a = team_b
  AND team_a NOT IN ('TBD','TBA','?')
  AND team_a_logo != team_b_logo;

-- 3. 通用自检：标题与队名不一致（如标题 TBD vs LOUD 但实际 LOUD vs LOUD）
SELECT id, team_a, team_b, title
FROM egg_match
WHERE title LIKE '%TBD vs%'
  AND team_a != 'TBD' AND team_b != 'TBD';

-- 4. 验证修复后
SELECT id, external_id, team_a, team_b, title, status, score_a, score_b
FROM egg_match
WHERE id IN (3451, 3452);

-- 5. 缓存清理（若启用 Redis 缓存热点赛事）
-- redis-cli -a $REDIS_PASSWORD DEL "match:hot" "match:list"
-- 或等待 10 分钟定时同步自动覆盖
