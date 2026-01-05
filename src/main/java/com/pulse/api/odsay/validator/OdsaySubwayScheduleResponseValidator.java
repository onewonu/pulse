package com.pulse.api.odsay.validator;

import com.pulse.api.odsay.dto.OdsaySubwayScheduleResponse;
import com.pulse.exception.ErrorCode;
import com.pulse.exception.search.OdsayApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OdsaySubwayScheduleResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(OdsaySubwayScheduleResponseValidator.class);
    private static final Integer INVALID_PARAMETER_CODE = -8;
    private static final Integer INVALID_STATION_ID_CODE = -9;
    private static final Integer NO_RESULTS_CODE = -99;
    private static final Integer SERVER_ERROR_CODE = 500;

    public OdsaySubwayScheduleResponse validate(OdsaySubwayScheduleResponse response) {
        String className = "OdsaySubwayScheduleResponse";

        if (response == null || response.getResult() == null) {
            String errorMessage = String.format("%s API response or result is null", className);
            log.error(errorMessage);
            throw new OdsayApiException(ErrorCode.API_RESPONSE_INVALID, errorMessage);
        }

        OdsaySubwayScheduleResponse.ResultData result = response.getResult();
        OdsaySubwayScheduleResponse.ErrorData error = result.getError();

        if (error != null) {
            Integer errorCode = error.getCode();
            String errorMessage = error.getMessage();

            log.error("{} API error - CODE: {}, MESSAGE: {}", className, errorCode, errorMessage);

            if (INVALID_PARAMETER_CODE.equals(errorCode)) {
                String message = String.format("%s API invalid parameter: %s", className, errorMessage);
                throw new OdsayApiException(ErrorCode.INVALID_PARAMETER, message);
            }

            if (INVALID_STATION_ID_CODE.equals(errorCode)) {
                String message = String.format("%s API invalid station ID: %s", className, errorMessage);
                throw new OdsayApiException(ErrorCode.INVALID_PARAMETER, message);
            }

            if (NO_RESULTS_CODE.equals(errorCode)) {
                log.info("{} API returned no results", className);
                return response;
            }

            if (SERVER_ERROR_CODE.equals(errorCode)) {
                String message = String.format("%s API server error: %s", className, errorMessage);
                throw new OdsayApiException(ErrorCode.API_COMMUNICATION_FAILED, message);
            }

            String message = String.format(
                    "%s API returned error code: %s - %s", className, errorCode, errorMessage
            );
            log.error(message);
            throw new OdsayApiException(ErrorCode.API_RESPONSE_INVALID, message);
        }

        List<OdsaySubwayScheduleResponse.PathData> paths = result.getPath();
        if (paths == null || paths.isEmpty()) {
            log.info("{} API returned empty response (no paths found)", className);
        } else {
            log.info("{} API returned {} path(s)", className, paths.size());
        }

        return response;
    }
}
