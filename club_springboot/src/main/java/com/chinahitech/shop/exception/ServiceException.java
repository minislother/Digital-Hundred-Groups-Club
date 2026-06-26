package com.chinahitech.shop.exception;

/**
 * 后端业务异常基类，携带统一错误码并由全局异常处理器转换为 Result 响应。
 */
public class ServiceException extends RuntimeException {

    private final String code;

    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public ServiceException() {
        super();
        this.code = ErrorCode.BUSINESS_ERROR;
    }

    public ServiceException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR;
    }

    public ServiceException(String code, String message) {
        super(message);
        this.code = normalizeCode(code);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = ErrorCode.BUSINESS_ERROR;
    }

    public ServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = normalizeCode(code);
    }

    public ServiceException(Throwable cause) {
        super(cause);
        this.code = ErrorCode.BUSINESS_ERROR;
    }

    protected ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = ErrorCode.BUSINESS_ERROR;
    }

    /**
     * 读取并返回业务数据或页面状态。
     */
    public String getCode() {
        return code;
    }

    private String normalizeCode(String code) {
        return code != null && !code.trim().isEmpty() ? code : ErrorCode.BUSINESS_ERROR;
    }
}
