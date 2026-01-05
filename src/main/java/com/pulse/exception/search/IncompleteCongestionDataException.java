package com.pulse.exception.search;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class IncompleteCongestionDataException extends BaseException {

    public IncompleteCongestionDataException(String message) {
        super(ErrorCode.INCOMPLETE_CONGESTION_DATA, message);
    }

}
