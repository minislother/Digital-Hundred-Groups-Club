package com.chinahitech.gateway;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 熔断时返回兼容现有前端的失败结构，不把失败伪装成成功。 */
@RestController
public class UnavailableController {
    private static final Logger log = LoggerFactory.getLogger(UnavailableController.class);

    @RequestMapping("/internal/unavailable")
    public ResponseEntity<Map<String, Object>> unavailable(ServerWebExchange exchange) {
        Throwable failure = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
        if (failure != null) {
            // Avoid logging tokens, query parameters or exception messages containing request data.
            log.warn("Gateway fallback, cause={}", failure.getClass().getSimpleName());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "success", false, "code", 50300, "message", "服务暂不可用，请稍后重试"));
    }
}
