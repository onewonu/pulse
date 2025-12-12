package com.pulse.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    API_COMMUNICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "External API communication failed"),
    API_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "Invalid API response"),
    MASTER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Required master data not found"),

    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "Invalid parameter provided"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");

    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}
