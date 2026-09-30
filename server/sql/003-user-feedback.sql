-- Execute after 001-init.sql and 002-site-settings.sql.
CREATE TABLE IF NOT EXISTS user_feedback (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 title VARCHAR(120) NOT NULL,
 content VARCHAR(4000) NOT NULL,
 contact VARCHAR(120) NULL,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 INDEX idx_user_feedback_created (created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
