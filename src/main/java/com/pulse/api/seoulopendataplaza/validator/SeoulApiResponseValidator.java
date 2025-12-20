package com.pulse.api.seoulopendataplaza.validator;

import com.pulse.api.seoulopendataplaza.ApiResult;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.exception.dataload.ApiResponseInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeoulApiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(SeoulApiResponseValidator.class);
    private static final String SUCCESS_CODE = "INFO-000";
    private static final String NO_DATA_CODE = "INFO-200";

    public SubwayApiResponse validate(SubwayApiResponse response) {
        String className = "SubwayApiResponse";

        if (response == null) {
            String errorMessage = String.format("%s data API response is null", className);
            log.error(errorMessage);
            throw new ApiResponseInvalidException(errorMessage);
        }

        ApiResult result = response.getResult();
        if (result != null) {
            String code = result.getCode();
            String message = result.getMessage();
            log.info("{} data API response - CODE: {}, MESSAGE: {}", className, code, message);

            if (SUCCESS_CODE.equals(code)) {
                return response;
            }

            if (NO_DATA_CODE.equals(code)) {
                log.info("{} data API returned no data available", className);
                return response;
            }

            String errorMessage = String.format(
                    "%s data API returned non-success code: %s - %s", className, code, message
            );
            log.error(errorMessage);
            throw new ApiResponseInvalidException(errorMessage);
        }

        if (!response.hasData()) {
            log.info("{} data API returned empty response (end of pagination)", className);
            return response;
        }

        return response;
    }
}
