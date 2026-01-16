package com.pulse.api.seoulopendata;

import com.pulse.api.seoulopendata.dto.subway.SubwayApiResponse;
import com.pulse.api.seoulopendata.validator.SeoulApiResponseValidator;
import com.pulse.config.SeoulApiProperties;
import com.pulse.exception.dataload.ApiCommunicationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SeoulOpenDataClient {

    private final RestTemplate restTemplate;
    private final SeoulApiResponseValidator validator;
    private final SeoulApiProperties properties;

    private static final String SEOUL_OPEN_API_FORMAT = "%s/%s/json/%s/%d/%d/%s";

    public SeoulOpenDataClient(
            RestTemplate restTemplate,
            SeoulApiResponseValidator validator,
            SeoulApiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.validator = validator;
        this.properties = properties;
    }

    public SubwayApiResponse fetchSubwayPassengerData(String yearMonth, int startIndex, int endIndex) {
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
            throw new ApiCommunicationException(
                    String.format(
                            "Failed to communicate with Seoul Open Data Plaza API: yearMonth=%s, range=%d-%d",
                            yearMonth,
                            startIndex,
                            endIndex
                    ), e
            );
        }
    }
}
