package com.pulse.service.auth;

import com.pulse.config.JwtProperties;
import com.pulse.dto.auth.LoginResponse;
import com.pulse.dto.auth.TokenRefreshResponse;
import com.pulse.entity.user.ProviderType;
import com.pulse.entity.user.RefreshToken;
import com.pulse.entity.user.User;
import com.pulse.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final SocialAuthService socialAuthService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthService(
            SocialAuthService socialAuthService,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService,
            JwtProperties jwtProperties
    ) {
        this.socialAuthService = socialAuthService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public LoginResponse login(
            ProviderType providerType,
            String nickname,
            String socialAccessToken
    ) {
        User user = socialAuthService.authenticateAndGetUser(providerType, nickname, socialAccessToken);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenService.createRefreshToken(user, refreshToken);

        LoginResponse response = LoginResponse.of(
                accessToken,
                refreshToken,
                jwtProperties.getAccessTokenExpirationSeconds(),
                new LoginResponse.UserInfo(user.getId(), user.getNickname(), user.getRole().name())
        );

        log.info("Login successful for user: id={}, role={}", user.getId(), user.getRole());
        return response;
    }

    @Transactional
    public TokenRefreshResponse refreshTokens(String refreshTokenValue) {
        log.info("Token refresh request received");

        RefreshToken refreshToken = refreshTokenService.verifyRefreshToken(refreshTokenValue);

        User user = refreshToken.getUser();

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

        refreshTokenService.createRefreshToken(user, newRefreshToken);

        TokenRefreshResponse response = TokenRefreshResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtProperties.getRefreshTokenExpirationSeconds()
        );

        log.info("Token refresh successful for user: id={}", user.getId());
        return response;
    }

    @Transactional
    public void logout(Long userId) {
        log.info("Logout request received for user: id={}", userId);
        refreshTokenService.deleteByUserId(userId);
        log.info("Logout successful for user: id={}", userId);
    }
}
