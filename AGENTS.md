# AGENTS.md

## 项目概述
数字百团是一个校园社团管理系统，包含 Spring Boot 后端和学生端、社团管理员端、超级管理员端三个 Vue 2 前端，用于社团信息管理、社团加入申请、活动报名、活动/社团审核、成员维护、批量导入和数据展示。

## 开发命令
- 后端运行测试：`cd club_springboot && mvn test`
- 后端本地启动：`cd club_springboot && mvn spring-boot:run`
- 后端构建打包：`cd club_springboot && mvn package`
- 学生端安装依赖：`cd vue_student && npm install`
- 学生端本地启动：`cd vue_student && npm run dev`
- 学生端构建打包：`cd vue_student && npm run build:prod`
- 管理员端安装依赖：`cd vue_admin && npm install`
- 管理员端本地启动：`cd vue_admin && npm run dev`
- 管理员端构建打包：`cd vue_admin && npm run build:prod`
- 超级管理员端安装依赖：`cd vue_superadmin && npm install`
- 超级管理员端本地启动：`cd vue_superadmin && npm run dev`
- 超级管理员端构建打包：`cd vue_superadmin && npm run build:prod`

## 关键目录
- `club_springboot/` - Spring Boot 后端，包含 controller、service、mapper、config、aop、mq、utils、resources/mapper 等。
- `vue_student/` - 学生端 Vue 2 应用，包含社团列表、活动列表、申请、个人信息、我的社团/申请等页面。
- `vue_admin/` - 社团管理员端 Vue 2 应用，包含申请列表、社团管理、成员管理、社团活动等页面。
- `vue_superadmin/` - 超级管理员端 Vue 2 应用，包含学生/社团管理、活动审批、批量添加、数据展示等页面。
- `club_springboot/src/main/resources/application.properties` - 后端端口、MySQL、Redis、Kafka、MyBatis 等配置。
- `club_springboot/src/main/resources/mapper/` - MyBatis XML SQL 映射。
- `.codex/rules/ai-readme/` - AI README 生成的 Codex Markdown 项目规则文档。

## 边界约束
- `manual/` 下的 AI README 文档由人工维护；已存在时不要覆盖。
- 不要凭空补业务规则、接口路径、命令或类名；无法从代码确认时标 `<!-- TODO -->`。
- 后端默认端口为 `8081`，三个前端开发环境默认请求 `http://localhost:8081`。
- 后端统一响应结构主要由 `Result` / `PageUtils` 生成，前端 `request.js` 按 `code === 20000` 判断成功。
- 数据库连接默认指向本地 MySQL `club` 库，Redis 默认 `127.0.0.1:6379`，Kafka 默认 `localhost:9092` 且当前业务监听配置可关闭。
- 修改功能时优先沿用现有分层：Controller -> Service -> ServiceImpl -> Mapper -> XML。
- 不要随意删除或覆盖用户已有改动；提交前检查当前工作树状态。

## AI 上下文
详细规则见 `.codex/rules/ai-readme/README.md`：
- 架构：`.codex/rules/ai-readme/generated/技术架构.md`
- 流程：`.codex/rules/ai-readme/generated/核心流程.md`
- 结构：`.codex/rules/ai-readme/generated/项目结构.md`
- 开发：`.codex/rules/ai-readme/generated/开发指南.md`
- 业务（人工维护）：`.codex/rules/ai-readme/manual/业务知识.md`
- 踩坑（人工维护）：`.codex/rules/ai-readme/manual/历史经验.md`
