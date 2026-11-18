package com.chinahitech.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;

/** 显式路由避免自动暴露注册中心的内部服务；非幂等写请求不配置自动重试。 */
@Configuration
public class GatewayRoutes {
    @Bean
    public HttpClientCustomizer disableTransportRetry() {
        // Reactor Netty 的连接重置重试同样可能重复提交报名；写请求故障应交由调用方明确处理。
        return client -> client.disableRetry(true);
    }
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
            @Value("${app.gateway.rate-limit-enabled:false}") boolean rateLimitEnabled,
            RedisRateLimiter limiter, KeyResolver remoteAddressKey) {
        return builder.routes()
                // AI SSE 允许长时间输出；不能套用普通 API 的20秒整流熔断超时。
                .route("club-knowledge", route -> route.path("/api/knowledge/**")
                        .filters(filters -> filters.stripPrefix(1).circuitBreaker(config -> config
                                .setName("knowledgeBackend").setFallbackUri("forward:/internal/unavailable")))
                        .metadata("response-timeout", 190000L).uri("lb://club-backend"))
                .route("club-api", route -> route.path("/api/**")
                        .filters(filters -> {
                            filters.stripPrefix(1);
                            filters.circuitBreaker(config -> config.setName("clubBackend")
                                    .setFallbackUri("forward:/internal/unavailable"));
                            if (rateLimitEnabled) {
                                filters.requestRateLimiter(config -> config.setRateLimiter(limiter)
                                        .setKeyResolver(remoteAddressKey));
                            }
                            return filters;
                        }).uri("lb://club-backend"))
                .route("club-uploads", route -> route.path("/upload/**")
                        .uri("lb://club-backend"))
                .build();
    }

    @Bean
    public RedisRateLimiter gatewayRateLimiter(
            @Value("${app.gateway.replenish-rate:30}") int rate,
            @Value("${app.gateway.burst-capacity:60}") int burst) {
        return new RedisRateLimiter(rate, burst);
    }

    @Bean
    public KeyResolver remoteAddressKey() {
        // 不信任客户端可伪造的 X-Forwarded-For；入口代理需单独配置可信代理边界。
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress() == null ? "unknown"
                : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
