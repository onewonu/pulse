package com.pulse.dto.auth;

public class TokenRefreshResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    public static TokenRefreshResponse of(String accessToken, String refreshToken, Long expiresIn) {
        TokenRefreshResponse response = new TokenRefreshResponse();
        response.accessToken = accessToken;
        response.refreshToken = refreshToken;
        response.tokenType = "Bearer";
        response.expiresIn = expiresIn;
        return response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }
}
