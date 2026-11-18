-- MySQL 8.x；先选定目标数据库，在停止报名写入的维护窗口执行。
-- 只增加约束，不自动合并/删除历史报名；预检失败时先人工核对并保留审计记录。
-- DDL 隐式提交，不能用事务回滚代替备份。禁止 mysql --force 忽略错误。
DROP PROCEDURE IF EXISTS migrate_activity_registration_unique;
DELIMITER $$
CREATE PROCEDURE migrate_activity_registration_unique()
BEGIN
    IF EXISTS (SELECT 1 FROM individual_activity
               WHERE activity_id IS NULL OR user_id IS NULL OR TRIM(user_id) = '') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registration migration blocked: null/blank business key';
    END IF;
    IF EXISTS (SELECT 1 FROM individual_activity GROUP BY activity_id, user_id HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registration migration blocked: duplicate activity/user pairs';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'individual_activity'
               AND index_name = 'uk_individual_activity_activity_user') THEN
        -- 重跑迁移时核对定义，不能仅凭同名就认为保护已经存在。
        IF (SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE() AND table_name = 'individual_activity'
              AND index_name = 'uk_individual_activity_activity_user') <> 2
           OR (SELECT COUNT(*) FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'individual_activity'
                 AND index_name = 'uk_individual_activity_activity_user' AND non_unique = 0 AND sub_part IS NULL
                 AND ((seq_in_index = 1 AND column_name = 'activity_id')
                   OR (seq_in_index = 2 AND column_name = 'user_id'))) <> 2 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Registration migration blocked: unexpected index definition';
        END IF;
        ALTER TABLE individual_activity
            MODIFY activity_id INT NOT NULL COMMENT '活动编号',
            MODIFY user_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户编号';
    ELSE
        ALTER TABLE individual_activity
            MODIFY activity_id INT NOT NULL COMMENT '活动编号',
            MODIFY user_id VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户编号',
            ADD UNIQUE KEY uk_individual_activity_activity_user (activity_id, user_id);
    END IF;
END$$
DELIMITER ;
CALL migrate_activity_registration_unique();
DROP PROCEDURE migrate_activity_registration_unique;
