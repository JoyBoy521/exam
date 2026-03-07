ALTER TABLE exam_record_answer
  ADD COLUMN teacher_comment varchar(500) NULL COMMENT '教师评语' AFTER score;
