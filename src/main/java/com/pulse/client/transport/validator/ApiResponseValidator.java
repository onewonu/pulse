package com.pulse.client.transport.validator;

import com.pulse.client.transport.dto.ApiResponse;

public interface ApiResponseValidator {
    <T extends ApiResponse> T validate(T response, Class<T> responseType);
}
