package com.pulse.exception.dataload;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class ApiCommunicationException extends BaseException {

    public ApiCommunicationException(String message, Throwable cause) {
        super(ErrorCode.API_COMMUNICATION_FAILED, message, cause);
    }
}
