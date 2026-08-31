-- 生产热修（按 externalId，兼容 id 漂移）
-- 在服务器执行： mysql -h 127.0.0.1 -u egg -p"$DB_PASSWORD" egg_esports < fix-team-duplicate-by-external-id.sql

-- 3452: MIBR vs LOUD
UPDATE egg_match
SET team_a = 'MIBR',
    team_a_logo = 'https://static.lolesports.com/teams/1648844013086_mibr_white-logo.png',
    team_b = 'LOUD',
    team_b_logo = 'https://static.lolesports.com/teams/1644060140569_LOUDGreen.png',
    title = 'VCT Americas Playoffs — MIBR vs LOUD'
WHERE external_id = '115581239610515784' AND game_id = 1;

-- 3451: LEV vs G2
UPDATE egg_match
SET team_a = 'LEV',
    team_a_logo = 'https://static.lolesports.com/teams/1644148043034_Leviatncolor.png',
    team_b = 'G2',
    team_b_logo = 'https://static.lolesports.com/teams/1675329807714_G2-Esports-Logo1.png',
    title = 'VCT Americas Playoffs — LEV vs G2'
WHERE external_id = '115581239610515780' AND game_id = 1;

SELECT external_id, team_a, team_b, title, status FROM egg_match WHERE external_id IN ('115581239610515780','115581239610515784');
