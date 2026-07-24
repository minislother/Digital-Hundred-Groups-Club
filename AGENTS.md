# AGENTS.md

## 项目概述
数字百团是一个校园社团管理系统，包含 Spring Boot 后端、统一网页身份入口、学生端、社团管理员端、超级管理员端三个 Vue 3 业务网页端，以及 Android 原生端。系统用于社团信息管理、入团申请、活动报名、活动/社团审核、成员维护、批量导入和数据展示。

```text
统一入口/Android
  -> 学生端 / 管理员端 / 超级管理员端
  -> Spring Boot API
  -> MySQL / Redis / Kafka
```

## 开发命令
- 后端运行测试：`cd club_springboot && mvn test`
- 后端本地启动：`cd club_springboot && mvn spring-boot:run`
- 后端构建打包：`cd club_springboot && mvn package`
- 统一网页入口安装依赖：`cd web_portal && npm install`
- 统一网页入口启动：`cd web_portal && npm run dev`
- 统一网页入口构建：`cd web_portal && npm run build:prod`
- 学生端安装依赖：`cd vue_student && npm install`
- 学生端本地启动：`cd vue_student && npm run dev`
- 学生端构建打包：`cd vue_student && npm run build:prod`
- 管理员端安装依赖：`cd vue_admin && npm install`
- 管理员端本地启动：`cd vue_admin && npm run dev`
- 管理员端构建打包：`cd vue_admin && npm run build:prod`
- 超级管理员端安装依赖：`cd vue_superadmin && npm install`
- 超级管理员端本地启动：`cd vue_superadmin && npm run dev`
- 超级管理员端构建打包：`cd vue_superadmin && npm run build:prod`
- 前端聚合构建：`npm run build:frontends`
- Android 单元测试：`cd baituan_android && .\gradlew testDebugUnitTest`
- Android 调试构建：`cd baituan_android && .\gradlew assembleDebug`

## 关键目录
- `club_springboot/` - Spring Boot 后端，包含 controller、service、mapper、config、exception、utils、resources/mapper 等。
- `web_portal/` - 网页端统一身份入口，负责选择学生、管理员、超级管理员并跳转到对应登录页。
- `vue_student/` - 学生端 Vue 3 应用，包含社团列表、活动列表、申请、个人信息、我的社团/申请等页面。
- `vue_admin/` - 社团管理员端 Vue 3 应用，包含申请列表、社团管理、成员管理、社团活动等页面。
- `vue_superadmin/` - 超级管理员端 Vue 3 应用，包含学生/社团管理、活动审批、批量添加、数据展示等页面。
- `frontend-shared/` - 三个业务网页端共享工具模块。
- `baituan_android/` - Android 原生端，使用 Kotlin、Jetpack Compose、Hilt、Retrofit、DataStore。
- `club_springboot/src/main/resources/application.properties` - 后端端口、MySQL、Redis、Kafka、MyBatis 等配置。
- `club_springboot/src/main/resources/mapper/` - MyBatis XML SQL 映射。
- `.codex/rules/ai-readme/` - AI README 生成的 Codex Markdown 项目规则文档。

## 边界约束
- `manual/` 下的 AI README 文档由人工维护；已存在时不要覆盖。
- 不要凭空补业务规则、接口路径、命令或类名；无法从代码确认时标 `<!-- TODO -->`。
- 后端默认端口为 `8081`，三个业务网页端开发环境默认请求 `http://localhost:8081`。
- 统一网页入口默认端口为 `9527`，学生端 `9533`，管理员端 `9531`，超级管理员端 `9532`。
- 后端统一响应结构主要由 `Result` / `PageUtils` 生成，前端 `request.js` 按 `code === 20000` 判断成功。
- 数据库连接默认指向本地 MySQL `club` 库，Redis 默认 `127.0.0.1:6379`，Kafka 默认 `localhost:9092` 且当前业务监听配置可关闭。
- 修改功能时优先沿用现有分层：Controller -> Service -> ServiceImpl -> Mapper -> XML。
- 统一入口只负责身份选择和跳转，不保存三端 token，不承载业务页面。
- 不要随意删除或覆盖用户已有改动；提交前检查当前工作树状态。

## 专项执行规则
- Code review：按 [@CODE_REVIEW_RULES](.codex/rules/CODE_REVIEW_RULES.md) 执行。
- 执行单元测试：按 [@TEST_RULES](.codex/rules/TEST_RULES.md) 执行。
- 单元测试覆盖率：按 [@TEST_ACC_RULES](.codex/rules/TEST_ACC_RULES.md) 执行。

## AI 上下文
详细规则见 `.codex/rules/ai-readme/README.md`：
- 架构：`.codex/rules/ai-readme/generated/技术架构.md`
- 流程：`.codex/rules/ai-readme/generated/核心流程.md`
- 结构：`.codex/rules/ai-readme/generated/项目结构.md`
- 开发：`.codex/rules/ai-readme/generated/开发指南.md`
- 业务（人工维护）：`.codex/rules/ai-readme/manual/业务知识.md`
- 踩坑（人工维护）：`.codex/rules/ai-readme/manual/历史经验.md`
