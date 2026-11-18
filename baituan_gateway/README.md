# 数字百团网关

独立的 Spring Boot 4.0.9 / Spring Cloud 2025.1.4 应用，编译和运行要求 Java 17+。
业务后端已迁移到 Java 21 / Boot 4；网关仍可独立在 Java 17+ 运行，两者通过 HTTP 集成。

## 已实现能力

- Spring Cloud Gateway：显式 `/api/**` 与 `/upload/**` 路由；`/api` 前缀仅移除一次。
- Spring Cloud LoadBalancer：以 `lb://club-backend` 分配请求。
- Consul Discovery：cluster profile 仅使用健康实例，不自动暴露注册中心所有服务。
- Resilience4j CircuitBreaker：API 连接失败时返回 HTTP 503 和 `code=50300`，不伪造业务成功。
- Redis RequestRateLimiter：支持多网关共享额度；默认关闭，直接连接对端为限流 key。
- Actuator：默认仅暴露 health；后台鉴权仍在业务服务执行，网关原样传递 JWT 请求头。
- 写请求不设置 Gateway Retry，且显式关闭 Reactor Netty 的连接重置重试。

## 本地运行与验证

```powershell
mvn -B -f baituan_gateway/pom.xml package
java -jar baituan_gateway/target/baituan-gateway-1.0.0.jar
python baituan_gateway/scripts/smoke_test.py
```

默认网关为 8080，业务后端为 8081。例如原后端 `/activity/all` 对应网关 `/api/activity/all`。
不要直接把 TeamRag `/api/rag/qa` 暴露为公开路由；须经过数字百团的用户权限、project 与会话映射适配。

## 双后端部署

先按 `deploy/production.md` 准备 `.env`，再执行：

```powershell
docker compose -f docker-compose.yml -f docker-compose.cluster.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.cluster.yml up -d --build
```

链路：浏览器 → Nginx 最少连接分配 → 两个 Gateway → Consul 健康发现 → 轮询两个 backend → 共享 MySQL/Redis。
Nginx 使用 Docker DNS 动态解析网关副本；增加或替换网关无需把地址写死到配置。
现有端口和业务响应不变，两个后端共享上传卷。原单实例 Compose 仍可单独使用。
Consul 不发布宿主端口，单节点注册中心仅适合开发或初期部署，不具备注册中心高可用。
生产环境扩大规模前应部署受保护的 Consul 集群、ACL/TLS，并实测共享数据库及异步任务的并发行为。

限流通过 `GATEWAY_RATE_LIMIT_ENABLED=true` 启用；`app.gateway.replenish-rate` 和 `burst-capacity` 控制速率。
当前 Nginx 代理后的请求共享对端地址额度，因此集群配置默认关闭网关 IP 限流，保留后端现有业务限流。
不能仅启用该开关就认为获得按最终用户限流，需先定义可信代理或已验证身份的 key 策略。

Consul 在 10 秒健康检查后排除故障实例；发现与缓存存在传播时间，这段时间可能返回 503。
网关不会自动重放失败的报名请求。故障发生在写入之后时，客户端应先查询结果再决定是否重试。

## 验证边界

本机 smoke 脚本会启动两个临时 HTTP 服务和真实网关进程，检查轮询、路径和凭据透传、单次 POST、上传路由与故障响应。
这不替代真实 MySQL/Redis/Kafka/Consul 容器集成测试，也不能证明报名在多实例压力下不会超卖。

版本参考：[Spring Cloud 官方兼容矩阵](https://spring.io/projects/spring-cloud/)。
