-- Fix for legacy sys_user schema mismatch.
-- This script will rebuild sys_user table to match backend entity fields.
-- WARNING: it will clear existing sys_user data.

DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
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

INSERT INTO sys_user (username, display_name, password, role, status, create_time, update_time)
VALUES
('admin', '平台管理员', '123456', 'ADMIN', 1, NOW(), NOW()),
('teacher', '默认教师', '123456', 'TEACHER', 1, NOW(), NOW());
