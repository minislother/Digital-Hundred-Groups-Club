# 生产部署说明

本文档说明数字百团项目的推荐生产部署方式。当前项目已经补充 Docker Compose 部署方案，同时保留传统 Jar + Nginx 部署思路。

## 一、Docker Compose 推荐方案

Docker Compose 会编排以下服务：

| 服务 | 容器名 | 说明 |
| --- | --- | --- |
| `mysql` | `baituan-mysql` | MySQL 8.4 LTS，首次启动会导入数据库和生产索引 |
| `redis` | `baituan-redis` | Redis 7.4，开启 AOF、密码、内存上限和 LRU 淘汰 |
| `backend` | `baituan-backend` | Spring Boot 后端，使用 `prod` profile |
| `frontend` | `baituan-frontend` | Nginx 1.30 托管统一入口、学生端、管理员端、超级管理员端 |
| `kafka` | `baituan-kafka` | Apache Kafka 3.9，可选服务，使用 `--profile mq` 才启动 |

MySQL、Redis、Kafka 和后端只加入 Docker 内部网络，不映射宿主机端口。只有前端 Nginx 端口绑定到 `127.0.0.1`，正式服务器应再通过宿主机 Nginx、云负载均衡或网关提供 HTTPS。

## 二、首次部署步骤

1. 安装并启动 Docker Desktop。

2. 自动生成本机 `.env` 和随机密钥：

```powershell
.\deploy\initialize-production-env.ps1
```

脚本不会覆盖已有 `.env`。如果确认需要轮换全部本机密钥，可显式执行 `-Force`。

3. 检查 `.env`，正式部署时至少确认以下值：

```text
MYSQL_ROOT_PASSWORD
MYSQL_PASSWORD
REDIS_PASSWORD
JWT_SECRET
```

初始化脚本会生成随机密码和 48 字节 JWT 密钥，但域名、CORS、资源上限仍需按生产服务器调整。

4. 构建并启动基础服务：

```powershell
docker compose up -d --build mysql redis backend frontend
```

5. 如需启用 Kafka：

```powershell
docker compose --profile mq up -d --build
```

6. 查看服务状态：

```powershell
docker compose ps
docker compose logs -f backend
```

## 三、访问地址

本地 Docker 部署默认端口：

| 应用 | 地址 |
| --- | --- |
| 统一入口 | `http://localhost:9527` |
| 学生端 | `http://localhost:9533` |
| 管理员端 | `http://localhost:9531` |
| 超级管理员端 | `http://localhost:9532` |
| 后端健康检查 | `http://localhost:9527/health` |

后端的 `8081` 端口不会直接暴露给宿主机，健康检查由前端 Nginx 安全转发。

## 四、生产域名建议

正式上线建议使用 HTTPS 和真实域名，例如：

| 应用 | 示例域名 |
| --- | --- |
| 统一入口 | `https://club.example.com` |
| 学生端 | `https://student.example.com` |
| 管理员端 | `https://admin.example.com` |
| 超级管理员端 | `https://superadmin.example.com` |
| API | 通过 Nginx `/api/` 反向代理，不建议直接暴露后端端口 |

如果部署到真实服务器，请同步修改 `.env` 中的：

```text
FRONTEND_STUDENT_URL
FRONTEND_ADMIN_URL
FRONTEND_SUPERADMIN_URL
CORS_ALLOWED_ORIGINS
```

## 五、数据与文件持久化

Compose 已配置以下 Docker volume：

| Volume | 用途 |
| --- | --- |
| `mysql_data` | MySQL 数据 |
| `redis_data` | Redis AOF 数据 |
| `backend_uploads` | 后端上传文件 |
| `backend_logs` | 后端日志 |
| `kafka_data` | Kafka 数据 |

生产环境务必定期备份 `mysql_data` 和 `backend_uploads`。

当前 Compose 还设置了容器内存、CPU、进程数和日志轮转上限。这些值是单机起步值，上线后应根据监控结果调整 `.env` 中的 `*_MEMORY_LIMIT`、`*_CPU_LIMIT`、数据库连接池和 Redis 内存参数。

## 六、后端生产配置

后端生产 profile 位于：

```text
club_springboot/src/main/resources/application-prod.properties
```

当前生产默认值已经包含：

- `server.forward-headers-strategy=framework`，适配 Nginx/HTTPS 反向代理。
- HikariCP 数据库连接池参数。
- `app.rate-limit.fail-open=false`，生产环境 Redis 限流异常时默认拒绝，避免限流失效。
- `management.health.redis.enabled=true`，健康检查包含 Redis 状态。
- 日志输出目录通过 `LOG_PATH` 配置。

## 七、传统 Jar + Nginx 部署

如果不用 Docker，也可以使用传统部署方式：

```bash
cd club_springboot
mvn clean test
mvn clean package -DskipTests
java -jar target/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

三个业务前端和统一入口分别构建：

```bash
npm run build:frontends
```

再将产物发布到 Nginx 静态目录，并参考：

```text
deploy/nginx/baituan.conf.example
```

## 八、上线前检查清单

- `.env` 中所有默认密码已经替换。
- `JWT_SECRET` 已替换为高强度随机值。
- MySQL 不使用 root 账号供后端连接。
- Redis 已设置密码。
- MySQL、Redis、Kafka、后端没有直接映射公网端口。
- `CORS_ALLOWED_ORIGINS` 只包含真实前端域名。
- 后端 `mvn test` 通过。
- Docker Compose 配置校验通过：`docker compose config`。
- 前端生产构建通过。
- `backend_uploads` 和数据库已纳入备份策略。
- 对外访问启用 HTTPS。
- 服务器安全组只开放必要端口。

## 九、Docker Desktop 本机排障

如果 `docker desktop status` 显示 `stopped`，或 `docker info` 提示无法连接 Docker Engine，优先检查：

1. Windows 是否启用虚拟化平台、WSL 和 Hyper-V 相关组件。
2. BIOS/UEFI 是否启用 CPU 虚拟化。
3. Docker Desktop 是否以管理员授权完成首次初始化。
4. 是否安装了 WSL2 内核，并且 `wsl --version` 可正常输出。

项目已提供管理员修复脚本：

```powershell
Start-Process powershell.exe -Verb RunAs -Wait -ArgumentList @(
  '-NoProfile',
  '-ExecutionPolicy', 'Bypass',
  '-File', '.\deploy\enable-docker-windows-features.ps1'
)
```

脚本会启用 WSL、Virtual Machine Platform，并将 hypervisor 启动方式设为 `auto`。执行后需要重启电脑，再重新打开 Docker Desktop。
