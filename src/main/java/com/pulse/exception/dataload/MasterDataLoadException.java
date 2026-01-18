package com.pulse.exception.dataload;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class MasterDataLoadException extends BaseException {

    public MasterDataLoadException(String message, Throwable cause) {
        super(ErrorCode.MASTER_DATA_LOAD_FAILED, message, cause);
    }
}
