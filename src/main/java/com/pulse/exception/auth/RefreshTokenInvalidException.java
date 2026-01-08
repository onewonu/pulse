package com.pulse.exception.auth;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class RefreshTokenInvalidException extends BaseException {

    public RefreshTokenInvalidException() {
        super(ErrorCode.REFRESH_TOKEN_INVALID, ErrorCode.REFRESH_TOKEN_INVALID.getMessage());
    }

    public RefreshTokenInvalidException(String message) {
        super(ErrorCode.REFRESH_TOKEN_INVALID, message);
    }
}
