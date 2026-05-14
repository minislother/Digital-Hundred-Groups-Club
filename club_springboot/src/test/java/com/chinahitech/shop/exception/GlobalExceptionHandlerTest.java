package com.chinahitech.shop.exception;

import com.chinahitech.shop.utils.Result;
import com.chinahitech.shop.utils.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test");

    @Test
    void handleBusinessExceptionUsesCustomCodeAndMessage() {
        Result result = handler.handleBusinessException(
                new BusinessException("CUSTOM_CODE", "business failed"), request);

        assertError(result, "CUSTOM_CODE", "business failed");
    }

    @Test
    void handleServiceExceptionUsesSpecificSubclassNameWhenCodeIsDefault() {
        Result result = handler.handleServiceException(new UserNotFoundException("missing user"), request);

        assertError(result, "UserNotFoundException", "missing user");
    }

    @Test
    void handleServiceExceptionKeepsExplicitCode() {
        Result result = handler.handleServiceException(
                new ServiceException("EXPLICIT_CODE", "service failed"), request);

        assertError(result, "EXPLICIT_CODE", "service failed");
    }

    @Test
    void handleRateLimitExceptionUsesRateLimitCode() {
        Result result = handler.handleRateLimitException(new RateLimitException("slow down"), request);

        assertError(result, ErrorCode.RATE_LIMIT, "slow down");
    }

    @Test
    void handleValidExceptionUsesFirstFieldErrorMessage() {
        BindException exception = new BindException(new Object(), "target");
        exception.addError(new FieldError("target", "name", "name is required"));

        Result result = handler.handleValidException(exception, request);

        assertError(result, ErrorCode.PARAM_ERROR, "name is required");
    }

    @Test
    void handleMissingServletRequestParameterExceptionNamesMissingParameter() {
        MissingServletRequestParameterException exception =
                new MissingServletRequestParameterException("studentId", "String");

        Result result = handler.handleMissingServletRequestParameterException(exception, request);

        assertError(result, ErrorCode.PARAM_ERROR, "Missing required parameter: studentId");
    }

    @Test
    void handleGlobalExceptionUsesSystemErrorCode() {
        Result result = handler.handleGlobalException(new RuntimeException("boom"), request);

        assertFalse(result.getSuccess());
        assertEquals(ResultCode.ERROR, result.getCode());
        assertEquals(ErrorCode.SYSTEM_ERROR, result.getData().get("errorCode"));
    }

    private static void assertError(Result result, String errorCode, String message) {
        assertFalse(result.getSuccess());
        assertEquals(ResultCode.ERROR, result.getCode());
        assertEquals(message, result.getMessage());
        assertEquals(errorCode, result.getData().get("errorCode"));
    }
}
