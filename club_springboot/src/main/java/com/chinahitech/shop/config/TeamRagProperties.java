package com.chinahitech.shop.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

/** TeamRag 的部署身份与社团项目映射仅在服务端配置，不接受前端覆盖。 */
@Data
@Component
@ConfigurationProperties(prefix = "app.teamrag")
public class TeamRagProperties {
    private boolean enabled;
    private java.util.Set<String> readOnlyProjects = new java.util.HashSet<>();
    private String baseUrl = "http://127.0.0.1:8082";
    private String tokenSecret;
    private String tenantId = "baituan";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 45000;
    private Map<Integer, String> projects = new HashMap<>();
}
