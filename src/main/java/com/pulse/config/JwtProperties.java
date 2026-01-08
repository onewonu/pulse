package com.pulse.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationSeconds;
    private long refreshTokenExpirationSeconds;

    public String getSecret() {
        return secret;
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public long getRefreshTokenExpirationSeconds() {
        return refreshTokenExpirationSeconds;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public void setAccessTokenExpirationSeconds(long accessTokenExpirationSeconds) {
        this.accessTokenExpirationSeconds = accessTokenExpirationSeconds;
    }

    public void setRefreshTokenExpirationSeconds(long refreshTokenExpirationSeconds) {
        this.refreshTokenExpirationSeconds = refreshTokenExpirationSeconds;
    }
}
