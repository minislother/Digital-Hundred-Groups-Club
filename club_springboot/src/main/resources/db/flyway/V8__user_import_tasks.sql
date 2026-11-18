-- user.user_id is already a NOT NULL primary key; do not add a redundant index.
CREATE TABLE IF NOT EXISTS user_import_guard (id INT PRIMARY KEY);
INSERT IGNORE INTO user_import_guard VALUES (1);
CREATE TABLE IF NOT EXISTS user_import_task (
 id VARCHAR(36) PRIMARY KEY, uploader VARCHAR(100) NOT NULL,
 file_path VARCHAR(1024) NOT NULL, status VARCHAR(24) NOT NULL,
 owner VARCHAR(36), lease_until DATETIME(6),
 processed BIGINT NOT NULL DEFAULT 0, succeeded BIGINT NOT NULL DEFAULT 0,
 duplicates BIGINT NOT NULL DEFAULT 0, invalid BIGINT NOT NULL DEFAULT 0,
 total BIGINT NULL, error_message VARCHAR(500),
 created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 INDEX idx_import_queue(status, created_at), INDEX idx_import_uploader(uploader, created_at)
);
CREATE TABLE IF NOT EXISTS user_import_error (
 task_id VARCHAR(36) NOT NULL, row_no BIGINT NOT NULL, reason VARCHAR(100) NOT NULL,
 PRIMARY KEY(task_id,row_no)
);
