package com.pulse.exception.search;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class NoSchedulesAvailableException extends BaseException {

    public NoSchedulesAvailableException(String message) {
        super(ErrorCode.NO_SCHEDULES_AVAILABLE, message);
    }

}
