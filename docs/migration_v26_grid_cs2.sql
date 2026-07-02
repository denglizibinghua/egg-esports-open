-- Migration V26: GRID Open Access — CS2 游戏入口
INSERT INTO egg_game (id, name, sort_order, status) VALUES
    (4, 'CS2', 4, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);

SELECT * FROM egg_game ORDER BY sort_order;
