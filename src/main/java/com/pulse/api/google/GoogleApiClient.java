package com.pulse.api.google;

import com.pulse.api.google.dto.GoogleTokenInfoResponse;
import com.pulse.api.google.dto.GoogleTokenResponse;
import com.pulse.config.GoogleApiProperties;
import com.pulse.exception.auth.SocialLoginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class GoogleApiClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleApiClient.class);
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestTemplate restTemplate;
    private final GoogleApiProperties googleApiProperties;

    public GoogleApiClient(RestTemplate restTemplate, GoogleApiProperties googleApiProperties) {
        this.restTemplate = restTemplate;
        this.googleApiProperties = googleApiProperties;
    }

    public GoogleTokenResponse getAccessToken(String authorizationCode, String redirectUri) {

        String decodedCode = URLDecoder.decode(authorizationCode, StandardCharsets.UTF_8);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = createTokenRequestEntity(decodedCode, redirectUri, headers);

            ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(
                    TOKEN_URL,
                    request,
                    GoogleTokenResponse.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to exchange Google authorization code: {}", e.getMessage());
            throw new SocialLoginException("Failed to exchange Google authorization code");
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createTokenRequestEntity(String decodedCode, String redirectUri, HttpHeaders headers) {
        String actualRedirectUri = (redirectUri != null) ? redirectUri : googleApiProperties.getRedirectUri();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleApiProperties.getClientId());
        params.add("client_secret", googleApiProperties.getClientSecret());
        params.add("redirect_uri", actualRedirectUri);
        params.add("code", decodedCode);

        return new HttpEntity<>(params, headers);
    }

    public GoogleTokenInfoResponse getUserInfo(String accessToken) {

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<GoogleTokenInfoResponse> response = restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    GoogleTokenInfoResponse.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch Google user info: {}", e.getMessage());
            throw new SocialLoginException("Failed to verify Google token");
        }
    }
}
