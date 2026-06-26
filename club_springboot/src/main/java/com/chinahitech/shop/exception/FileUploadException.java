package com.chinahitech.shop.exception;

/**
 * 文件上传、保存或批量导入文件处理失败时抛出的业务异常。
 */
public class FileUploadException extends ServiceException {
    /**
     * 创建对应业务异常对象，便于统一异常处理器返回错误信息。
     */
    public FileUploadException() {
        super();
    }

    /**
     * 处理文件上传相关逻辑。
     */
    public FileUploadException(String message) {
        super(message);
    }

    /**
     * 处理文件上传相关逻辑。
     */
    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 处理文件上传相关逻辑。
     */
    public FileUploadException(Throwable cause) {
        super(cause);
    }

    /**
     * 处理文件上传相关逻辑。
     */
    protected FileUploadException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
