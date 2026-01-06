package com.pulse.exception.config;

import com.pulse.exception.BaseException;
import com.pulse.exception.ErrorCode;

public class AwsConfigurationException extends BaseException {

    public AwsConfigurationException(String message, Throwable cause) {
        super(ErrorCode.AWS_CONFIGURATION_ERROR, message, cause);
    }
}
