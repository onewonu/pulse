package com.pulse.client.transport;

import com.pulse.client.transport.dto.bus.BusApiResponse;
import com.pulse.client.transport.dto.subway.SubwayApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SeoulOpenApiClient {

    private final RestTemplate restTemplate;

    @Value("${seoul-api.base-url}")
    private String baseUrl;

    @Value("${seoul-api.key}")
    private String apiKey;

    @Value("${seoul-api.services.bus}")
    private String busService;

    @Value("${seoul-api.services.subway}")
    private String subwayService;

    public SeoulOpenApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BusApiResponse fetchBusRidershipData(String yearMonth, int startIndex, int endIndex) {
        return restTemplate.getForObject(
                String.format("%s/%s/json/%s/%d/%d/%s", baseUrl, apiKey, busService, startIndex, endIndex, yearMonth),
                BusApiResponse.class
        );
    }

    public SubwayApiResponse fetchSubwayRidershipData(String yearMonth, int startIndex, int endIndex) {
        return restTemplate.getForObject(
                String.format("%s/%s/json/%s/%d/%d/%s", baseUrl, apiKey, subwayService, startIndex, endIndex, yearMonth),
                SubwayApiResponse.class
        );
    }
}
