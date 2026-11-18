-- 停写并备份后手工执行一次；不自动修改生产数据库。
ALTER TABLE `user`
 MODIFY COLUMN user_name TEXT NULL,
 ADD COLUMN user_name_lookup CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD INDEX idx_user_name_lookup(user_name_lookup),
 MODIFY COLUMN email TEXT NULL,
 MODIFY COLUMN phone TEXT NULL,
 ADD COLUMN email_lookup CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD COLUMN phone_lookup CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
 ADD INDEX idx_user_email_lookup(email_lookup),
 ADD INDEX idx_user_phone_lookup(phone_lookup);
CREATE TABLE contact_migration_progress (
 migration_id VARCHAR(80) PRIMARY KEY,
 last_user_id VARCHAR(255) NOT NULL
);
