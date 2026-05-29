# 数字百团社团管理系统

数字百团是一个校园社团管理系统，采用前后端分离架构，包含 Spring Boot 后端和三个 Vue 2 前端：学生端、社团管理员端、超级管理员端。系统围绕社团信息展示、入团申请、活动报名、活动/社团审核、成员维护、批量导入和数据统计展示等场景设计。

## 项目模块

| 目录 | 说明 |
| --- | --- |
| `club_springboot/` | Spring Boot 后端服务 |
| `vue_student/` | 学生端 Vue 2 应用 |
| `vue_admin/` | 社团管理员端 Vue 2 应用 |
| `vue_superadmin/` | 超级管理员端 Vue 2 应用 |
| `club_finalversion.sql` | 数据库初始化脚本 |

## 主要功能

### 学生端

- 浏览和搜索社团、活动信息
- 查看社团和活动详情
- 申请加入社团
- 报名参加活动
- 查看我的社团、我的申请和已加入活动
- 维护个人资料

### 社团管理员端

- 管理社团资料、附件和图片
- 处理学生入团申请
- 维护社团成员和成员权限
- 发布、维护和审核活动成员
- 提交社团年审材料
- 查看社团相关申请和活动信息

### 超级管理员端

- 管理学生、管理员和社团信息
- 审核社团和活动申请
- 管理年审记录和反馈
- 批量导入用户数据
- 查看社团人数、活动人数等统计数据

## 技术栈

### 后端

- Java 16
- Spring Boot 2.5.3
- Spring Security
- MyBatis / MyBatis-Plus
- MySQL
- Redis
- Kafka
- JWT
- Apache POI
- JaCoCo

### 前端

- Vue 2.6.10
- Vue Router
- Vuex
- Element UI
- Axios
- ECharts（超级管理员端）
- Vue CLI 4

## 本地运行

### 后端

```bash
cd club_springboot
mvn test
mvn spring-boot:run
```

后端默认端口：`8081`

常用环境变量：

```bash
DB_URL=jdbc:mysql://localhost:3306/club
DB_USERNAME=root
DB_PASSWORD=
JWT_SECRET=your-secret
```

默认配置中 Redis 使用 `127.0.0.1:6379`，Kafka 使用 `localhost:9092`。当前 Kafka 监听可通过配置关闭。

### 学生端

```bash
cd vue_student
npm install
npm run dev
```

### 社团管理员端

```bash
cd vue_admin
npm install
npm run dev
```

### 超级管理员端

```bash
cd vue_superadmin
npm install
npm run dev
```

三个前端开发环境默认请求后端地址：`http://localhost:8081`

## 构建命令

```bash
cd club_springboot && mvn package
cd vue_student && npm run build:prod
cd vue_admin && npm run build:prod
cd vue_superadmin && npm run build:prod
```

## 后端结构

```text
club_springboot/src/main/java/com/chinahitech/shop
├── aop          # 日志、限流等切面
├── bean         # 实体和 DTO
├── config       # 安全、Redis、MyBatis 等配置
├── controller   # 接口层
├── exception    # 统一异常和业务异常
├── mapper       # MyBatis Mapper
├── mq           # Kafka 消息生产和消费
├── service      # 业务服务
└── utils        # 通用工具
```

## 测试

后端已配置 JUnit、Mockito 和 JaCoCo：

```bash
cd club_springboot
mvn test
```

测试报告生成在：

```text
club_springboot/target/site/jacoco/
```

## 说明

本项目为校园社团管理系统实践项目。若需要部署到生产环境，请根据实际情况修改数据库、Redis、Kafka、文件上传目录、JWT 密钥和前端接口地址等配置。
