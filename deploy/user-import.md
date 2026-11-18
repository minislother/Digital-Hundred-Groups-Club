# 用户 Excel 导入（不使用消息队列）

## 启用

1. 备份并核对数据库，确认 `user.user_id` 为 NOT NULL 主键。现有基线已经包含该主键，不需要增加重复索引。
2. 核对并完成项目已有的 `deploy/contact-encryption-schema.sql` 联系方式加密升级，导入沿用姓名加密及检索摘要，要求现有 CONTACT 密钥配置完整。
3. 审核并执行 `club_springboot/src/main/resources/db/flyway/V8__user_import_tasks.sql`，或按现有 schema profile 的 Flyway 流程迁移。不要对未核对基线的存量库直接启用 Flyway。
4. 设置 `USER_IMPORT_ENABLED=true` 后启动服务。默认关闭，防止未迁移的环境启动后台查询。
5. `USER_IMPORT_DIRECTORY` 默认为 `data/user-import`。包含账号初始密码的原文件必须保存在非公开目录，不得映射到 Nginx 静态站点或 `/upload`。多实例必须挂载同一个共享目录并使用相同路径；完成任务的文件和明细按学校数据保留策略人工清理，不删除待处理和处理中任务的文件。

## 默认参数

- `USER_IMPORT_CONCURRENCY=2`：所有实例共享的并发上限，单个实例最多两个执行线程。
- `USER_IMPORT_BATCH_SIZE=500`：每批数据行数，运行时限制为 1～1000。
- `USER_IMPORT_MAX_PENDING=100`：等待及处理中的任务总容量。
- `USER_IMPORT_MAX_ROWS=200000`：单文件非空数据行上限；超限任务失败，已提交批次保留。
- Servlet 文件大小沿用 `MAX_FILE_SIZE` / `MAX_REQUEST_SIZE`，默认 10MB。

## 行为与恢复

上传只落盘并创建任务，接口不再投递 Kafka。数据库队列锁控制容量及全局执行数量；任务有独立 owner 和 120 秒租约，每 3 秒续租。失联任务重新排队并更换 owner。每个批次锁定任务行验证 owner、租约和检查点，旧执行者无法写入已被接管的任务。

XLSX 使用 POI SAX 逐行读取，仍需保存 POI 的样式和共享字符串索引；不是严格恒定内存。旧 XLS 使用 POI 工作簿读取，仍受文件大小及任务并发限制，大文件建议转换为 XLSX。

每批数据、错误明细和检查点在同一个事务中提交。重复键冲突先回滚批量 SQL 到保存点，再逐条定位重复记录；其他数据库错误回滚当前批。重启恢复会从头扫描文件，跳过已提交的数据序号。文件必须保持不变。

状态：PENDING / PROCESSING / SUCCESS / PARTIAL_SUCCESS / FAILED。FAILED 不自动无限重试，可检查错误后重新上传，已有账号会跳过。错误明细中的行号是跨 Sheet 的非空数据序号，不包含表头及空行。

仅支持模板中的用户字段（学号、姓名或名字、密码），新增用户身份为学生。页面原有管理员、社团卡片没有对应实现，已收敛为真实支持的学生导入。

## 接口

均要求超级管理员身份；查询仅限上传人本人。

- `POST /topManager/uploadExcel`：multipart 字段 `file`，返回 `data.task`，含字符串任务编号 `id`。
- `GET /topManager/importTasks`：最近 20 个本人任务。
- `GET /topManager/importTasks/{id}`：本人单个任务进度。
- `GET /topManager/importTasks/{id}/errors?after=0`：按数据序号游标，每页最多 100 条，仅返回序号及原因，不返回密码或完整原始数据。

总数在文件扫描完成后确定，处理期间展示已处理行数，不伪造百分比。页面每 3 秒查询，离开后停止，重新打开可恢复查看。

## 验证边界

测试使用独立 H2 内存数据库验证真实事务、保存点、并发账号冲突、批次回滚、租约失效及检查点恢复；另测 xls/xlsx 多 Sheet 和回调失败。上线前仍需在目标 MySQL 版本及实际共享文件系统上验收与压测。
