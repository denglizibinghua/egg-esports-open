-- 公告表
CREATE TABLE IF NOT EXISTS `egg_announcement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
  `content` TEXT COMMENT '公告内容',
  `game_id` BIGINT DEFAULT NULL COMMENT '关联游戏ID，NULL=全站公告',
  `status` TINYINT DEFAULT 1 COMMENT '0=下架 1=上架',
  `sort_order` INT DEFAULT 0 COMMENT '排序',
  `start_time` DATETIME DEFAULT NULL COMMENT '展示开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '展示结束时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';