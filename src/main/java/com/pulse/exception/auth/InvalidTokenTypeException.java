package com.pulse.exception.auth;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class InvalidTokenTypeException extends BaseException {

    public InvalidTokenTypeException(String message) {
        super(ErrorCode.ACCESS_TOKEN_INVALID, message);
    }
}
