# baituan 活动搜索

## 查询用途与边界

ES 服务于活动发现：同时搜索活动名称、社团、主办方、简介、附件正文，并按相关性、热度、id 排序；支持分隔音节和完整连写拼音。精确详情、报名、权限判断仍以 MySQL 为准。MySQL 的事务与唯一约束不会被 ES 替代。

中文采用内置 CJK analyzer（中文相邻双字切分），部署无需 IK 插件；拼音由 pinyin4j 生成后用 standard analyzer。当前支持多音字的首个读音，不提供首字母模糊匹配、同义词或专业词典。CJK 是可部署的基线，不宣称在所有业务语料上优于 IK；后续应根据真实查询集评估召回率再决定换分词器。

唯一映射来源：`club_springboot/src/main/resources/search/activity-index.json`。严格 mapping、版本元数据、整型排序字段、审核状态过滤均显式配置。客户端/核心/x-content 对齐 7.17.22。本次兼容项目现有版本，7.17 已停止更新，后续升级需单独做客户端及集群迁移验证。

## 启用步骤

1. 备份目标数据库，在暂停活动写入的维护窗口执行 `club_springboot/src/main/resources/db/migration/activity-search-outbox.sql`。该脚本创建持久化队列和三类触发器，并把历史活动入队；不会删除活动数据。MySQL 需为 8.x，迁移账号需要 TRIGGER 权限。重跑有 DROP/CREATE 触发器间隙，因此不能边写入边重跑。
2. 启动与客户端匹配的 ES。仓库 Dockerfile/yml 是单节点开发配置（关闭 ES 认证），仅绑定本机或放在受控开发网络；正式集群使用 HTTPS/认证及副本策略。
3. 设置环境变量并启动后端。默认保持关闭，避免未经迁移就连接业务库。

```text
ELASTICSEARCH_ENABLED=true
ELASTICSEARCH_HOST=127.0.0.1
ELASTICSEARCH_PORT=9200
ELASTICSEARCH_SCHEME=http
ELASTICSEARCH_INDEX=baituan_activity_v2
# 认证集群再设置 ELASTICSEARCH_USERNAME / ELASTICSEARCH_PASSWORD
```

应用首次自动创建物理索引 `baituan_activity_v2_000001` 和别名 `baituan_activity_v2`，并入队回填历史活动。写入要求目标是别名，防止索引丢失后 ES 自动生成错误动态 mapping。也可以用 PowerShell 7 执行 `./init-index.ps1`；该方式预建索引后需调用下面的回填接口。

旧的 `baituan_activity` 可能没有 mapping，不能直接复用。使用新索引名保留旧索引。更换分词器时需要新物理索引及完整回填；本实现不提供无停机双写/切换编排，维护窗口内完成回填、验证后再开放搜索，不要宣称零停机迁移。

## 接口契约

- `GET /activity/search?keyword=人工智能&pageNum=1&pageSize=20`：STUDENT、MANAGER、TOP_MANAGER 均可；匿名 401。
- 默认第 1 页、每页 10 条；每页 1–100 条，页码乘页大小最多 10000，超出明确报错。需要更深遍历应另做 search_after 导出接口。
- `data.items` 为当前页；`data.page.total/pages` 来自 ES 精确计数，采用 from/size，绝不对前 100 条做内存分页。
- ES 命中 id 会批量回查 MySQL，返回完整业务字段并过滤撤回/删除记录。同步窗口可能导致本页少于 pageSize；`filteredStaleHits` 表示本页过滤量，`totalScope=indexMatches` 说明 total 为索引匹配数，不假称数据库强一致总数。
- `searchSource=elasticsearch/mysql` 和 `degraded` 明确区分正常与降级。ES 异常/超时/分片失败时用 MySQL 分页和 COUNT 降级，仅搜索名称、社团、主办方，不冒充具有附件/拼音能力。空关键词直接使用 MySQL 列表分页。
- `POST /search/index/rebuildActivities`：仅 TOP_MANAGER；返回 `queued` 和 `state=QUEUED`，表示重新入队所有现存活动，不是完成索引，也不是删除再建物理索引。
- `GET /search/index/status`：仅 TOP_MANAGER；返回 pending、retrying、oldest。pending=0 后还需等待 ES refresh（默认 1s）才能保证已处理内容对搜索可见。

## 同步、失败与附件

活动表 INSERT/UPDATE/DELETE 触发器写 outbox，与业务事务一起提交或回滚；覆盖审核、资料更新、热度变化及删除，不依赖某个 Controller 记得调用 ES。业务事务不再访问 ES。

后台每 2 秒触发一轮，最多处理 50 条；每条独立数据库事务以 FOR UPDATE SKIP LOCKED 领取，多个实例可以并行。成功后删任务，失败保留错误并以 2/4/8…秒退避、最高 300 秒，无需 Kafka。进程重启后继续处理。ES 连接/读取/连接池等待超时分别为 1s/3s/0.5s。消费者仍会短时持有一个数据库连接；这是一套适合当前项目规模的方案，不等于已完成高吞吐压测。

事件自增 id 作为 ES external_gte 版本，拒绝较旧任务覆盖较新任务。消费者重新读取当前 MySQL 行，不使用过时字段快照。删除/撤回写 `is_accepted=false` 的精简墓碑，保留版本避免延迟请求复活内容。不要清空/重置 outbox 自增序列后继续写原索引；灾后恢复应使用新的索引名完整回填。

Tika 从本地上传目录抽取真正正文，不把附件 URL 当正文。真实路径校验阻止目录/符号链接逃逸，不下载远程 URL；输入最多 10 MiB，输出最多 100000 字符（长文本会截断）。已验证 txt、docx、pdf、zip；不包含 OCR 承诺。缺失、损坏、超限文件会使该活动任务保留重试，错误不会伪装为空正文成功；修复附件后自动重试，其他活动仍可处理。大型或不可信文档应进一步采用独立解析进程做 CPU/时间隔离，本实现的大小限制不等于解析执行时间硬上限。

运维检查：对 pending/oldest/重试数告警，定期查询 `activity_search_outbox.last_error`；持久故障不会自动丢弃任务，会占用数据库空间。停止使用 ES 时若保留触发器，事件仍持续累积，需制定停用/恢复策略。

## 可复现验证

```powershell
# 测试库必须是本机13306、名称以 codex_search_ 开头；禁止填业务库
$env:SEARCH_TEST_DB_URL='jdbc:mysql://127.0.0.1:13306/codex_search_integration?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8'
$env:ELASTICSEARCH_SMOKE_URL='http://127.0.0.1:19200'
mvn '-Dtest=ActivitySearchServiceTest,ActivitySearchIntegrationTest,ElasticsearchAvailabilityTest,SecurityConfigTest' test
# 使用新建的 codex_search_http_* 测试库启动真实后端、JWT/HTTP验证，最终自动停止后端
python scripts/test_activity_search_http.py
```

集成测试会重建其专用库的 activity/outbox 表；只放合成数据。HTTP 脚本依赖已构建 jar 和脚本内指定的本机 Docker/JDK 路径。验收输出位于 `target/surefire-reports`、`target/search-http/result.json`；不是生产 QPS/延迟结论。

