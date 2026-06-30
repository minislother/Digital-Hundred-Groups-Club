# 数字百团校园社团管理系统

数字百团是一个面向高校社团场景的全栈管理系统，覆盖学生、社团管理员和超级管理员三类用户。项目采用前后端分离架构，包含 Spring Boot 后端、三套 Vue 3 网页端、一个 Android 原生端，以及可复用的前端共享模块。

系统主要解决社团展示、入团申请、活动报名、活动审核、社团审核、成员维护、资料上传、批量导入和数据统计等校园社团管理流程。

## 项目结构

| 目录/文件 | 说明 |
| --- | --- |
| `club_springboot/` | Spring Boot 后端服务，提供登录鉴权、社团、活动、申请、审核、上传等接口 |
| `vue_student/` | 学生网页端，面向社团浏览、活动报名、个人中心等场景 |
| `vue_admin/` | 社团管理员网页端，面向社团资料、成员、申请、活动管理等场景 |
| `vue_superadmin/` | 超级管理员网页端，面向学生、管理员、社团、活动、年审和统计管理 |
| `frontend-shared/` | 三个网页端共享的前端工具模块 |
| `baituan_android/` | Android 原生 App，支持三类身份入口和移动端工作台 |
| `deploy/` | 生产部署说明、环境变量示例和 Nginx 示例配置 |
| `club_finalversion.sql` | MySQL 初始化脚本 |
| `AGENTS.md` | 面向 Codex/AI 协作的项目说明 |

## 功能概览

### 学生端

- 浏览、搜索社团和活动
- 查看社团详情、活动详情、封面和附件信息
- 申请加入社团、报名活动
- 查看我的社团、我的活动、我的申请
- 维护个人资料

### 社团管理员端

- 管理社团简介、封面图和附件
- 处理学生入团申请
- 维护社团成员和成员状态
- 发布、查看和管理社团活动
- 查看活动报名和社团相关审核信息

### 超级管理员端

- 管理学生、社团管理员和社团信息
- 审核社团创建申请和活动申请
- 批量导入用户数据
- 管理年审记录和反馈
- 查看社团、活动、成员等统计数据

### Android 端

- 启动后先选择身份：学生、社团管理员、超级管理员
- 学生端支持登录注册、社团/活动浏览、详情查看、申请和报名
- 管理员端和超级管理员端提供移动工作台，展示各自需要处理的数据和操作入口
- 使用 DataStore 保存登录 token 和身份，网络层自动携带认证信息

## 技术栈

### 后端

- Java 17 本地运行环境，Maven 编译目标为 Java 16
- Spring Boot 2.5.3
- Spring Security + JWT
- MyBatis / MyBatis-Plus
- MySQL
- Redis
- Kafka
- Apache POI
- JUnit、Mockito、JaCoCo

### 网页端

- Vue 3
- Vue Router 4
- Vuex 4
- Element Plus
- Axios
- Vue CLI 5
- ESLint

### Android

- Kotlin
- Jetpack Compose
- MVVM + Repository 分层
- Hilt
- Retrofit + OkHttp
- Gson
- DataStore
- Gradle / Android Gradle Plugin

## 环境要求

- JDK 17
- Maven 3.8+
- Node.js 和 npm
- MySQL 8.x
- Redis
- Kafka 可选，默认业务监听关闭
- Android Studio 和 Android SDK Platform 35

本项目后端默认端口为 `8081`，三个网页端开发模式默认请求 `http://localhost:8081`。

## 数据库初始化

先创建数据库：

```sql
CREATE DATABASE club DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

再导入初始化脚本：

```bash
mysql -u root -p club < club_finalversion.sql
```

## 后端运行

后端配置文件位于：

```text
club_springboot/src/main/resources/application.properties
```

本地运行前至少需要配置数据库密码和 JWT 密钥：

```powershell
$env:DB_PASSWORD="你的 MySQL 密码"
$env:JWT_SECRET="请替换为足够长的随机密钥"
```

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | 后端端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/club...` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | 无默认值 | MySQL 密码，必须显式配置 |
| `JWT_SECRET` | 无默认值 | JWT 签名密钥 |
| `UPLOAD_DIR` | `upload/` | 文件上传目录 |
| `REDIS_HOST` | `127.0.0.1` | Redis 地址 |
| `KAFKA_ENABLED` | `false` | 是否启用 Kafka 业务能力 |

启动后端：

```bash
cd club_springboot
mvn test
mvn spring-boot:run
```

健康检查：

```text
GET http://localhost:8081/actuator/health
```

## 网页端运行

首次运行前分别安装依赖：

```bash
cd vue_student && npm install
cd ../vue_admin && npm install
cd ../vue_superadmin && npm install
```

启动学生端：

```bash
cd vue_student
npm run dev
```

启动社团管理员端：

```bash
cd vue_admin
npm run dev
```

启动超级管理员端：

```bash
cd vue_superadmin
npm run dev
```

也可以在根目录使用聚合构建脚本：

```bash
npm run build:frontends
```

## Android 端运行

使用 Android Studio 打开：

```text
baituan_android/
```

调试环境默认后端地址：

```text
http://10.0.2.2:8081/
```

这是 Android 模拟器访问电脑本机 `localhost` 的标准地址。真机调试时需要把 `BAITUAN_DEBUG_BASE_URL` 改为电脑局域网 IP，例如：

```properties
BAITUAN_DEBUG_BASE_URL=http://192.168.1.10:8081/
```

生产环境地址通过 Gradle 属性配置：

```properties
BAITUAN_RELEASE_BASE_URL=https://你的生产后端域名/
```

Android 端不依赖手写 `BuildConfig`，后端地址由 Gradle 写入资源后通过 `AppConfig` 读取。

## 构建与测试

后端测试：

```bash
cd club_springboot
mvn test
```

网页端检查和构建：

```bash
cd vue_student && npm run lint && npm run build:prod
cd ../vue_admin && npm run lint && npm run build:prod
cd ../vue_superadmin && npm run lint && npm run build:prod
```

Android 测试、构建和 Lint：

```bash
cd baituan_android
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
```

Windows PowerShell 下可使用：

```powershell
cd baituan_android
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease :app:lintDebug
```

## 最近验证状态

当前代码已完成以下验证：

- 后端 `mvn test` 通过，136 个测试用例成功
- 三个网页端 ESLint 通过
- 三个网页端生产构建通过
- 使用 Playwright 对三个网页端登录页进行冒烟测试，通过输入框、标题和控制台错误检查
- Android `testDebugUnitTest`、`assembleDebug`、`assembleRelease`、`lintDebug` 通过

## 生产部署

推荐部署方式：

- Spring Boot 后端以 Jar、systemd 或容器方式运行
- Nginx 托管三个网页端构建产物
- Nginx 通过 `/api` 反向代理到后端
- MySQL、Redis 使用生产环境账号和独立密码
- 生产环境必须配置足够强度的 `JWT_SECRET`
- Android release 包使用 HTTPS 后端地址和正式签名证书

详细部署步骤见：

```text
deploy/production.md
```

## 上线前检查清单

- 已导入 `club_finalversion.sql`
- `DB_PASSWORD`、`JWT_SECRET` 已配置
- 后端 `mvn test` 通过
- 三个网页端 `npm run build:prod` 通过
- Android release 构建通过
- Redis、Kafka、上传目录按生产环境配置
- Nginx 已开启 HTTPS
- 前端接口地址和 Android release 后端地址已切换到生产域名
- 生产数据库不要使用 root 账号

## 说明

本项目是校园社团管理系统实践项目。若用于真实生产环境，请根据学校实际组织架构、权限边界、数据合规要求和部署环境继续完善安全策略、审计日志、备份策略和运维监控。
