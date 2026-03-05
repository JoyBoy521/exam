/*
 Navicat Premium Dump SQL

 Source Server         : 2
 Source Server Type    : MySQL
 Source Server Version : 50744 (5.7.44-log)
 Source Host           : localhost:3306
 Source Schema         : exam_system

 Target Server Type    : MySQL
 Target Server Version : 50744 (5.7.44-log)
 File Encoding         : 65001

 Date: 05/03/2026 22:20:08
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for anti_cheat_rules
-- ----------------------------
DROP TABLE IF EXISTS `anti_cheat_rules`;
CREATE TABLE `anti_cheat_rules`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_no` int(11) NOT NULL,
  `page_blur_weight` int(11) NOT NULL,
  `window_switch_weight` int(11) NOT NULL,
  `network_disconnect_weight` int(11) NOT NULL,
  `copy_paste_weight` int(11) NOT NULL,
  `other_weight` int(11) NOT NULL,
  `duration_step_seconds` int(11) NOT NULL,
  `medium_risk_threshold` int(11) NOT NULL,
  `high_risk_threshold` int(11) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of anti_cheat_rules
-- ----------------------------
INSERT INTO `anti_cheat_rules` VALUES (1, 'default', 1, 5, 10, 8, 20, 3, 15, 40, 70, '2026-02-27 00:14:08.265429');

-- ----------------------------
-- Table structure for audit_logs
-- ----------------------------
DROP TABLE IF EXISTS `audit_logs`;
CREATE TABLE `audit_logs`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NULL DEFAULT NULL,
  `action` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_id` bigint(20) NULL DEFAULT NULL,
  `detail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_audit_logs_user`(`user_id`) USING BTREE,
  CONSTRAINT `fk_audit_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of audit_logs
-- ----------------------------
INSERT INTO `audit_logs` VALUES (1, 6, 'LOGIN', 'USER', 6, 'User login', '2026-02-26 16:14:26.353974');
INSERT INTO `audit_logs` VALUES (2, 6, 'EXAM_STATUS_UPDATED', 'EXAM', 2, 'published', '2026-02-26 16:14:54.651209');
INSERT INTO `audit_logs` VALUES (3, 7, 'LOGIN', 'USER', 7, 'User login', '2026-02-27 15:44:53.911807');
INSERT INTO `audit_logs` VALUES (4, 9, 'REGISTER', 'USER', 9, 'New student registered', '2026-02-28 15:09:27.465769');
INSERT INTO `audit_logs` VALUES (5, 6, 'LOGIN', 'USER', 6, 'User login', '2026-02-28 15:30:58.601521');
INSERT INTO `audit_logs` VALUES (6, 6, 'LOGIN', 'USER', 6, 'User login', '2026-02-28 15:30:59.687450');
INSERT INTO `audit_logs` VALUES (7, 6, 'QUESTION_CREATED', 'QUESTION', 4, '1', '2026-02-28 16:05:34.796531');

-- ----------------------------
-- Table structure for cheat_events
-- ----------------------------
DROP TABLE IF EXISTS `cheat_events`;
CREATE TABLE `cheat_events`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `duration_seconds` int(11) NOT NULL,
  `happened_at` datetime(6) NOT NULL,
  `detail` varchar(256) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_cheat_exam`(`exam_id`) USING BTREE,
  INDEX `idx_cheat_exam_student`(`exam_id`, `student_id`) USING BTREE,
  INDEX `idx_cheat_happened_at`(`happened_at`) USING BTREE,
  INDEX `fk_cheat_events_student`(`student_id`) USING BTREE,
  CONSTRAINT `fk_cheat_events_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_cheat_events_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cheat_events
-- ----------------------------
INSERT INTO `cheat_events` VALUES (1, 1, 3, 'WINDOW_SWITCH', 15, '2026-02-26 23:14:08.269098', 'Alt+Tab');
INSERT INTO `cheat_events` VALUES (2, 1, 3, 'PAGE_BLUR', 10, '2026-02-26 23:19:08.269098', 'lost focus');

-- ----------------------------
-- Table structure for class_info
-- ----------------------------
DROP TABLE IF EXISTS `class_info`;
CREATE TABLE `class_info`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '班级ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '班级名称',
  `student_count` int(11) NULL DEFAULT 0 COMMENT '学生人数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '班级信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of class_info
-- ----------------------------
INSERT INTO `class_info` VALUES (1, '2026级软件工程1班', 45, '2026-03-05 00:20:07');
INSERT INTO `class_info` VALUES (2, '2026级软件工程2班', 42, '2026-03-05 00:20:07');
INSERT INTO `class_info` VALUES (3, '计算机网络选修课群', 108, '2026-03-05 00:20:07');
INSERT INTO `class_info` VALUES (4, '1', 0, '2026-03-05 00:26:31');

-- ----------------------------
-- Table structure for classrooms
-- ----------------------------
DROP TABLE IF EXISTS `classrooms`;
CREATE TABLE `classrooms`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK7yey8vi13fi8vuxrarkj0ortc`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of classrooms
-- ----------------------------
INSERT INTO `classrooms` VALUES (1, '2026-02-28 15:08:32.827159', '软件1班');
INSERT INTO `classrooms` VALUES (2, '2026-02-28 15:08:32.834065', '软件2班');

-- ----------------------------
-- Table structure for course_classrooms
-- ----------------------------
DROP TABLE IF EXISTS `course_classrooms`;
CREATE TABLE `course_classrooms`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `classroom_id` bigint(20) NOT NULL,
  `course_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_classroom_course_class`(`course_id`, `classroom_id`) USING BTREE,
  INDEX `idx_course_classroom_course`(`course_id`) USING BTREE,
  INDEX `idx_course_classroom_classroom`(`classroom_id`) USING BTREE,
  CONSTRAINT `FKq43uy22q30inlgfbbbtj6twi` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKt3sxn2eleyd3ejflgh5c8s49o` FOREIGN KEY (`classroom_id`) REFERENCES `classrooms` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_classrooms
-- ----------------------------
INSERT INTO `course_classrooms` VALUES (1, 1, 1);
INSERT INTO `course_classrooms` VALUES (2, 2, 1);

-- ----------------------------
-- Table structure for course_members
-- ----------------------------
DROP TABLE IF EXISTS `course_members`;
CREATE TABLE `course_members`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) NOT NULL,
  `member_role` enum('STUDENT','TEACHER') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_course_member_course_user`(`course_id`, `user_id`) USING BTREE,
  INDEX `idx_course_member_course`(`course_id`) USING BTREE,
  INDEX `idx_course_member_user`(`user_id`) USING BTREE,
  INDEX `idx_course_member_role`(`member_role`) USING BTREE,
  CONSTRAINT `FK1sao50jhghhqxqqe5e3g4k18u` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKfyhomwd02tu69x4n4qy6d3n7v` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of course_members
-- ----------------------------
INSERT INTO `course_members` VALUES (1, '2026-02-28 15:08:32.860918', 'TEACHER', 1, 6);
INSERT INTO `course_members` VALUES (2, '2026-02-28 15:08:32.868553', 'STUDENT', 1, 7);
INSERT INTO `course_members` VALUES (3, '2026-02-28 15:08:32.876049', 'STUDENT', 1, 8);
INSERT INTO `course_members` VALUES (4, '2026-02-28 15:21:49.197006', 'STUDENT', 1, 9);

-- ----------------------------
-- Table structure for courses
-- ----------------------------
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `cover_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `status` enum('ACTIVE','ARCHIVED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of courses
-- ----------------------------
INSERT INTO `courses` VALUES (1, NULL, '2026-02-28 15:08:32.780849', '课程内含章节学习、练习、考试与讨论。', 'ACTIVE', 'Java 程序设计', '2026-02-28 15:08:32.780849');

-- ----------------------------
-- Table structure for discussion_folders
-- ----------------------------
DROP TABLE IF EXISTS `discussion_folders`;
CREATE TABLE `discussion_folders`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `created_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKshlqn0md5ir5qq7ue51vicarx`(`course_id`) USING BTREE,
  CONSTRAINT `FKshlqn0md5ir5qq7ue51vicarx` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of discussion_folders
-- ----------------------------

-- ----------------------------
-- Table structure for discussion_replies
-- ----------------------------
DROP TABLE IF EXISTS `discussion_replies`;
CREATE TABLE `discussion_replies`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `author_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `topic_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK7wjt86159hsa5ca950lw2yrfq`(`topic_id`) USING BTREE,
  CONSTRAINT `FK7wjt86159hsa5ca950lw2yrfq` FOREIGN KEY (`topic_id`) REFERENCES `discussion_topics` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of discussion_replies
-- ----------------------------

-- ----------------------------
-- Table structure for discussion_topics
-- ----------------------------
DROP TABLE IF EXISTS `discussion_topics`;
CREATE TABLE `discussion_topics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `author_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `pinned` bit(1) NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint(20) NOT NULL,
  `folder_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKmrh7fo2msgbpnfflk20ilcqa9`(`course_id`) USING BTREE,
  INDEX `FKayeh40bwgj1k6e2mokvw32t97`(`folder_id`) USING BTREE,
  CONSTRAINT `FKayeh40bwgj1k6e2mokvw32t97` FOREIGN KEY (`folder_id`) REFERENCES `discussion_folders` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKmrh7fo2msgbpnfflk20ilcqa9` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of discussion_topics
-- ----------------------------
INSERT INTO `discussion_topics` VALUES (1, 'teacher1', '请同学们把本周最困惑的知识点写在这里，老师统一答疑。', '2026-02-28 15:08:32.923173', b'1', '本周学习疑难点集中讨论', 1, NULL);
INSERT INTO `discussion_topics` VALUES (2, 'student1', '欢迎分享错题经验和复盘方法。', '2026-02-28 15:08:32.927703', b'0', '你认为最容易错的题型是？', 1, NULL);

-- ----------------------------
-- Table structure for exam
-- ----------------------------
DROP TABLE IF EXISTS `exam`;
CREATE TABLE `exam`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '考试名称',
  `class_id` bigint(20) NULL DEFAULT NULL COMMENT '发放班级ID',
  `paper_ids` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '关联的试卷IDs(逗号分隔,支持多卷防作弊)',
  `start_time` datetime NOT NULL COMMENT '考试开始时间',
  `end_time` datetime NOT NULL COMMENT '考试结束时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'NOT_STARTED' COMMENT '状态: NOT_STARTED, ONGOING, FINISHED',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '考试安排表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam
-- ----------------------------
INSERT INTO `exam` VALUES (1, '2026级软件工程期中统一考试', NULL, '1', '2026-03-04 23:20:07', '2026-03-05 01:20:07', 'ONGOING', '2026-03-05 00:20:07');
INSERT INTO `exam` VALUES (2, '计算机网络随堂小测', NULL, '2', '2026-03-06 00:20:07', '2026-03-07 00:20:07', 'NOT_STARTED', '2026-03-05 00:20:07');
INSERT INTO `exam` VALUES (3, 'Java 基础期初摸底考', NULL, '1', '2026-03-03 00:20:07', '2026-03-04 00:20:07', 'FINISHED', '2026-03-05 00:20:07');

-- ----------------------------
-- Table structure for exam_answer_drafts
-- ----------------------------
DROP TABLE IF EXISTS `exam_answer_drafts`;
CREATE TABLE `exam_answer_drafts`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `user_answer` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `marked_flag` tinyint(1) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_answer_draft_exam_student_question`(`exam_id`, `student_id`, `question_id`) USING BTREE,
  INDEX `idx_draft_exam_student`(`exam_id`, `student_id`) USING BTREE,
  INDEX `idx_draft_updated_at`(`updated_at`) USING BTREE,
  INDEX `fk_exam_answer_drafts_student`(`student_id`) USING BTREE,
  INDEX `fk_exam_answer_drafts_question`(`question_id`) USING BTREE,
  CONSTRAINT `fk_exam_answer_drafts_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_answer_drafts_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_answer_drafts_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_answer_drafts
-- ----------------------------

-- ----------------------------
-- Table structure for exam_enrollments
-- ----------------------------
DROP TABLE IF EXISTS `exam_enrollments`;
CREATE TABLE `exam_enrollments`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `makeup_eligible` tinyint(1) NOT NULL,
  `extra_minutes` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_enrollment_exam_student`(`exam_id`, `student_id`) USING BTREE,
  INDEX `idx_exam_enrollment_exam`(`exam_id`) USING BTREE,
  INDEX `idx_exam_enrollment_student`(`student_id`) USING BTREE,
  CONSTRAINT `fk_exam_enrollments_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_enrollments_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_enrollments
-- ----------------------------

-- ----------------------------
-- Table structure for exam_questions
-- ----------------------------
DROP TABLE IF EXISTS `exam_questions`;
CREATE TABLE `exam_questions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `sort_order` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_exam_questions_exam`(`exam_id`) USING BTREE,
  INDEX `fk_exam_questions_question`(`question_id`) USING BTREE,
  CONSTRAINT `fk_exam_questions_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_exam_questions_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_questions
-- ----------------------------
INSERT INTO `exam_questions` VALUES (1, 1, 1, 1);
INSERT INTO `exam_questions` VALUES (2, 1, 2, 2);
INSERT INTO `exam_questions` VALUES (3, 1, 3, 3);
INSERT INTO `exam_questions` VALUES (4, 3, 2, 1);
INSERT INTO `exam_questions` VALUES (5, 3, 1, 2);
INSERT INTO `exam_questions` VALUES (6, 3, 3, 3);

-- ----------------------------
-- Table structure for exam_record
-- ----------------------------
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL COMMENT '考试ID',
  `user_id` bigint(20) NOT NULL COMMENT '答题人(学生)ID',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'DOING' COMMENT '状态: DOING(答题中), SUBMITTED(已交卷), GRADED(已阅卷)',
  `objective_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '客观题自动得分',
  `subjective_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '主观题人工得分',
  `total_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '总分',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始作答时间',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '交卷时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_exam_user`(`exam_id`, `user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生考试记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_record
-- ----------------------------
INSERT INTO `exam_record` VALUES (1, 1, 1, 'SUBMITTED', 60.0, 0.0, 60.0, '2026-03-04 23:30:07', '2026-03-05 00:10:07');
INSERT INTO `exam_record` VALUES (2, 1, 2, 'GRADED', 55.0, 30.0, 85.0, '2026-03-04 23:25:07', '2026-03-05 00:05:07');
INSERT INTO `exam_record` VALUES (3, 1, 3, 'SUBMITTED', 70.0, 0.0, 70.0, '2026-03-04 23:35:07', '2026-03-05 00:15:07');

-- ----------------------------
-- Table structure for exam_record_answer
-- ----------------------------
DROP TABLE IF EXISTS `exam_record_answer`;
CREATE TABLE `exam_record_answer`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `record_id` bigint(20) NOT NULL COMMENT '考试记录表(Exam_Record)的ID',
  `question_id` bigint(20) NOT NULL COMMENT '题目ID',
  `user_answer` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '学生的作答内容',
  `is_correct` tinyint(1) NULL DEFAULT NULL COMMENT '是否正确: 1对, 0错 (客观题专用)',
  `score` decimal(4, 1) NULL DEFAULT 0.0 COMMENT '该题最终得分',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_record_id`(`record_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生答题明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exam_record_answer
-- ----------------------------

-- ----------------------------
-- Table structure for exams
-- ----------------------------
DROP TABLE IF EXISTS `exams`;
CREATE TABLE `exams`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_at` datetime(6) NULL DEFAULT NULL,
  `end_at` datetime(6) NULL DEFAULT NULL,
  `makeup_end_at` datetime(6) NULL DEFAULT NULL,
  `course_id` bigint(20) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKr1qm93flajdaclug2fg8i7bcg`(`course_id`) USING BTREE,
  CONSTRAINT `FKr1qm93flajdaclug2fg8i7bcg` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exams
-- ----------------------------
INSERT INTO `exams` VALUES (1, 'Java 基础测验', 'published', '2026-02-24 16:14:20.957790', '2026-03-05 16:14:20.957790', '2026-03-12 16:14:20.957790', 1);
INSERT INTO `exams` VALUES (2, 'Spring Boot 周测', 'published', '2026-02-25 16:14:20.957790', '2026-03-08 16:14:20.957790', '2026-03-15 16:14:20.957790', 1);
INSERT INTO `exams` VALUES (3, '1', 'draft', NULL, NULL, NULL, 1);

-- ----------------------------
-- Table structure for makeup_requests
-- ----------------------------
DROP TABLE IF EXISTS `makeup_requests`;
CREATE TABLE `makeup_requests`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `teacher_comment` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `reviewed_at` datetime(6) NULL DEFAULT NULL,
  `reviewer_id` bigint(20) NULL DEFAULT NULL,
  `approved_extra_minutes` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_makeup_requests_exam`(`exam_id`) USING BTREE,
  INDEX `fk_makeup_requests_student`(`student_id`) USING BTREE,
  INDEX `fk_makeup_requests_reviewer`(`reviewer_id`) USING BTREE,
  CONSTRAINT `fk_makeup_requests_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_makeup_requests_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_makeup_requests_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of makeup_requests
-- ----------------------------

-- ----------------------------
-- Table structure for paper
-- ----------------------------
DROP TABLE IF EXISTS `paper`;
CREATE TABLE `paper`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '试卷名称',
  `total_score` decimal(5, 1) NULL DEFAULT 0.0 COMMENT '试卷总分',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '组卷人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '试卷表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of paper
-- ----------------------------
INSERT INTO `paper` VALUES (1, '2026级软件工程期中统一考试卷', 100.0, 'teacher', '2026-03-05 00:20:07');
INSERT INTO `paper` VALUES (2, '计算机网络随堂小测(随机)', 50.0, 'teacher', '2026-03-05 00:20:07');

-- ----------------------------
-- Table structure for paper_question
-- ----------------------------
DROP TABLE IF EXISTS `paper_question`;
CREATE TABLE `paper_question`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `paper_id` bigint(20) NOT NULL COMMENT '试卷ID',
  `question_id` bigint(20) NOT NULL COMMENT '题目ID',
  `score` decimal(4, 1) NOT NULL DEFAULT 0.0 COMMENT '该题在该试卷中的分值',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '题号排序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_paper_id`(`paper_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '试卷题目关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of paper_question
-- ----------------------------
INSERT INTO `paper_question` VALUES (1, 1, 1, 5.0, 1);
INSERT INTO `paper_question` VALUES (2, 1, 3, 5.0, 2);
INSERT INTO `paper_question` VALUES (3, 1, 2, 10.0, 3);

-- ----------------------------
-- Table structure for practices
-- ----------------------------
DROP TABLE IF EXISTS `practices`;
CREATE TABLE `practices`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `question_count` int(11) NOT NULL,
  `status` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `course_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKgknublcd0bmd60x0fxlddu35v`(`course_id`) USING BTREE,
  CONSTRAINT `FKgknublcd0bmd60x0fxlddu35v` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of practices
-- ----------------------------
INSERT INTO `practices` VALUES (1, '2026-02-28 15:08:32.910377', '围绕变量、循环与方法的基础练习。', 10, 'published', 'Java 语法练习一', 1);
INSERT INTO `practices` VALUES (2, '2026-02-28 15:08:32.914995', '聚焦核心注解与工程结构理解。', 8, 'published', 'Spring Boot 注解练习', 1);

-- ----------------------------
-- Table structure for question
-- ----------------------------
DROP TABLE IF EXISTS `question`;
CREATE TABLE `question`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '题型: SINGLE_CHOICE, TRUE_FALSE, SHORT_ANSWER',
  `stem` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '题干(支持存富文本/HTML)',
  `options` json NULL COMMENT '选项，使用JSON数组存储，如 [\"A. 1\", \"B. 2\"]',
  `answer` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标准答案',
  `analysis` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '题目解析',
  `difficulty` tinyint(4) NULL DEFAULT 2 COMMENT '难度: 1简单, 2中等, 3困难',
  `knowledge_points` json NULL COMMENT '知识点集合(JSON数组)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '题库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of question
-- ----------------------------
INSERT INTO `question` VALUES (1, 'SINGLE_CHOICE', 'Java 中如何开启一个线程？', '[\"A. 继承 Thread 类\", \"B. 实现 Runnable 接口\", \"C. 使用 Callable 和 Future\", \"D. 以上都可以\"]', 'D', '基础多线程知识', 1, '[\"Java基础\", \"多线程\"]', '2026-03-05 00:20:07');
INSERT INTO `question` VALUES (2, 'SHORT_ANSWER', '请简述 Spring Boot 中 @SpringBootApplication 注解的核心作用。', NULL, '它是一个复合注解，包含 @SpringBootConfiguration 等。', 'Spring Boot 启动核心', 2, '[\"Spring Boot\"]', '2026-03-05 00:20:07');
INSERT INTO `question` VALUES (3, 'TRUE_FALSE', 'HTTP 是无状态协议。', '[\"正确\", \"错误\"]', '正确', '网络基础', 1, '[\"计算机网络\"]', '2026-03-05 00:20:07');
INSERT INTO `question` VALUES (4, 'SINGLE_CHOICE', '13', '[\"13\", \"2\", \"3\", \"1\"]', 'A', NULL, 2, '[\"计算机网络\"]', '2026-03-05 00:30:40');

-- ----------------------------
-- Table structure for questions
-- ----------------------------
DROP TABLE IF EXISTS `questions`;
CREATE TABLE `questions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `type` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stem` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `options_json` tinytext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `answer` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `score` int(11) NOT NULL,
  `analysis_text` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `knowledge_point` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of questions
-- ----------------------------
INSERT INTO `questions` VALUES (1, 'SINGLE', 'Java 中声明常量用哪个关键字？', '[\"var\",\"final\",\"let\",\"const\"]', 'final', 5, '常量在定义后不可再次赋值，Java 使用 final 关键字实现。', 'Java 基础语法');
INSERT INTO `questions` VALUES (2, 'JUDGE', 'HTTP 是无状态协议。', '[\"true\",\"false\"]', 'true', 5, 'HTTP 请求之间默认不保存会话状态，需要 Cookie/Session/Token 才能保持登录态。', 'HTTP 协议');
INSERT INTO `questions` VALUES (3, 'MULTIPLE', '以下哪些是 Spring Boot 常用注解？', '[\"@SpringBootApplication\",\"@RestController\",\"@Select\",\"@Autowired\"]', '@Autowired,@RestController,@SpringBootApplication', 10, '@Select 属于 MyBatis 注解，其他三个属于 Spring 生态常用注解。', 'Spring Boot 注解体系');
INSERT INTO `questions` VALUES (4, 'JUDGE', '1', '[\"选项A\",\"选项B\",\"选项C\",\"选项D\"]', '错误', 5, NULL, NULL);

-- ----------------------------
-- Table structure for student
-- ----------------------------
DROP TABLE IF EXISTS `student`;
CREATE TABLE `student`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `student_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学号(登录账号)',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '姓名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '123456' COMMENT '密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联系电话',
  `class_id` bigint(20) NULL DEFAULT NULL COMMENT '所属班级ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_student_no`(`student_no`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of student
-- ----------------------------
INSERT INTO `student` VALUES (1, '20260001', '张三', '123456', '13800138000', 1, '2026-03-05 00:20:07');
INSERT INTO `student` VALUES (2, '20260002', '李四', '123456', '13900139000', 1, '2026-03-05 00:20:07');
INSERT INTO `student` VALUES (3, '20260003', '王五', '123456', '13700137000', 1, '2026-03-05 00:20:07');

-- ----------------------------
-- Table structure for submission_items
-- ----------------------------
DROP TABLE IF EXISTS `submission_items`;
CREATE TABLE `submission_items`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `submission_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `user_answer` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `correct_flag` tinyint(1) NOT NULL,
  `score_awarded` int(11) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_submission_items_submission`(`submission_id`) USING BTREE,
  INDEX `fk_submission_items_question`(`question_id`) USING BTREE,
  CONSTRAINT `fk_submission_items_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_submission_items_submission` FOREIGN KEY (`submission_id`) REFERENCES `submissions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of submission_items
-- ----------------------------

-- ----------------------------
-- Table structure for submissions
-- ----------------------------
DROP TABLE IF EXISTS `submissions`;
CREATE TABLE `submissions`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `score` int(11) NOT NULL,
  `submitted_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_submission_exam_student`(`exam_id`, `student_id`) USING BTREE,
  INDEX `idx_submission_exam`(`exam_id`) USING BTREE,
  INDEX `idx_submission_student`(`student_id`) USING BTREE,
  CONSTRAINT `fk_submissions_exam` FOREIGN KEY (`exam_id`) REFERENCES `exams` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_submissions_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of submissions
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名/学号/工号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码(后续可存BCrypt密文)',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '真实姓名',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT, TEACHER, ADMIN',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '123456', '超级管理员', 'ADMIN', '2026-03-04 22:12:48');
INSERT INTO `sys_user` VALUES (2, 'teacher', '123456', '王老师', 'TEACHER', '2026-03-04 22:12:48');
INSERT INTO `sys_user` VALUES (3, 'student', '123456', '张三', 'STUDENT', '2026-03-04 22:12:48');

-- ----------------------------
-- Table structure for teacher_alert_follow_ups
-- ----------------------------
DROP TABLE IF EXISTS `teacher_alert_follow_ups`;
CREATE TABLE `teacher_alert_follow_ups`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `assignee_teacher_id` bigint(20) NULL DEFAULT NULL,
  `handle_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `handle_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_alert_follow_up_student`(`student_id`) USING BTREE,
  INDEX `fk_teacher_alert_follow_ups_assignee`(`assignee_teacher_id`) USING BTREE,
  CONSTRAINT `fk_teacher_alert_follow_ups_assignee` FOREIGN KEY (`assignee_teacher_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_teacher_alert_follow_ups_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_alert_follow_ups
-- ----------------------------

-- ----------------------------
-- Table structure for teacher_kp_action_follow_ups
-- ----------------------------
DROP TABLE IF EXISTS `teacher_kp_action_follow_ups`;
CREATE TABLE `teacher_kp_action_follow_ups`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `knowledge_point` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_kp_action_follow_up_point`(`knowledge_point`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_kp_action_follow_ups
-- ----------------------------

-- ----------------------------
-- Table structure for teacher_kp_action_records
-- ----------------------------
DROP TABLE IF EXISTS `teacher_kp_action_records`;
CREATE TABLE `teacher_kp_action_records`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `knowledge_point` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `previous_execution_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `new_execution_status` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `execution_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `operator_id` bigint(20) NULL DEFAULT NULL,
  `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of teacher_kp_action_records
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `class_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (5, 'admin', '$2a$10$i9afsvvAstPuswj3EpMab.27KllGzbHRReV2gnh7ECXeCYHY0EkrS', 'ADMIN', NULL);
INSERT INTO `users` VALUES (6, 'teacher1', '$2a$10$UOjZ5HZ2EfZPXx5633ICIeKtP.ZBcST8k0k5ncxoJT/mwEhKsp98S', 'TEACHER', NULL);
INSERT INTO `users` VALUES (7, 'student1', '$2a$10$CMD0ZagyHJhYJsWVXmtBfei3atpJ98idR9LYyUtMCk93LBLfx7T6S', 'STUDENT', '软件1班');
INSERT INTO `users` VALUES (8, 'student2', '$2a$10$nqQhH1KM3v8r6zbpODEmD.2IqZbjr5OLXiqeDOSCgL4wI7EbbNYIy', 'STUDENT', '软件2班');
INSERT INTO `users` VALUES (9, 'demo1772291367', '$2a$10$4itxhXuoCUUtElcXAzGeTOTohlxByfQ7ucaw9NYkma4jMrtaDwyVi', 'STUDENT', '软件9班');

SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------
-- App tables for current backend (exam + student model)
-- ----------------------------
DROP TABLE IF EXISTS `student_wrong_book`;
CREATE TABLE `student_wrong_book` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `student_id` bigint(20) NOT NULL,
  `question_id` bigint(20) NOT NULL,
  `error_type` varchar(64) DEFAULT NULL,
  `notes` varchar(1000) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wrong_book_student_question` (`student_id`,`question_id`),
  KEY `idx_wrong_book_student` (`student_id`),
  KEY `idx_wrong_book_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `parallel_group`;
CREATE TABLE `parallel_group` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL,
  `paper_ids` json NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `parallel_assignment`;
CREATE TABLE `parallel_assignment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `group_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `student_name` varchar(64) DEFAULT NULL,
  `paper_id` bigint(20) NOT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parallel_group_student` (`group_id`,`student_id`),
  KEY `idx_parallel_group` (`group_id`),
  KEY `idx_parallel_student` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `exam_cheat_event`;
CREATE TABLE `exam_cheat_event` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `type` varchar(32) NOT NULL,
  `duration_seconds` int(11) NOT NULL DEFAULT 0,
  `detail` varchar(256) DEFAULT NULL,
  `happened_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_exam_cheat_exam` (`exam_id`),
  KEY `idx_exam_cheat_exam_student` (`exam_id`,`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP TABLE IF EXISTS `student_makeup_request`;
CREATE TABLE `student_makeup_request` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `exam_id` bigint(20) NOT NULL,
  `student_id` bigint(20) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'PENDING',
  `teacher_comment` varchar(500) DEFAULT NULL,
  `approved_extra_minutes` int(11) DEFAULT NULL,
  `requested_at` datetime(6) NOT NULL,
  `reviewed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_makeup_exam` (`exam_id`),
  KEY `idx_makeup_student` (`student_id`),
  KEY `idx_makeup_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
