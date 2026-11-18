# 列表与搜索分页契约

## 数据库列表

以下接口默认第 1 页、每页 10 条，显式传入 pageNum 与 pageSize。每页最多 100 条，OFFSET 超过 10000 返回 PAGE_TOO_DEEP。Controller 通过 Service 调用 Mapper 的 LIMIT/OFFSET 与 COUNT，不能对全量列表再截页。

| 模块 | 已检查的列表入口 | 处理 |
| --- | --- | --- |
| activity | all、myActivity、myApps、allApps、myJoinedActivities、getActivityApplicants | SQL 分页，保留各自的可见性与筛选条件 |
| group | all、allApps | SQL 分页 |
| individualActivity | allActivities、getAllStudents、getAllApplyStudents、allManagedActivities | SQL 分页 |
| individualGroup | allGroups、getStudentsByGroup、getGroupApplyList、allManagedGroups、getAllStudents | SQL 分页；个人社团和成员树批量补充关联社团，避免逐条回查 |
| application | all、myApps、recApps | SQL 分页，保留原有申请筛选 |
| inspection | all、findAllByGroup、findAllByGroupAndYear、findAllByYear | SQL 分页 |
| topManager | getAllUsers | SQL 分页 |
| activity/search | 关键词分页 | ES from/size，最多前 10000 个匹配，异常回落到同样有限制的 MySQL |

旧调用方不传分页参数也只返回第一页。Android 已有页码参数的入口继续沿用原契约；allGroups 保留 stuItems 和 groupItems，同时返回 page。网页的列表请求必须明确携带分页参数，并用 page.total 展示总数。统计卡片若只持有一页数据，应标明为本页统计。

## 游标与免计数

- individualActivity/studentSlice 和 individualGroup/studentSlice 从当前认证身份读取学生编号，传 beforeId 与 pageSize，按关系 ID 倒序，多查一条判断 hasNext，返回 nextCursor，不执行 COUNT。
- individualActivity/memberSlice 和 individualGroup/memberSlice 传 resourceId、status、beforeId 与 pageSize。status 只接受 0 或 1，服务层校验管理员是否管理对应资源。
- activity/searchSlice 传 keyword、pageSize 与 cursor。ES 使用一分钟 PIT 和 search_after，按相关性、热度、活动 ID、分片文档标识稳定排序，不统计总命中数。MySQL 降级按活动 ID 倒序。游标签名绑定关键词、数据源与过期时间，所有实例必须使用相同 JWT_SECRET。翻页中 ES 故障返回重新搜索提示，不混用两种排序。
- 学生活动列表和我的社团使用“加载更多”。更改搜索条件重新开始游标。ES 到末页主动关闭 PIT，用户离开页面后由一分钟保活超时释放。
- PIT 固定索引视图，数据库回查仍过滤撤回或删除记录。因此一页可能不足 pageSize，hasNext 依照原索引命中判断。

## 全部控制器检查范围

已检查 controller 目录下的列表、单记录、统计、状态变更、认证、上传及知识库代理入口。认证、报名、修改、审批、上传、单记录详情和流式问答不应用分页。天气和视频返回固定对象。活动/社团热门和成员排行在 SQL 中已限制 5 条。报名生产与消费死信查询各有 200 条上限；索引同步状态返回汇总数据。

TeamRag 的 groups 枚举来自服务端配置的项目映射，逐项校验权限。documents 代理上游 /api/document，当前本地 TeamRag 对应 GET 接口直接返回全部文档，尚无分页契约。该项需要在 TeamRag 同步新增服务端分页后再调整代理和共享知识库组件，本次没有伪装为数据库分页。

项目未发现独立的全量数据导出控制器。附件下载是单文件操作。新增导出时应使用独立的分批读取任务，不依赖列表接口循环加载到浏览器。

## 数据库升级与运行验证

V7__list_pagination_indexes.sql 增加与实际筛选、排序匹配的联合索引。采用显式 schema profile 或 DBA 维护窗口执行。未启用 Flyway 的部署也需显式应用 V7，禁止高峰期随应用启动执行索引 DDL。脚本按索引名判断已存在，支持重复执行。

索引增加写入成本。隔离数据库测试重复执行迁移、运行所有新分页与计数 SQL，并用代表数据检查 EXPLAIN。生产仍应按实际分布检查慢 SQL、扫描行数和锁等待，不能从小规模测试推算学生承载量。

跨实例监控应观察列表 p95/p99、COUNT 耗时、数据库连接池等待、ES 超时及 PIT 数。数据库页码分页允许并发写入引起页间漂移；需要连续浏览时使用不可变关系 ID 或 PIT 游标。
