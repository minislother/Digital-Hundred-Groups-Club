# 技术升级实施记录

## 已落地

| 范围 | 当前实现 |
| --- | --- |
| 业务后端 | Java 21、Spring Boot 4.0.9、Jakarta、SecurityFilterChain、MyBatis-Plus 3.5.18、Springdoc 3.0.3 |
| JSON与缓存 | 暂保留Jackson 2兼容模块，维持现有JSON、JWT和Redis缓存契约；不代表已迁移Jackson 3 |
| 网页工程 | 四端Vite 7.3.1，pnpm工作区及锁文件；Axios 1.x；新增模块使用TypeScript和Pinia |
| 状态兼容 | 旧页面仍保留Vuex，新增知识库会话用Pinia；没有一次性重写所有旧页面 |
| 测试 | 三业务端Vitest与Vue Test Utils 2；后端JUnit/Mockito；三端知识库和统一入口浏览器回归 |
| AI | TeamRag项目授权、按用户/社团隔离的会话、UTF-8 SSE、资料列表、管理端上传与停止回答 |
| 微服务入口 | Gateway、LoadBalancer、Consul Discovery、Resilience4j、可选Redis网关限流 |
| 部署 | Nginx最少连接分配双网关，网关轮询双后端；共享数据库、Redis和上传卷 |
| 数据库升级 | Flyway独立迁移目录、显式schema profile、存量库基线保护与校验 |
| 监控 | Prometheus、Grafana、自动配置的数据源与实例/请求/延迟/JVM面板 |
| 备份 | Compose数据库备份及随机临时库恢复验证脚本，禁止覆盖现有备份 |
| 自动检查 | 后端、网关和前端各有GitHub Actions配置 |

当前业务仍是一个业务后端加独立网关，并未把所有领域拆成独立微服务。现有审批沿用原单级批准/拒绝语义，不凭空增加多级审批规则，也未强行引入Flowable。

## 本地使用

```powershell
# 使用本地JDK 21，不更改系统级Java设置
.\club_springboot\scripts\mvn21.ps1 test
# 前端依赖与四端构建
pnpm install --frozen-lockfile
npm run build:frontends
# 各业务端单测示例
npm --prefix vue_admin run test:unit
npm --prefix vue_admin run typecheck
# 网关
mvn -B -f baituan_gateway/pom.xml package
python baituan_gateway/scripts/smoke_test.py
python baituan_gateway/scripts/smoke_test.py --consul
# 浏览器回归需先构建前端，并安装Python Playwright及Chromium
python frontend-shared/tests/knowledge_browser.py
```

后端要求JDK 21，网关可独立使用JDK 17+，前端要求Node 22.12+。三端和统一入口原开发端口不变。

## TeamRag接入

详见 [knowledge.md](knowledge.md) 与 `deploy/env/teamrag.yml.example`。
需要实际服务地址、租户、后端密钥及“社团ID → 已入库project”映射。密钥不得放入前端或聊天。
功能默认关闭，未配置时明确提示。前端只显示当前用户有权访问的社团名称，不暴露其他项目映射。
资料上传、索引和模型调用仍由TeamRag完成。当前接口仅提供文本SSE，不伪造结构化引用。

## Flyway

迁移位于 `club_springboot/src/main/resources/db/flyway`：

- V1只创建初始结构，不包含DROP或演示数据；仅用于空库。
- V2创建缓存补偿与报名发件箱表。
- V3预检历史报名，再添加唯一约束；遇到重复/空业务键会停止，不自动删除数据。
- V4创建活动搜索发件箱与触发器。
- V5创建报名消费回执与补偿表。关闭自动建表的环境需要先执行该迁移。
- V6为报名发件箱增加最近一次发送错误类别。启用 Flyway 或关闭自动建表的环境需在升级实例前应用该迁移。

升级报名链路时统一切换所有业务后端副本。新版本使用按请求令牌区分的 Redis 在途预占键，并用消费回执阻止已处理事件再次落库。旧消息仍可消费，缺少新令牌的旧死信在管理 API 的 `consumerItems` 中标为 `MANUAL_REVIEW`。

默认不自动接管现有数据库。备份并核对存量表结构后，显式启用 `prod,schema` profile。
存量库首次接管需要操作者明确设置 `DB_BASELINE_EXISTING=true`，以版本1建立历史；空库保持false。
完成首次接管后移除该开关。禁用Flyway clean，禁止用自动repair掩盖校验失败。
`schema` profile同时关闭旧缓存自动建表器。不能把profile顺序写反，避免prod覆盖schema设置。
迁移账号需有建表、索引、例程与触发器权限；MySQL开启binlog时，还需遵守实例的触发器权限策略。
可用 `DB_MIGRATION_USERNAME` / `DB_MIGRATION_PASSWORD` 指定专用迁移账号，不必提升业务账号权限。

```powershell
python deploy/backup_database.py outputs/backups/club-before-upgrade.sql --verify
```

恢复验证只操作本次创建的随机临时数据库，不覆盖业务库。需要运行中的Compose MySQL。

## 集群与监控

基础集群：

```powershell
docker compose -f docker-compose.yml -f docker-compose.cluster.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.cluster.yml up -d --build
```

监控叠加 `docker-compose.monitoring.yml`。需设置 `GRAFANA_ADMIN_PASSWORD` 和 `METRICS_TOKEN_FILE`。
后者指向本地保存的有效超级管理员JWT文件，Prometheus用它采集两个业务实例；过期后须轮换。
Grafana只绑定本机3000端口。网关管理端口9090仅供内部网络采集；Nginx公开链路不代理该端口。
单节点Consul、共享MySQL/Redis和单Nginx不具备整套基础设施高可用；生产扩容前需验证各自的故障恢复。

## 验证及尚未验收的边界

- 后端迁移后的JUnit测试、前端构建/单测/类型检查与浏览器回归实际执行，结果见本次执行日志。
- 网关以真实进程和两个临时HTTP服务验证轮询、凭据透传、写请求不重放、熔断和故障503。
- Consul客户端通过模拟其HTTP API验证服务发现与健康实例列表变更；这不等同真实Consul集群测试。
- Compose组合配置已校验，但当前Docker Desktop因本机dockerInference套接字错误无法启动引擎。
- 因此尚未执行真实容器集成、MySQL迁移、备份恢复、Redis限流及多实例压力验证。
- TeamRag真实地址、社团project映射和本地密钥未配置前，不能宣称真实模型、召回质量及资料入库已验收。

用户原有报名、缓存、搜索和SQL改动保留；未提交或推送。


## 报名流量与维护

- 后端的 `/actuator/health/traffic` 供 Docker 和 Consul 判断能否承接流量，只检查数据库和应用健康。全局 `/actuator/health` 仍保留已启用的 Redis 健康状态，运维应独立监控。数据库故障仍会让实例退出流量。
- `observability` profile 每 30 秒采样待投递条数和最老待投递事件年龄。查询最多等待 3 秒，失败时样本为 NaN。两个指标来自共享数据库，跨实例查询取 `max`。异步报名从写入发件箱前到消费提交后的耗时使用 `baituan_admission_completion_seconds_bucket`，跨实例按 `le` 汇总后再算分位数。旧消息没有计时字段，仍可正常消费。压测机、生产者和消费者应校准时钟。
- 异步压测应启用 Compose 的 `mq` profile，设置 `KAFKA_ENABLED=true` 和 `KAFKA_LISTENER_AUTO_STARTUP=true`，并确认发件箱和消费回执均有数据。
- 压测从 Nginx 入口走 Gateway 与两个后端，分别测单热门活动、多活动并发、Redis 停机和 Kafka 确认变慢。记录受理与落库延迟、积压清空时间、数据库行锁等待、连接池等待、重复和超卖数。连接池预算按实例数相加。Redis 降级限额当前按活动及实例生效，不能当作数据库全局限额。
- Controller 调用统一报名服务，名额和写入规则由报名服务维护。发件箱负责投递，消费事务负责报名落库和回执，补偿器负责 Redis 令牌释放。修改事件字段时保留旧消息兼容性，数据库变更同时考虑 Flyway 和自动初始化路径。跨边界改动需验证回滚、重投与双实例认领。
