-- Execute after 001-init.sql when upgrading an existing database.
CREATE TABLE IF NOT EXISTS site_setting (
 setting_key VARCHAR(64) PRIMARY KEY,
 setting_value TEXT NOT NULL,
 updated_by BIGINT NULL,
 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 version INT NOT NULL DEFAULT 0,
 FOREIGN KEY (updated_by) REFERENCES app_user(id)
);
INSERT IGNORE INTO site_setting(setting_key,setting_value) VALUES('about_us','');
