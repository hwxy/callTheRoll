-- Run manually against a dedicated database. Never run against an unrelated schema.
CREATE DATABASE IF NOT EXISTS dianming CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE dianming;
CREATE TABLE app_user (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(80) NOT NULL,
 student_no VARCHAR(64) NULL,
 phone VARCHAR(20) NULL,
 password_hash VARCHAR(100) NOT NULL,
 role VARCHAR(16) NOT NULL,
 owner_teacher_id BIGINT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE,
 auth_version INT NOT NULL DEFAULT 0,
 version INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_user_owner FOREIGN KEY (owner_teacher_id) REFERENCES app_user(id),
 CONSTRAINT ck_user_role CHECK (role IN ('ADMIN','TEACHER','STUDENT')),
 CONSTRAINT ck_user_login CHECK (student_no IS NOT NULL OR phone IS NOT NULL),
 CONSTRAINT ck_student_owner CHECK (role <> 'STUDENT' OR owner_teacher_id IS NOT NULL)
);
-- A single namespace prevents cross-column login collisions.
CREATE TABLE login_alias (
 alias VARCHAR(64) COLLATE utf8mb4_bin PRIMARY KEY,
 user_id BIGINT NOT NULL,
 FOREIGN KEY (user_id) REFERENCES app_user(id), INDEX idx_alias_user(user_id)
);
CREATE TABLE activity (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 name VARCHAR(100) NOT NULL,
 creator_id BIGINT NOT NULL,
 repeat_draw BOOLEAN NOT NULL DEFAULT FALSE,
 round_no INT NOT NULL DEFAULT 1,
 version INT NOT NULL DEFAULT 0,
 deleted INT NOT NULL DEFAULT 0,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (creator_id) REFERENCES app_user(id)
);
CREATE TABLE activity_teacher (
 activity_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 PRIMARY KEY (activity_id,teacher_id),
 FOREIGN KEY (activity_id) REFERENCES activity(id),
 FOREIGN KEY (teacher_id) REFERENCES app_user(id)
);
CREATE TABLE activity_student (
 activity_id BIGINT NOT NULL, student_id BIGINT NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 points INT NOT NULL DEFAULT 0,
 pet VARCHAR(10) NULL,
 PRIMARY KEY (activity_id,student_id),
 FOREIGN KEY (activity_id) REFERENCES activity(id),
 FOREIGN KEY (student_id) REFERENCES app_user(id),
 CHECK (points >= 0), CHECK (pet IS NULL OR pet IN ('cat','rabbit','dragon'))
);
CREATE TABLE draw_record (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 activity_id BIGINT NOT NULL, student_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 round_no INT NOT NULL, request_key VARCHAR(80) COLLATE utf8mb4_bin NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 resolved_by BIGINT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 resolved_at TIMESTAMP NULL,
 UNIQUE KEY uk_draw_request(activity_id,request_key),
 INDEX idx_draw_round(activity_id,round_no,student_id),
 INDEX idx_draw_status(activity_id,status),
 FOREIGN KEY (activity_id) REFERENCES activity(id),
 FOREIGN KEY (student_id) REFERENCES app_user(id),
 FOREIGN KEY (teacher_id) REFERENCES app_user(id),
 FOREIGN KEY (resolved_by) REFERENCES app_user(id),
 CHECK (status IN ('PENDING','AWARDED','SKIPPED'))
);
CREATE TABLE score_ledger (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 draw_id BIGINT NOT NULL UNIQUE, activity_id BIGINT NOT NULL,
 student_id BIGINT NOT NULL, teacher_id BIGINT NOT NULL,
 delta INT NOT NULL DEFAULT 1,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY (draw_id) REFERENCES draw_record(id),
 FOREIGN KEY (activity_id,student_id) REFERENCES activity_student(activity_id,student_id),
 FOREIGN KEY (teacher_id) REFERENCES app_user(id), CHECK (delta=1)
);
CREATE TABLE audit_log (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 actor_id BIGINT NOT NULL, action VARCHAR(64) NOT NULL,
 target_id BIGINT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_audit_actor(actor_id,created_at)
);
CREATE TABLE site_setting (
 setting_key VARCHAR(64) PRIMARY KEY,
 setting_value TEXT NOT NULL,
 updated_by BIGINT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 version INT NOT NULL DEFAULT 0,
 FOREIGN KEY (updated_by) REFERENCES app_user(id)
);
INSERT INTO site_setting(setting_key,setting_value) VALUES('about_us','');
