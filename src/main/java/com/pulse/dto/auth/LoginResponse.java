package com.pulse.dto.auth;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    UserInfo user
) {
    public record UserInfo(
        Long id,
        String nickname,
        String role
    ) {}

    public static LoginResponse of(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
