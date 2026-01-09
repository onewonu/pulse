package com.pulse.exception.user;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class UserNotFoundException extends BaseException {

    public UserNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message);
    }
}
