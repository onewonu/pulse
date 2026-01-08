package com.pulse.exception.auth;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class SocialLoginException extends BaseException {

    public SocialLoginException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }
}
