-- 修复战队头像 Mixed Content：http → https
-- 根因：RiotAdapter 未做 toHttps，导致 https://eggesports.com 页面加载 http://static.lolesports.com 被浏览器拦截
-- 前端已加 fixLogoUrl 防御，下游同步已改 RiotAdapter.toHttps()，本脚本修复存量脏数据
-- 执行前请备份: mysqldump -uroot -padmin123 egg_esports egg_match > backup_match_$(date +%Y%m%d).sql

-- 1) 存量 http 批量改 https（幂等，重复执行无害）
UPDATE egg_match SET team_a_logo = REPLACE(team_a_logo, 'http://', 'https://') WHERE team_a_logo LIKE 'http://%';
UPDATE egg_match SET team_b_logo = REPLACE(team_b_logo, 'http://', 'https://') WHERE team_b_logo LIKE 'http://%';

-- 2) 可选：/uploads/demo 头像若 Nginx 未托管 uploads，会 404；设为 null 则前端回退到首字母占位
-- 若希望 demo 也显示占位图而非 404，直接执行下两行（不影响真实赛事）：
-- UPDATE egg_match SET team_a_logo = NULL WHERE team_a_logo LIKE '/uploads/%';
-- UPDATE egg_match SET team_b_logo = NULL WHERE team_a_logo LIKE '/uploads/%' OR team_b_logo LIKE '/uploads/%';

-- 3) 验证
SELECT COUNT(*) AS http_a_remain FROM egg_match WHERE team_a_logo LIKE 'http://%';
SELECT COUNT(*) AS http_b_remain FROM egg_match WHERE team_b_logo LIKE 'http://%';
SELECT id, team_a, LEFT(team_a_logo,80) AS a_logo, team_b, LEFT(team_b_logo,80) AS b_logo FROM egg_match WHERE game_id=1 AND team_a != 'TBD' ORDER BY match_time DESC LIMIT 5;
