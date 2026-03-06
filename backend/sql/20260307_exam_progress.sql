-- Exam progress and heartbeat tables
-- Run this once on database `exam_system`.

CREATE TABLE IF NOT EXISTS exam_answer_drafts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  question_id BIGINT NOT NULL,
  user_answer VARCHAR(1000) NOT NULL,
  marked_flag TINYINT NOT NULL DEFAULT 0,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_exam_answer_draft_exam_student_question (exam_id, student_id, question_id),
  KEY idx_draft_exam_student (exam_id, student_id),
  KEY idx_draft_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS exam_heartbeats (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  exam_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  answered_count INT NOT NULL DEFAULT 0,
  total_count INT NOT NULL DEFAULT 0,
  time_left_seconds INT NOT NULL DEFAULT 0,
  last_active_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_exam_heartbeat_exam_student (exam_id, student_id),
  KEY idx_exam_heartbeat_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
