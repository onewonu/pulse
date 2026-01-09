package com.pulse.service.auth;

import com.pulse.config.JwtProperties;
import com.pulse.entity.user.RefreshToken;
import com.pulse.entity.user.User;
import com.pulse.exception.auth.RefreshTokenExpiredException;
import com.pulse.exception.auth.RefreshTokenInvalidException;
import com.pulse.repository.user.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtProperties jwtProperties
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public void createRefreshToken(User user, String token) {
        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser_Id(user.getId());
        if (existingToken.isPresent()) {
            refreshTokenRepository.delete(existingToken.get());
            log.info("Deleted existing refresh token for user: {}", user.getId());
        }

        LocalDateTime expiryDate = LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationSeconds());

        RefreshToken refreshToken = RefreshToken.of(token, user, expiryDate);
        refreshTokenRepository.save(refreshToken);

        log.info("Created new refresh token for user: {}", user.getId());
    }

    @Transactional
    public RefreshToken verifyRefreshToken(String token) {
        Optional<RefreshToken> optionalRefreshToken = refreshTokenRepository.findByToken(token);

        if (optionalRefreshToken.isEmpty()) {
            log.error("Refresh token not found in database");
            throw new RefreshTokenInvalidException("Refresh token not found");
        }

        RefreshToken refreshToken = optionalRefreshToken.get();

        if (refreshToken.isExpired()) {
            log.error("Refresh token expired for user: {}", refreshToken.getUserId());
            refreshTokenRepository.delete(refreshToken);
            throw new RefreshTokenExpiredException();
        }

        return refreshToken;
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        Optional<RefreshToken> token = refreshTokenRepository.findByUser_Id(userId);
        if (token.isPresent()) {
            refreshTokenRepository.delete(token.get());
            log.info("Deleted refresh token for user: {}", userId);
        }
    }

    @Transactional
    public void deleteExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens");
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        log.info("Completed cleanup of expired refresh tokens");
    }
}
