package com.chinahitech.shop.aop;

import com.chinahitech.shop.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("接口限流切面单元测试")
@ExtendWith(MockitoExtension.class)
class RateLimiterAspectTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @InjectMocks
    private RateLimiterAspect rateLimiterAspect;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("限流检查 - 超过阈值抛出异常")
    void testDoBefore_OverLimit_ThrowException() throws NoSuchMethodException {
        // Given - 准备测试数据和Mock行为
        Method method = DemoApi.class.getDeclaredMethod("limited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(0L);

        // When & Then - 执行被测试方法并验证异常
        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> rateLimiterAspect.doBefore(joinPoint, rateLimiter)
        );
        assertEquals("访问过快", exception.getMessage());
    }

    @Test
    @DisplayName("限流脚本 - Redis异常时返回拒绝")
    void testExecuteSlidingWindowScript_RedisException_ReturnZero() {
        // Given - 准备测试数据和Mock行为
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new RuntimeException("redis down"));

        // When - 执行被测试方法
        Long result = rateLimiterAspect.executeSlidingWindowScript("rate:test", 10L, 1L);

        // Then - 验证结果
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("限流Key - 按IP使用转发头首个地址")
    void testDoBefore_IpType_UseForwardedForFirstIp() throws NoSuchMethodException {
        // Given - 准备测试数据和Mock行为
        Method method = DemoApi.class.getDeclaredMethod("ipLimited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        bindRequest("X-Forwarded-For", "10.0.0.1, 10.0.0.2", null);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        whenScriptAllowed();

        // When - 执行被测试方法
        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        // Then - 验证限流key包含首个转发IP
        assertCapturedKeyEquals("rate_limit:" + DemoApi.class.getName() + ":ipLimited:ip:10.0.0.1");
    }

    @Test
    @DisplayName("限流Key - 按用户优先使用请求头用户ID")
    void testDoBefore_UserType_UseHeaderUserId() throws NoSuchMethodException {
        // Given - 准备测试数据和Mock行为
        Method method = DemoApi.class.getDeclaredMethod("userLimited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        bindRequest("X-User-Id", "u1001", null);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        whenScriptAllowed();

        // When - 执行被测试方法
        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        // Then - 验证限流key包含用户ID
        assertCapturedKeyEquals("rate_limit:" + DemoApi.class.getName() + ":userLimited:user:u1001");
    }

    @Test
    @DisplayName("重复提交限流 - 使用RepeatLimit注解执行")
    void testDoBeforeRepeat_Allowed_ExecuteScript() throws NoSuchMethodException {
        // Given - 准备测试数据和Mock行为
        Method method = DemoApi.class.getDeclaredMethod("repeatLimited");
        RepeatLimit repeatLimit = method.getAnnotation(RepeatLimit.class);
        bindRequest("X-Forwarded-For", "127.0.0.1", null);
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        whenScriptAllowed();

        // When - 执行被测试方法
        rateLimiterAspect.doBeforeRepeat(joinPoint, repeatLimit);

        // Then - 验证脚本被执行
        assertCapturedKeyEquals("repeat_limit:" + DemoApi.class.getName() + ":repeatLimited:ip:127.0.0.1");
    }

    private void whenScriptAllowed() {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(1L);
    }

    private void bindRequest(String headerName, String headerValue, String userIdParam) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        if (headerName != null) {
            request.addHeader(headerName, headerValue);
        }
        if (userIdParam != null) {
            request.setParameter("userId", userIdParam);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void assertCapturedKeyEquals(String expectedKey) {
        ArgumentCaptor<List> keysCaptor = ArgumentCaptor.forClass(List.class);
        verify(stringRedisTemplate).execute(
                any(DefaultRedisScript.class),
                keysCaptor.capture(),
                anyString(),
                anyString(),
                anyString()
        );
        assertEquals(expectedKey, keysCaptor.getValue().get(0));
    }

    private static class DemoApi {
        @RateLimiter(limit = 1, window = 5, message = "访问过快", type = RateLimiter.LimitType.METHOD)
        void limited() {
        }

        @RateLimiter(type = RateLimiter.LimitType.IP)
        void ipLimited() {
        }

        @RateLimiter(type = RateLimiter.LimitType.USER)
        void userLimited() {
        }

        @RepeatLimit
        void repeatLimited() {
        }
    }
}
