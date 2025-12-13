package com.pulse.api.seoulopendataplaza;

import com.pulse.api.seoulopendataplaza.dto.bus.BusApiResponse;
import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.validator.ApiResponseValidator;
import com.pulse.config.SeoulApiProperties;
import com.pulse.exception.dataload.ApiCommunicationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SeoulOpenDataPlazaClient {

    private final RestTemplate restTemplate;
    private final ApiResponseValidator validator;
    private final SeoulApiProperties properties;

    private static final String SEOUL_OPEN_API_FORMAT = "%s/%s/json/%s/%d/%d/%s";

    public SeoulOpenDataPlazaClient(
            RestTemplate restTemplate,
            ApiResponseValidator validator,
            SeoulApiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.validator = validator;
        this.properties = properties;
    }

    public BusApiResponse fetchBusRidershipData(String yearMonth, int startIndex, int endIndex) {
        String url = String.format(
                SEOUL_OPEN_API_FORMAT,
                properties.getBaseUrl(),
                properties.getKey(),
                properties.getServices().getBus(),
                startIndex,
                endIndex,
                yearMonth
        );
        return fetchData(url, BusApiResponse.class);
    }

    public SubwayApiResponse fetchSubwayRidershipData(String yearMonth, int startIndex, int endIndex) {
        String url = String.format(
                SEOUL_OPEN_API_FORMAT,
                properties.getBaseUrl(),
                properties.getKey(),
                properties.getServices().getSubway(),
                startIndex,
                endIndex,
                yearMonth
        );
        return fetchData(url, SubwayApiResponse.class);
    }

    private <T extends ApiResponse> T fetchData(String url, Class<T> responseType) {
        try {
            T response = restTemplate.getForObject(url, responseType);
            return validator.validate(response, responseType);
        } catch (RestClientException e) {
            throw new ApiCommunicationException("Failed to communicate with Seoul Open Data Plaza API: " + url, e);
        }
    }
}
