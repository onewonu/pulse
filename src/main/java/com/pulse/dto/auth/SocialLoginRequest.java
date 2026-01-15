package com.pulse.dto.auth;

import com.pulse.entity.user.ProviderType;
import jakarta.validation.constraints.NotBlank;

public class SocialLoginRequest {

    @NotBlank(message = "Provider type is required")
    private String providerType;

    private String nickname;

    @NotBlank(message = "Authorization code is required")
    private String authorizationCode;

    private String redirectUri;

    public ProviderType getProviderType() {
        try {
            return ProviderType.valueOf(providerType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported provider type: " + providerType);
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setProviderType(String providerType) {
        this.providerType = providerType;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }
}
