package com.pulse.dto;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

import java.time.LocalDateTime;

public class ErrorResponse {
    private final String errorCode;
    private final String message;
    private final LocalDateTime timestamp;

    private ErrorResponse(String errorCode, String message, LocalDateTime timestamp) {
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static ErrorResponse of(BaseException exception) {
        return new ErrorResponse(
            exception.getErrorCode().name(),
            exception.getMessage(),
            LocalDateTime.now()
        );
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(
            errorCode.name(),
            message,
            LocalDateTime.now()
        );
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
