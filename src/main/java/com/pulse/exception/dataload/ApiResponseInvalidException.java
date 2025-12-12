package com.pulse.exception.dataload;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class ApiResponseInvalidException extends BaseException {

    public ApiResponseInvalidException(String message) {
        super(ErrorCode.API_RESPONSE_INVALID, message);
    }
}
