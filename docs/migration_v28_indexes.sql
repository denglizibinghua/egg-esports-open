-- ============================================
-- v28: 押注与赛事表高频查询索引
-- 针对热点查询路径补建复合索引，避免全表扫描
-- ============================================

-- 押注表：用户查自己的下注记录（按 status 过滤），高频查询
-- 命中场景：BetQueryService.getMyBets(userId, status) 等
ALTER TABLE egg_bet
    ADD INDEX idx_bet_user_status (user_id, status);

-- 赛事表：按游戏 + 状态过滤的列表查询（首页/管理后台）
-- 命中场景：EggMatchService.getMatchList(status, gameId, ...)、getHotMatches
ALTER TABLE egg_match
    ADD INDEX idx_match_game_status (game_id, status);
