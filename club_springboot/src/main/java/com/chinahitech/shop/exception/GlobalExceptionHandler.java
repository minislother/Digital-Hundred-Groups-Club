package com.chinahitech.shop.exception;

import com.chinahitech.shop.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * 全局异常处理器，将控制器和业务层抛出的异常转换为统一的 Result 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理明确指定错误码的业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business exception, path={}, code={}, message={}", path(request), e.getCode(), e.getMessage());
        return error(e.getCode(), messageOrDefault(e, "Operation failed"));
    }

    /**
     * 处理业务服务层抛出的通用业务异常。
     */
    @ExceptionHandler(ServiceException.class)
    public Result handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("Service exception, path={}, type={}, message={}",
                path(request), e.getClass().getSimpleName(), e.getMessage());
        return error(serviceErrorCode(e), messageOrDefault(e, "Operation failed"));
    }

    /**
     * 处理接口限流异常，返回限流错误码。
     */
    @ExceptionHandler(RateLimitException.class)
    public Result handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        log.warn("Rate limit, path={}, message={}", path(request), e.getMessage());
        return error(ErrorCode.RATE_LIMIT, messageOrDefault(e, "Too many requests, please try again later"));
    }

    /**
     * 处理 Bean Validation 或表单绑定产生的参数校验异常。
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result handleValidException(Exception e, HttpServletRequest request) {
        String message = "Parameter validation failed";
        if (e instanceof MethodArgumentNotValidException) {
            message = fieldErrorMessage(((MethodArgumentNotValidException) e).getBindingResult().getFieldError(), message);
        } else if (e instanceof BindException) {
            message = fieldErrorMessage(((BindException) e).getBindingResult().getFieldError(), message);
        }
        log.warn("Validation failed, path={}, message={}", path(request), message);
        return error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理缺少必填请求参数的异常。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "Missing required parameter: " + e.getParameterName();
        log.warn("Missing parameter, path={}, message={}", path(request), message);
        return error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求参数类型无法转换的异常。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Parameter type mismatch: " + e.getName();
        log.warn("Parameter type mismatch, path={}, message={}", path(request), message);
        return error(ErrorCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求体 JSON 或表单内容无法读取的异常。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Request body not readable, path={}, message={}", path(request), e.getMessage());
        return error(ErrorCode.PARAM_ERROR, "Request body format error");
    }

    /**
     * 处理请求路径不存在的异常。
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public Result handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found, path={}, method={}", path(request), e.getHttpMethod());
        return error(ErrorCode.NOT_FOUND, "Request path not found");
    }

    /**
     * 处理请求方法不支持的异常。
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported, path={}, method={}", path(request), e.getMethod());
        return error(ErrorCode.METHOD_NOT_ALLOWED, "Request method not supported");
    }

    /**
     * 兜底处理未被其他规则捕获的系统异常。
     */
    @ExceptionHandler(Exception.class)
    public Result handleGlobalException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception, path={}, message={}", path(request), e.getMessage(), e);
        return error(ErrorCode.SYSTEM_ERROR, "System error, please try again later");
    }

    private Result error(String errorCode, String message) {
        return Result.error()
                .message(message)
                .data("errorCode", errorCode);
    }

    private String fieldErrorMessage(FieldError fieldError, String defaultMessage) {
        return Objects.nonNull(fieldError) ? fieldError.getDefaultMessage() : defaultMessage;
    }

    private String messageOrDefault(Throwable e, String defaultMessage) {
        return Objects.nonNull(e.getMessage()) && !e.getMessage().trim().isEmpty()
                ? e.getMessage()
                : defaultMessage;
    }

    private String serviceErrorCode(ServiceException e) {
        if (!ErrorCode.BUSINESS_ERROR.equals(e.getCode()) || e.getClass().equals(ServiceException.class)) {
            return e.getCode();
        }
        return e.getClass().getSimpleName();
    }

    private String path(HttpServletRequest request) {
        return Objects.nonNull(request) ? request.getRequestURI() : "";
    }
}
