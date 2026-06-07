# 生产部署说明

本文档说明数字百团项目的推荐生产部署方式。示例以一台 Linux 服务器、Nginx 托管前端、Spring Boot Jar 运行后端为基础。

## 部署结构

推荐将三个前端分别部署到独立域名或子域名，后端只暴露给 Nginx 反向代理：

| 服务 | 示例域名 | 说明 |
| --- | --- | --- |
| 学生端 | `student.example.com` | `vue_student` 构建产物 |
| 社团管理员端 | `admin.example.com` | `vue_admin` 构建产物 |
| 超级管理员端 | `superadmin.example.com` | `vue_superadmin` 构建产物 |
| 后端 API | 仅内网 `127.0.0.1:8081` | Nginx 通过 `/api/` 反代 |

如果暂时只有一个域名，也可以先部署其中一个前端到根路径，另外两个使用不同二级域名或端口。当前三个前端 `publicPath` 均为 `/`，不建议直接放到同一域名的不同子路径下。

## 后端环境变量

复制 `deploy/env/backend.prod.env.example` 为服务器上的环境变量文件，并按实际值修改。生产启动必须至少配置：

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_HOST`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `UPLOAD_DIR`

`JWT_SECRET` 建议使用 32 字节以上随机字符串；生产数据库不要使用 `root` 账号。

## 后端构建与启动

```bash
cd club_springboot
mvn clean test
mvn clean package -DskipTests
java -jar target/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

生产环境建议使用 systemd 或容器托管后端进程，并将环境变量注入运行环境。

## 前端构建

三个前端生产环境均使用 `/api` 作为后端 API 前缀，由 Nginx 转发到后端。

```bash
cd vue_student && npm install && npm run build:prod
cd ../vue_admin && npm install && npm run build:prod
cd ../vue_superadmin && npm install && npm run build:prod
```

将三个 `dist/` 分别发布到服务器目录，例如：

```text
/var/www/baituan/student
/var/www/baituan/admin
/var/www/baituan/superadmin
```

## Nginx

参考 `deploy/nginx/baituan.conf.example`。上线前需要替换：

- `student.example.com`
- `admin.example.com`
- `superadmin.example.com`
- SSL 证书路径
- 前端静态文件目录
- 后端地址

示例 Nginx 配置会传递 `X-Forwarded-Proto`、`X-Forwarded-Host` 等请求头；后端生产配置已启用
`server.forward-headers-strategy=framework`，用于让上传接口返回公网域名和 HTTPS 协议下的文件地址。

## 健康检查

后端已开放健康检查接口：

```text
GET /actuator/health
```

Nginx 或部署平台可使用该接口判断后端是否存活。

## 上线前检查清单

- 数据库已导入 `club_finalversion.sql`
- MySQL、Redis、Kafka 地址均使用生产配置
- `JWT_SECRET` 已配置且不同于开发环境
- `CORS_ALLOWED_ORIGINS` 只包含真实前端域名
- 上传目录 `UPLOAD_DIR` 已创建并可被后端进程读写
- Nginx 已开启 HTTPS
- 后端日志目录存在且有轮转策略
- 前端三个 `dist/` 均来自最新代码构建
- `mvn test` 通过
