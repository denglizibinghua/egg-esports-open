-- v20: 公告弹窗类型 + 押注截止时间
ALTER TABLE egg_announcement ADD COLUMN display_type INT DEFAULT 1 COMMENT '展示类型: 1=横幅 2=弹窗';
ALTER TABLE egg_match ADD COLUMN bet_deadline DATETIME NULL COMMENT '押注截止时间';
