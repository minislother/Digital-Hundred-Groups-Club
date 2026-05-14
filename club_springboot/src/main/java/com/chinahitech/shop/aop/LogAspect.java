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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

@Aspect
@Component
public class LogAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LogAspect.class);
    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);
    private static final String[] SENSITIVE_FIELDS = {
            "password", "salt", "valicode", "validateCode", "token",
            "userId", "userNumber", "studentId", "stuNumber", "stunumber",
            "phone", "email"
    };

    @Pointcut("execution(public * com.chinahitech..*Controller.*(..))")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
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
        LOG.info("args: {}", JSONObject.toJSONString(filterArgs(joinPoint.getArgs()), sensitiveFilter()));

        try {
            Object result = joinPoint.proceed();
            LOG.info("result: {}", JSONObject.toJSONString(result, sensitiveFilter()));
            return result;
        } finally {
            LOG.info("------------- request end cost:{} ms -------------", System.currentTimeMillis() - startTime);
            MDC.clear();
        }
    }

    private Object[] filterArgs(Object[] args) {
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest
                    || args[i] instanceof ServletResponse
                    || args[i] instanceof MultipartFile) {
                continue;
            }
            arguments[i] = args[i];
        }
        return arguments;
    }

    private PropertyPreFilters.MySimplePropertyPreFilter sensitiveFilter() {
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludeFilter = filters.addFilter();
        excludeFilter.addExcludes(SENSITIVE_FIELDS);
        return excludeFilter;
    }
}
