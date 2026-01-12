package com.pulse.api.kakao;

import com.pulse.api.kakao.dto.KakaoTokenResponse;
import com.pulse.api.kakao.dto.KakaoUserInfoResponse;
import com.pulse.config.KakaoApiProperties;
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

@Component
public class KakaoApiClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoApiClient.class);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestTemplate restTemplate;
    private final KakaoApiProperties kakaoApiProperties;

    public KakaoApiClient(RestTemplate restTemplate, KakaoApiProperties kakaoApiProperties) {
        this.restTemplate = restTemplate;
        this.kakaoApiProperties = kakaoApiProperties;
    }

    public KakaoTokenResponse getAccessToken(String authorizationCode) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = createAuthTokenRequest(authorizationCode, headers);

            ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                    TOKEN_URL,
                    request,
                    KakaoTokenResponse.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to exchange Kakao authorization code: {}", e.getMessage());
            throw new SocialLoginException("Failed to exchange Kakao authorization code");
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createAuthTokenRequest(String authorizationCode, HttpHeaders headers) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoApiProperties.getClientId());
        params.add("redirect_uri", kakaoApiProperties.getRedirectUri());
        params.add("code", authorizationCode);
        params.add("client_secret", kakaoApiProperties.getClientSecret());

        return new HttpEntity<>(params, headers);
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<KakaoUserInfoResponse> response = restTemplate.exchange(
                    USER_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    KakaoUserInfoResponse.class
            );

            return response.getBody();
        } catch (RestClientException e) {
            log.error("Failed to fetch Kakao user info: {}", e.getMessage());
            throw new SocialLoginException("Failed to verify Kakao token");
        }
    }
}
