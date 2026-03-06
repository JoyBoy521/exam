-- System users for teacher/admin login.
-- Run this once on database `exam_system`.

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  display_name VARCHAR(64) NOT NULL,
  password VARCHAR(128) NOT NULL,
  role VARCHAR(16) NOT NULL,
  status INT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Initial accounts (legacy plain password: 123456, will auto-upgrade to bcrypt after login)
INSERT INTO sys_user (username, display_name, password, role, status, create_time, update_time)
SELECT 'admin', '平台管理员', '123456', 'ADMIN', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'admin');

INSERT INTO sys_user (username, display_name, password, role, status, create_time, update_time)
SELECT 'teacher', '默认教师', '123456', 'TEACHER', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'teacher');
