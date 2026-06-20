-- Migration V12: 多游戏支持（LoL + Dota2）
-- 1. egg_game 表添加 LoL 和 Dota2

INSERT INTO egg_game (id, name, sort_order, status) VALUES
    (2, '英雄联盟', 2, 1),
    (3, 'Dota 2', 3, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 确认数据
SELECT * FROM egg_game ORDER BY sort_order;
