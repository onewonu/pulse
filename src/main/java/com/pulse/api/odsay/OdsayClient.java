package com.pulse.api.odsay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.api.odsay.dto.OdsayStationSearchResponse;
import com.pulse.api.odsay.validator.OdsayApiResponseValidator;
import com.pulse.config.OdsayApiProperties;
import com.pulse.exception.dataload.ApiCommunicationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OdsayClient {

    private static final String FORMAT = "json";
    private static final int KOREAN = 0;
    private static final String SUBWAY = "2";
    private static final Logger log = LogManager.getLogger(OdsayClient.class);

    private final OdsayApiResponseValidator validator;
    private final OdsayApiProperties properties;
    private final ObjectMapper objectMapper;

    public OdsayClient(
            OdsayApiResponseValidator validator,
            OdsayApiProperties properties,
            ObjectMapper objectMapper
    ) {
        this.validator = validator;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public OdsayStationSearchResponse searchStation(String stationName) {
        String urlString = buildUrl(stationName);
        log.info("Request URL: {}", urlString);

        try {
            String rawResponse = getRawResponse(urlString);
            log.info("Raw Response: {}", rawResponse);
            
            OdsayStationSearchResponse response = objectMapper.readValue(rawResponse, OdsayStationSearchResponse.class);

            return validator.validate(response);
        } catch (IOException e) {
            log.error("API call failed for URL: {}", urlString, e);
            throw new ApiCommunicationException("Failed to communicate with ODsay API: " + urlString, e);
        }
    }

    private String buildUrl(String stationName) {
        String encodedApiKey = URLEncoder.encode(properties.getKey(), StandardCharsets.UTF_8);
        String encodedStationName = URLEncoder.encode(stationName, StandardCharsets.UTF_8);

        return properties.getBaseUrl() +
                "?apiKey=" + encodedApiKey +
                "&stationName=" + encodedStationName +
                "&lang=" + KOREAN +
                "&output=" + FORMAT +
                "&displayCnt=" + properties.getDisplayCount() +
                "&stationClass=" + SUBWAY;
    }

    private static String getRawResponse(String urlString) throws IOException {
        URL url = URI.create(urlString).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestProperty("Content-type", "application/json");
            return readResponse(connection);
        } finally {
            connection.disconnect();
        }
    }

    private static String readResponse(HttpURLConnection connection) throws IOException {
        try (
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                )
        ) {

            StringBuilder response = new StringBuilder(1024);
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }

}
