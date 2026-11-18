# 活动报名数据库唯一兜底

`individual_activity` 使用完整业务唯一键
`uk_individual_activity_activity_user(activity_id, user_id)`，两列均为 NOT NULL。
不包含 status：待审核、已通过、已拒绝共用一条报名记录，审批更新原记录。
物理删除后数据库允许重新插入，但 Redis 成员/计数清理仍需由删除流程正确完成；
此次不增加新的“取消后重新报名”产品规则。该约束防重复关系，不单独保证名额不超售。

## 新库

项目根目录 `club_finalversion.sql` 已包含约束。

## 已有数据库

在备份和停止所有报名写入口（含消费者、管理员操作）后，用 MySQL 客户端选择目标库，执行：

```sql
SOURCE /absolute/path/club_springboot/src/main/resources/db/migration/activity-registration-unique.sql;
```

不要加 `--force`。脚本会先检查重复、NULL 和空白用户编号，不删除或合并任何记录。
若预检失败，用以下只读查询找出需要业务人工确认的记录：

```sql
SELECT activity_id, user_id, COUNT(*) AS copies
FROM individual_activity GROUP BY activity_id, user_id HAVING COUNT(*) > 1;
SELECT id, activity_id, user_id, status FROM individual_activity
WHERE activity_id IS NULL OR user_id IS NULL OR TRIM(user_id) = '';
SHOW INDEX FROM individual_activity;
```

确认哪条有效、保留审计记录并修正数据后再执行迁移。不要用 MIN(id) 自动删除冲突记录。
脚本要求 MySQL 8.x 及 ALTER/CREATE ROUTINE/EXECUTE 权限；DDL 隐式提交，不能靠事务回滚撤销。
预检和 DDL 之间即使发生竞争，最终唯一索引创建仍会拒绝重复，必须检查客户端退出码。
重跑会验证同名索引确实是完整的两列唯一键；同名但定义不同会拒绝继续。
本项目没有迁移框架自动执行此脚本，需先部署数据库约束，再启动新应用。

## 应用行为

同步报名碰到该唯一键冲突时返回既有 `APPLY_REPEAT` 业务错误，事务回滚后补偿本次 Redis 预占。
其他数据库错误继续上抛。异步消费者出现该键冲突后再次确认记录存在才按幂等重放处理。
Redis 返回 4（计数未初始化）不再被误认为预占成功，避免错误走异步入队/回滚扣减。

## 验证

```powershell
mvn '-Dtest=ActivityAdmissionUniquenessTest' test
# 使用独立 MySQL 8 测试容器；脚本只创建 codex_registration_* 数据库。
$env:MYSQL_TEST_PASSWORD='<test-container-password>'
python scripts/test_registration_unique.py --container codex-baituan-bench-mysql
```

真实数据库测试覆盖新库/旧库迁移、重复执行、脏数据拒绝、NULL、审批状态、同名错误索引和
16 个并发连接绕过 Redis 直接插入同一活动用户对。结果输出到 target，测试数据库保留供核查。
