package com.chinahitech.shop.config;

import com.chinahitech.shop.utils.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.servlet.ServletException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("JWT认证过滤器单元测试")
class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter();

    @BeforeEach
    void setUp() {
        System.setProperty("jwt.secret", "test-jwt-secret-test-jwt-secret-test-jwt-secret");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        System.clearProperty("jwt.secret");
    }

    @Test
    @DisplayName("JWT认证 - X-Token有效时写入认证上下文")
    void testDoFilterInternal_ValidXToken_SetAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Token", JwtUtils.generateToken("1001"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("1001", authentication.getPrincipal());
    }

    @Test
    @DisplayName("JWT认证 - token无效时不写入认证上下文")
    void testDoFilterInternal_InvalidToken_NoAuthentication() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Token", "bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
