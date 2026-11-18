-- Apply explicitly during a maintenance window after reviewing EXPLAIN on representative data.
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'individual_activity' AND index_name = 'idx_ia_activity_status_id') = 0,
'CREATE INDEX idx_ia_activity_status_id ON `individual_activity` (activity_id, status, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'individual_activity' AND index_name = 'idx_ia_user_id') = 0,
'CREATE INDEX idx_ia_user_id ON `individual_activity` (user_id, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'individual_group' AND index_name = 'idx_ig_group_status_user') = 0,
'CREATE INDEX idx_ig_group_status_user ON `individual_group` (group_id, status, user_id, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'individual_group' AND index_name = 'idx_ig_user_id') = 0,
'CREATE INDEX idx_ig_user_id ON `individual_group` (user_id, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'activity' AND index_name = 'idx_activity_visible_hot') = 0,
'CREATE INDEX idx_activity_visible_hot ON `activity` (is_accepted, hot, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'activity' AND index_name = 'idx_activity_group_time') = 0,
'CREATE INDEX idx_activity_group_time ON `activity` (group_name, time, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'group' AND index_name = 'idx_group_visible_hot') = 0,
'CREATE INDEX idx_group_visible_hot ON `group` (is_accepted, hot, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stu_app' AND index_name = 'idx_stuapp_student') = 0,
'CREATE INDEX idx_stuapp_student ON `stu_app` (stu_number, application_id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'stu_app' AND index_name = 'idx_stuapp_group') = 0,
'CREATE INDEX idx_stuapp_group ON `stu_app` (group_name, application_id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inspection' AND index_name = 'idx_inspection_group_year') = 0,
'CREATE INDEX idx_inspection_group_year ON `inspection` (group_name, year, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'inspection' AND index_name = 'idx_inspection_year') = 0,
'CREATE INDEX idx_inspection_year ON `inspection` (year, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user' AND index_name = 'idx_user_status') = 0,
'CREATE INDEX idx_user_status ON `user` (status, user_id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;

SET @page_index_sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'individual_group' AND index_name = 'idx_ig_group_status_id') = 0,
'CREATE INDEX idx_ig_group_status_id ON individual_group (group_id, status, id)', 'SELECT 1');
PREPARE page_index_stmt FROM @page_index_sql;
EXECUTE page_index_stmt;
DEALLOCATE PREPARE page_index_stmt;
