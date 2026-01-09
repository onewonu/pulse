package com.pulse.dto.auth;

import com.pulse.entity.user.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SocialLoginRequest {

    @NotNull(message = "Provider type is required")
    private ProviderType providerType;

    @NotBlank(message = "Provider ID is required")
    private String providerId;

    private String nickname;

    @NotBlank(message = "Social access token is required")
    private String socialAccessToken;

    public ProviderType getProviderType() {
        return providerType;
    }

    public String getProviderId() {
        return providerId;
    }

    public String getNickname() {
        return nickname;
    }

    public String getSocialAccessToken() {
        return socialAccessToken;
    }

    public void setProviderType(ProviderType providerType) {
        this.providerType = providerType;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setSocialAccessToken(String socialAccessToken) {
        this.socialAccessToken = socialAccessToken;
    }
}
