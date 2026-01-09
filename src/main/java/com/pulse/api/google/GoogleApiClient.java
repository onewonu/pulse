package com.pulse.api.google;

import com.pulse.api.google.dto.GoogleTokenInfoResponse;
import com.pulse.exception.auth.SocialLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class GoogleApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiClient.class);
    private static final String TOKEN_INFO_URL = "https://www.googleapis.com/oauth2/v3/tokeninfo?access_token={accessToken}";

    private final RestTemplate restTemplate;

    public GoogleApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public GoogleTokenInfoResponse getTokenInfo(String accessToken) {
        try {
            ResponseEntity<GoogleTokenInfoResponse> response = restTemplate.getForEntity(
                    TOKEN_INFO_URL,
                    GoogleTokenInfoResponse.class,
                    accessToken
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.warn("Failed to fetch Google token info: {}", e.getMessage());
            throw new SocialLoginException("Failed to verify Google token");
        }
    }
}
