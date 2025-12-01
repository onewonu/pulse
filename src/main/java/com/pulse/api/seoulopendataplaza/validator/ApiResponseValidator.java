package com.pulse.api.seoulopendataplaza.validator;

import com.pulse.api.seoulopendataplaza.ApiResponse;

public interface ApiResponseValidator {
    <T extends ApiResponse> T validate(T response, Class<T> responseType);
}
