-- Bind exam to course dimension (optional).
SET @db = DATABASE();
SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'exam' AND COLUMN_NAME = 'course_id';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE exam ADD COLUMN course_id BIGINT NULL AFTER title',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Make sure at least one active course exists for student course home.
INSERT INTO courses (title, description, status, created_at, updated_at)
SELECT '通用课程', '默认课程（用于承接未归类考试）', 'ACTIVE', NOW(6), NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM courses);

SET @default_course_id = (SELECT id FROM courses ORDER BY id ASC LIMIT 1);
UPDATE exam
SET course_id = @default_course_id
WHERE course_id IS NULL;
