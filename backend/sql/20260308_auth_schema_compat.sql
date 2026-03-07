-- Compatibility patch for legacy auth/register schema.
-- Works on older MySQL/MariaDB versions without `ADD COLUMN IF NOT EXISTS`.

CREATE TABLE IF NOT EXISTS class_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  student_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

SET @db = DATABASE();

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'display_name';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE sys_user ADD COLUMN display_name VARCHAR(64) NULL AFTER username',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'status';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE sys_user ADD COLUMN status INT NOT NULL DEFAULT 1 AFTER role',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'create_time';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE sys_user ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'update_time';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE sys_user ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'class_info' AND COLUMN_NAME = 'student_count';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE class_info ADD COLUMN student_count INT DEFAULT 0',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT COUNT(*) INTO @exists_col
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'class_info' AND COLUMN_NAME = 'create_time';
SET @sql = IF(@exists_col = 0,
  'ALTER TABLE class_info ADD COLUMN create_time DATETIME DEFAULT CURRENT_TIMESTAMP',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE sys_user
SET display_name = COALESCE(NULLIF(real_name, ''), username)
WHERE display_name IS NULL OR display_name = '';
