-- 消费回执和补偿任务共用唯一事件键，使落库成功和死信处理互斥。
CREATE TABLE IF NOT EXISTS activity_admission_receipt (
    event_id VARCHAR(80) NOT NULL PRIMARY KEY,
    kind VARCHAR(16) NOT NULL,
    payload LONGTEXT NULL,
    activity_id INT NULL,
    student_id VARCHAR(255) NULL,
    reservation_token VARCHAR(36) NULL,
    next_attempt_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admission_receipt_due (completed_at, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
