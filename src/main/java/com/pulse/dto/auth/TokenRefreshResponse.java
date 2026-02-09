package com.pulse.dto.auth;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn
) {
    public static TokenRefreshResponse of(String accessToken, String refreshToken, Long expiresIn) {
        return new TokenRefreshResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
