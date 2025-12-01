package com.pulse.api.seoulopendataplaza.ridershipData.validator;

import com.pulse.api.seoulopendataplaza.ridershipData.ApiResponse;

public interface ApiResponseValidator {
    <T extends ApiResponse> T validate(T response, Class<T> responseType);
}
