package com.pulse.dto.auth;

import com.pulse.entity.user.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialLoginRequest(
    @NotNull(message = "Provider type is required")
    ProviderType providerType,

    String nickname,

    @NotBlank(message = "Authorization code is required")
    String authorizationCode,

    String redirectUri
) {}
