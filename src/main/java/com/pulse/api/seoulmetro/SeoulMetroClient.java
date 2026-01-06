package com.pulse.api.seoulmetro;

import com.pulse.api.seoulmetro.dto.SeoulMetroTrainScheduleResponse;
import com.pulse.api.seoulmetro.validator.SeoulMetroApiResponseValidator;
import com.pulse.config.SeoulMetroApiProperties;
import com.pulse.exception.dataload.ApiCommunicationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SeoulMetroClient {

    private final RestTemplate restTemplate;
    private final SeoulMetroApiResponseValidator validator;
    private final SeoulMetroApiProperties properties;

    public SeoulMetroClient(
            RestTemplate restTemplate,
            SeoulMetroApiResponseValidator validator,
            SeoulMetroApiProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.validator = validator;
        this.properties = properties;
    }

    public SeoulMetroTrainScheduleResponse getTrainSchedule(
            String lineName,
            String stationName,
            String updownType,
            String dayType,
            String tmprTmtblYn
    ) {
        String url = buildUrl(lineName, stationName, updownType, dayType, tmprTmtblYn);

        try {
            SeoulMetroTrainScheduleResponse response = restTemplate.getForObject(url, SeoulMetroTrainScheduleResponse.class);
            return validator.validate(response);
        } catch (RestClientException e) {
            throw new ApiCommunicationException(
                    String.format(
                            "Failed to communicate with Seoul Metro API: line=%s, station=%s, updown=%s, dayType=%s",
                            lineName,
                            stationName,
                            updownType,
                            dayType
                    ), e
            );
        }
    }

    private String buildUrl(
            String lineName,
            String stationName,
            String updownType,
            String dayType,
            String tmprTmtblYn
    ) {
        return UriComponentsBuilder.fromUriString(properties.getBaseUrl())
                .queryParam("serviceKey", properties.getKey())
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", properties.getNumOfRows())
                .queryParam("dataType", properties.getDataType())
                .queryParam("lineNm", lineName)
                .queryParam("stnNm", stationName)
                .queryParam("upbdnbSe", updownType)
                .queryParam("wkndSe", dayType)
                .queryParam("tmprTmtblYn", tmprTmtblYn)
                .build()
                .toUriString();
    }
}
