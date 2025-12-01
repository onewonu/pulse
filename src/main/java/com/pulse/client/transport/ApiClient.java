package com.pulse.client.transport;

import com.pulse.client.transport.dto.ApiResponse;
import com.pulse.client.transport.dto.bus.BusApiResponse;
import com.pulse.client.transport.dto.subway.SubwayApiResponse;
import com.pulse.client.transport.validator.ApiResponseValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ApiClient {

    private final RestTemplate restTemplate;
    private final ApiResponseValidator validator;

    @Value("${seoul-api.base-url}")
    private String baseUrl;

    @Value("${seoul-api.key}")
    private String apiKey;

    @Value("${seoul-api.services.bus}")
    private String busService;

    @Value("${seoul-api.services.subway}")
    private String subwayService;

    private static final String SEOUL_OPEN_API_FORMAT = "%s/%s/json/%s/%d/%d/%s";

    public ApiClient(RestTemplate restTemplate, ApiResponseValidator validator) {
        this.restTemplate = restTemplate;
        this.validator = validator;
    }

    public BusApiResponse fetchBusRidershipData(String yearMonth, int startIndex, int endIndex) {
        String url = String.format(SEOUL_OPEN_API_FORMAT, baseUrl, apiKey, busService, startIndex, endIndex, yearMonth);
        return fetchData(url, BusApiResponse.class);
    }

    public SubwayApiResponse fetchSubwayRidershipData(String yearMonth, int startIndex, int endIndex) {
        String url = String.format(SEOUL_OPEN_API_FORMAT, baseUrl, apiKey, subwayService, startIndex, endIndex, yearMonth);
        return fetchData(url, SubwayApiResponse.class);
    }

    private <T extends ApiResponse> T fetchData(String url, Class<T> responseType) {
        T response = restTemplate.getForObject(url, responseType);
        return validator.validate(response, responseType);
    }
}
