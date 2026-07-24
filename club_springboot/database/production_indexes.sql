-- 数字百团生产查询索引。可重复执行：已存在的索引会被跳过。
-- 建议先在 staging 执行并观察 DDL 锁表时间，再安排生产维护窗口。

DELIMITER //

DROP PROCEDURE IF EXISTS ensure_baituan_index //
CREATE PROCEDURE ensure_baituan_index(
    IN target_table VARCHAR(64),
    IN target_index VARCHAR(64),
    IN index_columns VARCHAR(500)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = target_table
          AND index_name = target_index
    ) THEN
        SET @index_sql = CONCAT(
                'ALTER TABLE `', REPLACE(target_table, '`', '``'),
                '` ADD INDEX `', REPLACE(target_index, '`', '``'),
                '` ', index_columns
            );
        PREPARE index_statement FROM @index_sql;
        EXECUTE index_statement;
        DEALLOCATE PREPARE index_statement;
    END IF;
END //

DELIMITER ;

CALL ensure_baituan_index('group', 'idx_group_accept_hot_id', '(`is_accepted`, `hot`, `id`)');
CALL ensure_baituan_index('group', 'idx_group_create_user', '(`create_user`)');
CALL ensure_baituan_index('group', 'idx_group_leader', '(`leader`)');
CALL ensure_baituan_index('group', 'idx_group_name', '(`name`)');

CALL ensure_baituan_index('activity', 'idx_activity_accept_hot_id', '(`is_accepted`, `hot`, `id`)');
CALL ensure_baituan_index('activity', 'idx_activity_create_user', '(`create_user`)');
CALL ensure_baituan_index('activity', 'idx_activity_organizer', '(`organizer`)');
CALL ensure_baituan_index('activity', 'idx_activity_group_name', '(`group_name`)');
CALL ensure_baituan_index('activity', 'idx_activity_name_group', '(`name`, `group_name`)');

CALL ensure_baituan_index('individual_group', 'idx_individual_group_user', '(`user_id`)');
CALL ensure_baituan_index('individual_group', 'idx_individual_group_group_status', '(`group_id`, `status`)');
CALL ensure_baituan_index('individual_group', 'idx_individual_group_group_user', '(`group_id`, `user_id`)');

CALL ensure_baituan_index('individual_activity', 'idx_individual_activity_user', '(`user_id`)');
CALL ensure_baituan_index('individual_activity', 'idx_individual_activity_activity_status', '(`activity_id`, `status`)');
CALL ensure_baituan_index('individual_activity', 'idx_individual_activity_activity_user', '(`activity_id`, `user_id`)');

CALL ensure_baituan_index('user', 'idx_user_status', '(`status`)');
CALL ensure_baituan_index('user', 'idx_user_name', '(`user_name`)');

DROP PROCEDURE ensure_baituan_index;

ANALYZE TABLE `user`, `group`, activity, individual_group, individual_activity;
