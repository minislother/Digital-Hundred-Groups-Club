package com.chinahitech.shop.exception;

/**
 * 上传文件类型或 Excel 文件格式不受支持时抛出的业务异常。
 */
public class FileTypeException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public FileTypeException() {
        super();
    }

    public FileTypeException(String message) {
        super(message);
    }

    public FileTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileTypeException(Throwable cause) {
        super(cause);
    }

    protected FileTypeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
