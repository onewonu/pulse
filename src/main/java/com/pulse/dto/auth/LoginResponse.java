package com.pulse.dto.auth;

public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;

    public static class UserInfo {
        private final Long id;
        private final String nickname;
        private final String role;

        public UserInfo(Long id, String nickname, String role) {
            this.id = id;
            this.nickname = nickname;
            this.role = role;
        }

        public Long getId() {
            return id;
        }

        public String getNickname() {
            return nickname;
        }

        public String getRole() {
            return role;
        }
    }

    public static LoginResponse of(String accessToken, String refreshToken, Long expiresIn, UserInfo user) {
        LoginResponse response = new LoginResponse();
        response.accessToken = accessToken;
        response.refreshToken = refreshToken;
        response.tokenType = "Bearer";
        response.expiresIn = expiresIn;
        response.user = user;
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

    public UserInfo getUser() {
        return user;
    }
}
