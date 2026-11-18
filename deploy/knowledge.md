# TeamRag 知识库接入

## 服务端配置

TeamRag 必须开启 `RAG_SECURITY_ENABLED=true`。数字百团只连接该部署，不另建向量库或模型调用实现。

将 `deploy/env/teamrag.yml.example` 复制到仓库外的本地配置文件，填写真实的社团 ID → TeamRag project 映射。
通过 `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/绝对路径/teamrag.yml` 加载。设置以下环境变量：

- `TEAMRAG_BASE_URL`：服务根地址，容器内不能使用指向宿主机服务的 `127.0.0.1`。
- `TEAMRAG_TOKEN_SECRET`：与该 TeamRag 部署的 `RAG_SECURITY_TOKEN_SECRET` 一致，至少32字符；仅放后端。
- `TEAMRAG_TENANT_ID`：与 TeamRag 的 `RAG_TENANT_ID` 一致。
- `TEAMRAG_GROUP_1_PROJECT`：示例社团1实际使用的项目名；其他社团通过配置映射增加。

密钥共享表示数字百团是该 TeamRag 租户的可信凭据签发方。每次请求只签发120秒有效、单项目、按用户区分的 READER 或 EDITOR 凭据，绝不签发 ADMIN 或 `*` 权限。
两端时钟应同步。数字百团会先调用 `/api/auth/me` 确认上游实际开启认证且身份正确，防止匿名记忆串用。

## 使用与权限

三端侧栏均有“社团知识库”，也可访问 `/#/knowledge`；下拉框只显示当前用户可访问的社团。
学生只能读取已正式加入的社团资料；管理员只能管理自己负责的社团；超级管理员仍受显式项目映射限制。
每次请求重新查询数据库权限。社团、用户角色、用户编号共同构成会话范围，浏览器只传本地会话编号。

- `GET /knowledge/groups`：当前用户可访问的社团列表，不返回项目凭据。
- `POST /knowledge/groups/{groupId}/chat`：JSON `query`、`conversationId`，返回 UTF-8 SSE。
- `GET /knowledge/groups/{groupId}/documents`：当前项目可见资料。
- `POST /knowledge/groups/{groupId}/documents`：管理员上传 multipart `file`，最多10MB。

TeamRag 返回的文本可能含有资料引用；当前接口没有独立的结构化引用事件，因此不伪造逐条证据卡片。
文档列表可供核对。真实模型效果、引用质量和召回准确率需要在真实资料与模型配置下另行验收。

上游故障通过 SSE `error` 事件提示；用户可停止回答。后端使用有界线程池、连接/空闲读取超时及180秒流式期限。
问答复用后端Redis用户限流（默认每分钟10次），上传默认每分钟5次；生产Redis故障不放行。
网关为知识库配置独立长连接路由，Nginx 禁止缓冲；不重试生成请求。

容器部署时，在基础集群文件之后叠加 `docker-compose.knowledge.yml`，并设置 `TEAMRAG_CONFIG_FILE` 指向本地映射文件。
该文件为两个后端挂载只读配置，并提供连接独立TeamRag部署所需的出站网络；不要将密钥挂载到前端。

## 已验证范围

后端覆盖成员权限、管理员范围、禁用状态、通配项目拒绝、凭据作用域、会话隔离、匿名上游拒绝、SSE内容及异步分派。
浏览器测试使用模拟业务接口，覆盖三端聊天、错误提示、资料列表、上传入口权限展示、会话重置及375/768/1024/1440宽度。
这些验证不代表已经连接真实模型、Pinecone或实际入库资料。
