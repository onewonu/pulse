package com.pulse.dto;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

import java.time.LocalDateTime;

public record ErrorResponse(
    String errorCode,
    String message,
    LocalDateTime timestamp
) {
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
}
