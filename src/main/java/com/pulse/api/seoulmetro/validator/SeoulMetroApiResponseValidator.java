package com.pulse.api.seoulmetro.validator;

import com.pulse.api.seoulmetro.dto.SeoulMetroTrainScheduleResponse;
import com.pulse.exception.dataload.ApiResponseInvalidException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeoulMetroApiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(SeoulMetroApiResponseValidator.class);
    private static final String SUCCESS_CODE = "00";
    private static final String NO_DATA_CODE = "03";

    public SeoulMetroTrainScheduleResponse validate(SeoulMetroTrainScheduleResponse response) {
        String className = "SeoulMetroTrainScheduleResponse";

        if (response == null || response.getResponse() == null) {
            String errorMessage = String.format("%s API response is null", className);
            log.error(errorMessage);
            throw new ApiResponseInvalidException(errorMessage);
        }

        SeoulMetroTrainScheduleResponse.Header header = response.getResponse().getHeader();
        if (header == null) {
            String errorMessage = String.format("%s API response header is null", className);
            log.error(errorMessage);
            throw new ApiResponseInvalidException(errorMessage);
        }

        String resultCode = header.getResultCode();
        String resultMsg = header.getResultMsg();
        log.info("{} API response - resultCode: {}, resultMsg: {}", className, resultCode, resultMsg);

        if (SUCCESS_CODE.equals(resultCode)) {
            SeoulMetroTrainScheduleResponse.Body body = response.getResponse().getBody();
            if (body == null) {
                String errorMessage = String.format("%s API response body is null", className);
                log.error(errorMessage);
                throw new ApiResponseInvalidException(errorMessage);
            }

            if (!hasData(response)) {
                log.info("{} API returned empty response (no data available)", className);
                return response;
            }

            Integer totalCount = body.getTotalCount();
            log.info("{} API returned {} items", className, totalCount);
            return response;
        }

        if (NO_DATA_CODE.equals(resultCode)) {
            log.info("{} API returned no data available (resultCode: {})", className, resultCode);
            return response;
        }

        String errorMessage = String.format("%s API returned error code: %s - %s", className, resultCode, resultMsg);
        log.error(errorMessage);
        throw new ApiResponseInvalidException(errorMessage);

    }

    private boolean hasData(SeoulMetroTrainScheduleResponse response) {
        SeoulMetroTrainScheduleResponse.Body body = response.getResponse().getBody();
        if (body == null || body.getItems() == null) {
            return false;
        }
        return body.getItems().getItem() != null && !body.getItems().getItem().isEmpty();
    }
}
