CREATE TABLE IF NOT EXISTS `student_notice_read` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_id` bigint NOT NULL,
  `notice_id` varchar(128) NOT NULL,
  `read_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_notice` (`student_id`, `notice_id`),
  KEY `idx_student_read_at` (`student_id`, `read_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
