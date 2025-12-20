package com.pulse.api.seoulopendataplaza;

import com.pulse.api.seoulopendataplaza.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendataplaza.validator.SeoulApiResponseValidator;
import com.pulse.config.SeoulApiProperties;
import com.pulse.exception.dataload.ApiCommunicationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SeoulOpenDataPlazaClient {

    private final RestTemplate restTemplate;
    private final SeoulApiResponseValidator validator;
    private final SeoulApiProperties properties;

    private static final String SEOUL_OPEN_API_FORMAT = "%s/%s/json/%s/%d/%d/%s";

    public SeoulOpenDataPlazaClient(
            RestTemplate restTemplate,
            SeoulApiResponseValidator validator,
            SeoulApiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.validator = validator;
        this.properties = properties;
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
        try {
            SubwayApiResponse response = restTemplate.getForObject(url, SubwayApiResponse.class);
            return validator.validate(response);
        } catch (RestClientException e) {
            throw new ApiCommunicationException("Failed to communicate with Seoul Open Data Plaza API: " + url, e);
        }
    }
}
