package com.pulse.exception.dataload;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class MasterDataNotFoundException extends BaseException {

    public MasterDataNotFoundException(String dataType, String identifier) {
        super(ErrorCode.MASTER_DATA_NOT_FOUND, String.format("No %s master data: %s", dataType, identifier));
    }
}
