package com.chinahitech.shop.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.CodeSignature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("请求日志切面单元测试")
class LogAspectTest {

    private final LogAspect logAspect = new LogAspect();

    @Test
    @DisplayName("关闭详细请求日志 - 直接执行控制器且保持返回值")
    void testDoAround_RequestLogDisabled_ProceedWithoutSerialization() throws Throwable {
        // Given - 生产环境关闭逐请求详细日志，切面不再序列化请求和响应对象。
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("success");
        logAspect.setRequestLogEnabled(false);

        // When - 请求经过日志切面。
        Object result = logAspect.doAround(joinPoint);

        // Then - 业务方法仍执行一次并原样返回结果。
        assertEquals("success", result);
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("过滤日志入参 - 根据参数名脱敏登录账号和密码")
    void testFilterArgs_LoginSensitiveNames_MaskSensitiveValues() {
        // Given - 登录接口的基础类型入参没有对象字段名，必须依赖方法参数名识别敏感数据。
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "login",
                new String[]{"userId", "password"},
                new Object[]{"clubadmin", "ClubAdmin@2026#Baituan"}
        );

        // When - 过滤准备写入日志的参数。
        Object[] filtered = logAspect.filterArgs(joinPoint);

        // Then - 账号和密码都不应以明文形式进入日志。
        assertArrayEquals(new Object[]{"***", "***"}, filtered);
    }

    @Test
    @DisplayName("过滤日志入参 - 无参数名时登录第二个参数按密码脱敏")
    void testFilterArgs_LoginWithoutParameterNames_MaskSecondArgument() {
        // Given - 某些编译配置不会保留参数名，此时仍要保护登录密码。
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "login",
                new String[0],
                new Object[]{"clubadmin", "ClubAdmin@2026#Baituan"}
        );

        // When - 过滤准备写入日志的参数。
        Object[] filtered = logAspect.filterArgs(joinPoint);

        // Then - 至少保证登录密码不泄露，账号是否脱敏取决于参数名是否可用。
        assertArrayEquals(new Object[]{"clubadmin", "***"}, filtered);
    }

    @Test
    @DisplayName("过滤日志入参 - 普通业务参数保持可排查性")
    void testFilterArgs_NormalBusinessArgument_KeepValue() {
        // Given - 非敏感参数需要保留，方便线上排查具体业务请求。
        ProceedingJoinPoint joinPoint = mockJoinPoint(
                "getMyActivity",
                new String[]{"groupName", "pageNum"},
                new Object[]{"AI Innovation Lab", 1}
        );

        // When - 过滤准备写入日志的参数。
        Object[] filtered = logAspect.filterArgs(joinPoint);

        // Then - 普通业务参数不应被误脱敏。
        assertArrayEquals(new Object[]{"AI Innovation Lab", 1}, filtered);
    }

    private ProceedingJoinPoint mockJoinPoint(String methodName, String[] parameterNames, Object[] args) {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        CodeSignature signature = mock(CodeSignature.class);
        when(joinPoint.getArgs()).thenReturn(args);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn(methodName);
        when(signature.getParameterNames()).thenReturn(parameterNames);
        return joinPoint;
    }
}
