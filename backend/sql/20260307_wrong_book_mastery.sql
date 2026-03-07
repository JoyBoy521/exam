ALTER TABLE student_wrong_book
  ADD COLUMN practice_count int NOT NULL DEFAULT 0 COMMENT '练习次数' AFTER notes,
  ADD COLUMN correct_count int NOT NULL DEFAULT 0 COMMENT '答对次数(客观题)' AFTER practice_count,
  ADD COLUMN mastery_level varchar(32) NOT NULL DEFAULT 'UNPRACTICED' COMMENT '掌握度' AFTER correct_count,
  ADD COLUMN last_practice_time datetime NULL COMMENT '最近练习时间' AFTER mastery_level;
