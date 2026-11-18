package com.chinahitech.shop.config;

import com.chinahitech.shop.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** 验证新增平台入口不会向匿名用户或普通业务角色暴露。 */
@SpringBootTest(classes = {SecurityConfigTest.TestApplication.class, SecurityConfigTest.TestEndpoints.class})
@AutoConfigureMockMvc
@Import(OpenApiConfig.class)
@ActiveProfiles("observability")
@AutoConfigureMetrics
@DisplayName("平台入口权限测试")
class PlatformEndpointSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("接口文档和指标 - 超级管理员可以读取")
    void testPlatformEndpoints_TopManager_Ok() throws Exception {
        // Given - 超级管理员凭据
        String previous = System.getProperty("jwt.secret");
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
        try {
            String token = JwtUtils.generateToken("1", JwtUtils.ROLE_TOP_MANAGER);
            // When / Then - 真实 OpenAPI 与 Actuator 端点正常提供内容
            mockMvc.perform(get("/v3/api-docs").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.info.title").value("数字百团 API"));
            mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        } finally {
            if (previous == null) System.clearProperty("jwt.secret");
            else System.setProperty("jwt.secret", previous);
        }
    }

    @Test
    @DisplayName("接口文档和指标 - 匿名请求被拒绝")
    void testPlatformEndpoints_Anonymous_Unauthorized() throws Exception {
        // Given - 新增平台入口
        String[] paths = {"/v3/api-docs", "/swagger-ui/index.html", "/actuator/prometheus"};
        // When / Then - 未认证请求不能读取管理信息
        for (String path : paths) {
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    @DisplayName("接口文档和指标 - 学生和社团管理员被拒绝")
    void testPlatformEndpoints_BusinessRoles_Forbidden() throws Exception {
        // Given - 与现有测试一致的 JWT 签名环境
        String previous = System.getProperty("jwt.secret");
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
        try {
            for (String role : new String[]{JwtUtils.ROLE_STUDENT, JwtUtils.ROLE_MANAGER}) {
                String token = JwtUtils.generateToken("1", role);
                // When / Then - 已登录业务用户仍不能读取平台入口
                for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html", "/actuator/prometheus"}) {
                    mockMvc.perform(get(path).header("X-Token", token)).andExpect(status().isForbidden());
                }
            }
        } finally {
            if (previous == null) System.clearProperty("jwt.secret");
            else System.setProperty("jwt.secret", previous);
        }
    }
}
