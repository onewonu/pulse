package com.pulse.client.transport.validator;

import com.pulse.client.transport.dto.ApiResponse;
import com.pulse.client.transport.dto.ApiResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeoulApiResponseValidator implements ApiResponseValidator {

    private static final Logger log = LoggerFactory.getLogger(SeoulApiResponseValidator.class);
    private static final String SUCCESS_CODE = "INFO-000";
    private static final String NO_DATA_CODE = "INFO-200";

    @Override
    public <T extends ApiResponse> T validate(T response, Class<T> responseType) {
        String className = responseType.getSimpleName();

        if (response == null) {
            log.warn("{} data API response is null", className);
            return null;
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

            log.warn("{} data API returned non-success code: {} - {}", className, code, message);
            return null;
        } else if (!response.hasData()) {
            log.warn("{} data API response has no data", className);
            return null;
        }

        return response;
    }
}
