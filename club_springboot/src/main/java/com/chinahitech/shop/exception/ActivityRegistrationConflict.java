package com.chinahitech.shop.exception;

import org.springframework.dao.DuplicateKeyException;

/** 只识别报名业务唯一键，避免把其他主键/唯一键冲突误报成重复报名。 */
public final class ActivityRegistrationConflict {
    public static final String INDEX_NAME = "uk_individual_activity_activity_user";

    private ActivityRegistrationConflict() { }

    public static boolean matches(Throwable error) {
        if (!(error instanceof DuplicateKeyException)) { return false; }
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            // MySQL 的错误信息会包含 'table.index' 或 'index'，兼容两种限定形式。
            if (message != null && (message.contains("'" + INDEX_NAME + "'")
                    || message.contains("." + INDEX_NAME + "'"))) { return true; }
        }
        return false;
    }
}
