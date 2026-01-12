package com.pulse.dto.auth;

import com.pulse.entity.user.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SocialLoginRequest {

    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    private String nickname;

    @NotBlank(message = "Authorization code is required")
    private String authorizationCode;

    public ProviderType getProviderType() {
        return providerType;
    }

    public String getNickname() {
        return nickname;
    }

    public String getAuthorizationCode() {
        return authorizationCode;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setAuthorizationCode(String authorizationCode) {
        this.authorizationCode = authorizationCode;
    }
}
