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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business exception, path={}, code={}, message={}", path(request), e.getCode(), e.getMessage());
        return error(e.getCode(), messageOrDefault(e, "Operation failed"));
    }

    @ExceptionHandler(ServiceException.class)
    public Result handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("Service exception, path={}, type={}, message={}",
                path(request), e.getClass().getSimpleName(), e.getMessage());
        return error(serviceErrorCode(e), messageOrDefault(e, "Operation failed"));
    }

    @ExceptionHandler(RateLimitException.class)
    public Result handleRateLimitException(RateLimitException e, HttpServletRequest request) {
        log.warn("Rate limit, path={}, message={}", path(request), e.getMessage());
        return error(ErrorCode.RATE_LIMIT, messageOrDefault(e, "Too many requests, please try again later"));
    }

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

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = "Missing required parameter: " + e.getParameterName();
        log.warn("Missing parameter, path={}, message={}", path(request), message);
        return error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = "Parameter type mismatch: " + e.getName();
        log.warn("Parameter type mismatch, path={}, message={}", path(request), message);
        return error(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("Request body not readable, path={}, message={}", path(request), e.getMessage());
        return error(ErrorCode.PARAM_ERROR, "Request body format error");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public Result handleNoHandlerFoundException(NoHandlerFoundException e, HttpServletRequest request) {
        log.warn("No handler found, path={}, method={}", path(request), e.getHttpMethod());
        return error(ErrorCode.NOT_FOUND, "Request path not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result handleMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("Method not supported, path={}, method={}", path(request), e.getMethod());
        return error(ErrorCode.METHOD_NOT_ALLOWED, "Request method not supported");
    }

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
