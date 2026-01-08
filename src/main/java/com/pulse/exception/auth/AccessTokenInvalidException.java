package com.pulse.exception.auth;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class AccessTokenInvalidException extends BaseException {

    public AccessTokenInvalidException() {
        super(ErrorCode.ACCESS_TOKEN_INVALID, ErrorCode.ACCESS_TOKEN_INVALID.getMessage());
    }
}
