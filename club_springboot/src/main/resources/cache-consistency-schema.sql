-- 只新增补偿表，不修改现有业务表。生产环境可由 DBA 预先执行并关闭自动建表。
CREATE TABLE IF NOT EXISTS cache_invalidation_task (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    patterns TEXT NOT NULL,
    attempts BIGINT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_cache_task_due (next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS activity_admission_outbox (
    last_error VARCHAR(32) NULL,
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    payload JSON NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    dead_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admission_outbox_due (sent_at, dead_at, next_attempt_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
