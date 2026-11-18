package com.chinahitech.shop.aop;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Aspect
@Component
public class LogAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LogAspect.class);
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);
    private static final String MASKED_VALUE = "***";
    private static final String[] SENSITIVE_FIELDS = {
            "password", "salt", "valicode", "validateCode", "token",
            "userId", "userNumber", "studentId", "stuNumber", "stunumber",
            "phone", "email", "userName", "username"
    };
    private static final Set<String> SENSITIVE_FIELD_NAMES = new HashSet<>(Arrays.asList(
            "password", "pwd", "salt", "valicode", "validatecode", "token",
            "userid", "usernumber", "studentid", "stunumber", "phone", "email", "userName", "username"
    ));

    /**
     * 控制是否记录 Controller 的完整请求和响应。生产环境关闭后只跳过详细访问日志，
     * 不影响异常日志、业务处理和接口返回，可避免序列化大对象与同步日志 I/O 降低吞吐量。
     */
    @Value("${app.request-log.enabled:true}")
    private boolean requestLogEnabled = true;

    @Pointcut("execution(public * com.chinahitech..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!requestLogEnabled) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        MDC.put("LOG_ID", String.valueOf(SNOWFLAKE.nextId()));

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes == null ? null : attributes.getRequest();
        Signature signature = joinPoint.getSignature();

        LOG.info("------------- request start -------------");
        if (request != null) {
            LOG.info("request: {} {}", request.getRequestURL(), request.getMethod());
            LOG.info("remote: {}", request.getRemoteAddr());
        }
        LOG.info("handler: {}.{}", signature.getDeclaringTypeName(), signature.getName());
        LOG.info("args: {}", JSONObject.toJSONString(filterArgs(joinPoint), sensitiveFilter()));

        try {
            Object result = joinPoint.proceed();
            LOG.info("result: {}", JSONObject.toJSONString(result, sensitiveFilter()));
            return result;
        } finally {
            LOG.info("------------- request end cost:{} ms -------------", System.currentTimeMillis() - startTime);
            MDC.clear();
        }
    }

    /** 供单元测试切换详细请求日志，避免测试依赖 Spring 容器。 */
    void setRequestLogEnabled(boolean requestLogEnabled) {
        this.requestLogEnabled = requestLogEnabled;
    }

    /**
     * 过滤 Controller 入参日志：文件/Servlet 对象不打印，密码、验证码、token 等敏感基础类型参数统一脱敏。
     */
    Object[] filterArgs(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        String[] parameterNames = parameterNames(joinPoint.getSignature());
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest
                    || args[i] instanceof ServletResponse
                    || args[i] instanceof MultipartFile) {
                continue;
            }
            arguments[i] = shouldMaskArgument(joinPoint.getSignature(), parameterNames, i) ? MASKED_VALUE : args[i];
        }
        return arguments;
    }

    private String[] parameterNames(Signature signature) {
        if (signature instanceof CodeSignature) {
            return ((CodeSignature) signature).getParameterNames();
        }
        return new String[0];
    }

    private boolean shouldMaskArgument(Signature signature, String[] parameterNames, int index) {
        if (index < parameterNames.length && isSensitiveName(parameterNames[index])) {
            return true;
        }
        // 编译环境没有保留参数名时，登录接口的第二个参数按约定仍视为密码。
        return "login".equals(signature.getName()) && index == 1;
    }

    private boolean isSensitiveName(String name) {
        return name != null && SENSITIVE_FIELD_NAMES.contains(name.toLowerCase(Locale.ROOT));
    }

    private PropertyPreFilters.MySimplePropertyPreFilter sensitiveFilter() {
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludeFilter = filters.addFilter();
        excludeFilter.addExcludes(SENSITIVE_FIELDS);
        return excludeFilter;
    }
}
