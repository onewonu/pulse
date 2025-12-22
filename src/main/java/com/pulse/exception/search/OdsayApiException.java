package com.pulse.exception.search;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class OdsayApiException extends BaseException {

    public OdsayApiException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

}
