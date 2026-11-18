-- MySQL 8：先执行本脚本，再启用 ELASTICSEARCH_ENABLED；不修改或删除活动数据。
-- 触发器覆盖所有写入口（包括报名热度更新），事件随业务事务提交/回滚。
CREATE TABLE IF NOT EXISTS activity_search_outbox (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id INT NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  available_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  last_error VARCHAR(500) NULL,
  KEY idx_search_due (available_at, id)
) ENGINE=InnoDB;

-- 重复执行应在暂停活动写入的维护窗口操作，避免 DROP/CREATE 间隙漏掉事件。
DROP TRIGGER IF EXISTS activity_search_insert;
DROP TRIGGER IF EXISTS activity_search_update;
DROP TRIGGER IF EXISTS activity_search_delete;
CREATE TRIGGER activity_search_insert AFTER INSERT ON activity FOR EACH ROW
  INSERT INTO activity_search_outbox(activity_id) VALUES (NEW.id);
CREATE TRIGGER activity_search_update AFTER UPDATE ON activity FOR EACH ROW
  INSERT INTO activity_search_outbox(activity_id) VALUES (NEW.id);
CREATE TRIGGER activity_search_delete AFTER DELETE ON activity FOR EACH ROW
  INSERT INTO activity_search_outbox(activity_id) VALUES (OLD.id);

-- 初次回填及重跑修复；消费者按当前主库状态写入，不使用旧事件中的字段快照。
INSERT INTO activity_search_outbox(activity_id) SELECT id FROM activity FOR SHARE;
