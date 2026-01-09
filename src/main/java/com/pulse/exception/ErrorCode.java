package com.pulse.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    API_COMMUNICATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "External API communication failed"),
    API_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "Invalid API response"),
    MASTER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "Required master data not found"),

    NO_SCHEDULES_AVAILABLE(HttpStatus.NOT_FOUND, "No train schedules in time range"),
    INCOMPLETE_CONGESTION_DATA(HttpStatus.PARTIAL_CONTENT, "Congestion data incomplete for time range"),

    AWS_CONFIGURATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AWS configuration error occurred"),

    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "Authorization token is missing"),
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired"),
    ACCESS_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Access token is invalid or malformed"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Refresh token has expired"),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Refresh token is invalid or malformed"),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "Social login verification failed"),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found"),

    FORBIDDEN(HttpStatus.FORBIDDEN, "Access forbidden - insufficient permissions"),

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
