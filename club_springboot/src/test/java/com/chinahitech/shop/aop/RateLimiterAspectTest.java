package com.chinahitech.shop.aop;

import com.chinahitech.shop.exception.RateLimitException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RateLimiterAspect unit tests")
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

    @BeforeEach
    void setUp() {
        // 单元测试不会读取 Spring @Value，这里显式模拟本地默认的 Redis 异常放行策略。
        ReflectionTestUtils.setField(rateLimiterAspect, "failOpen", true);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doBefore - over limit throws exception")
    void testDoBefore_OverLimit_ThrowException() throws NoSuchMethodException {
        // Given - script rejects the request
        Method method = DemoApi.class.getDeclaredMethod("limited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        mockMethod(method);
        whenScriptReturns(0L);

        // When & Then - rate limit exception is thrown
        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> rateLimiterAspect.doBefore(joinPoint, rateLimiter)
        );
        assertEquals("too fast", exception.getMessage());
    }

    @Test
    @DisplayName("executeSlidingWindowScript - Redis exception allows request")
    void testExecuteSlidingWindowScript_RedisException_ReturnAllowCode() {
        // Given - Redis command fails
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new RuntimeException("redis down"));

        // When - executing script
        Long result = rateLimiterAspect.executeSlidingWindowScript("rate:test", 10L, 1L);

        // Then - request is allowed so Redis outages do not block login or normal APIs
        assertEquals(1L, result);
    }

    @Test
    @DisplayName("executeSlidingWindowScript - fail closed rethrows Redis exception")
    void testExecuteSlidingWindowScript_FailClosed_RethrowException() {
        // Given - production can disable fail-open when Redis is required for strict rate limiting
        ReflectionTestUtils.setField(rateLimiterAspect, "failOpen", false);
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenThrow(new RuntimeException("redis down"));

        // When & Then - Redis exceptions are propagated when fail-open is disabled
        assertThrows(
                RuntimeException.class,
                () -> rateLimiterAspect.executeSlidingWindowScript("rate:test", 10L, 1L)
        );
    }

    @Test
    @DisplayName("rate key - IP type uses first forwarded IP")
    void testDoBefore_IpType_UseForwardedForFirstIp() throws NoSuchMethodException {
        // Given - forwarded IP header
        Method method = DemoApi.class.getDeclaredMethod("ipLimited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        bindRequest("X-Forwarded-For", "10.0.0.1, 10.0.0.2", null);
        mockMethod(method);
        whenScriptReturns(1L);

        // When - checking rate limit
        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        // Then - key uses first IP
        assertCapturedKeyEquals("rate_limit:" + DemoApi.class.getName() + ":ipLimited:ip:10.0.0.1");
    }

    @Test
    @DisplayName("rate key - USER type uses security context principal")
    void testDoBefore_UserType_UseSecurityContextUserId() throws NoSuchMethodException {
        // Given - spoofed header and authenticated principal
        Method method = DemoApi.class.getDeclaredMethod("userLimited");
        RateLimiter rateLimiter = method.getAnnotation(RateLimiter.class);
        bindRequest("X-User-Id", "spoofed-user", null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u1001", null)
        );
        mockMethod(method);
        whenScriptReturns(1L);

        // When - checking rate limit
        rateLimiterAspect.doBefore(joinPoint, rateLimiter);

        // Then - key ignores spoofed header and uses authenticated user
        assertCapturedKeyEquals("rate_limit:" + DemoApi.class.getName() + ":userLimited:user:u1001");
    }

    @Test
    @DisplayName("repeat limit - annotation path executes script")
    void testDoBeforeRepeat_Allowed_ExecuteScript() throws NoSuchMethodException {
        // Given - RepeatLimit annotation
        Method method = DemoApi.class.getDeclaredMethod("repeatLimited");
        RepeatLimit repeatLimit = method.getAnnotation(RepeatLimit.class);
        bindRequest("X-Forwarded-For", "127.0.0.1", null);
        mockMethod(method);
        whenScriptReturns(1L);

        // When - checking repeat limit
        rateLimiterAspect.doBeforeRepeat(joinPoint, repeatLimit);

        // Then - script is executed with repeat key
        assertCapturedKeyEquals("repeat_limit:" + DemoApi.class.getName() + ":repeatLimited:ip:127.0.0.1");
    }

    @Test
    @DisplayName("报名入口在全局失败关闭时允许数据库降级，仍拒绝正常限流")
    void testDoBeforeRepeat_AdmissionRedisFailure_OnlyAllowsDegradation() throws NoSuchMethodException {
        // Given
        ReflectionTestUtils.setField(rateLimiterAspect, "failOpen", false);
        Method method = DemoApi.class.getDeclaredMethod("admission");
        var rule = method.getAnnotation(RepeatLimit.class);
        bindRequest(null, null, null);
        mockMethod(method);
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new org.springframework.data.redis.RedisConnectionFailureException("offline"))
                .thenReturn(0L);

        // When / Then
        assertDoesNotThrow(() -> rateLimiterAspect.doBeforeRepeat(joinPoint, rule));
        assertThrows(RateLimitException.class, () -> rateLimiterAspect.doBeforeRepeat(joinPoint, rule));
    }

    private void mockMethod(Method method) {
        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
    }

    private void whenScriptReturns(Long result) {
        when(stringRedisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(result);
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
        @RateLimiter(limit = 1, window = 5, message = "too fast", type = RateLimiter.LimitType.METHOD)
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

        @RepeatLimit(failOpenOnRedisError = true)
        void admission() {
        }
    }
}
