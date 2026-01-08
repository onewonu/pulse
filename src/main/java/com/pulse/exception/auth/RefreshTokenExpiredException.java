package com.pulse.exception.auth;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class RefreshTokenExpiredException extends BaseException {

    public RefreshTokenExpiredException() {
        super(ErrorCode.REFRESH_TOKEN_EXPIRED, ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage());
    }
}
