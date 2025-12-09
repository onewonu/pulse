package com.pulse.api.seoulopendataplaza;

import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.validator.ApiResponseValidator;
import com.pulse.config.AwsSecretsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SeoulOpenDataPlazaClient {

    private final RestTemplate restTemplate;
    private final ApiResponseValidator validator;

    @Value("${seoul-api.base-url:#{null}}")
    private String baseUrl;

    @Value("${seoul-api.key:#{null}}")
    private String apiKey;

    @Value("${seoul-api.services.bus}")
    private String busService;

    @Value("${seoul-api.services.subway}")
    private String subwayService;

    private static final String SEOUL_OPEN_API_FORMAT = "%s/%s/json/%s/%d/%d/%s";

    public SeoulOpenDataPlazaClient(
            RestTemplate restTemplate,
            ApiResponseValidator validator,
            @Autowired(required = false) AwsSecretsConfig.SeoulApiConfig seoulApiConfig
    ) {
        this.restTemplate = restTemplate;
        this.validator = validator;

        if (seoulApiConfig != null) {
            this.baseUrl = seoulApiConfig.getBaseUrl();
            this.apiKey = seoulApiConfig.getApiKey();
        }
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
