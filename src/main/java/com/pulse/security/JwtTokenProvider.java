package com.pulse.security;

import com.pulse.config.JwtProperties;
import com.pulse.entity.user.User;
import com.pulse.exception.auth.AccessTokenExpiredException;
import com.pulse.exception.auth.AccessTokenInvalidException;
import com.pulse.exception.auth.RefreshTokenExpiredException;
import com.pulse.exception.auth.RefreshTokenInvalidException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";
    private static final String TOKEN_TYPE_REFRESH = "REFRESH";
    private static final String ROLE_CLAIM = "role";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(ROLE_CLAIM, user.getRole().name())
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpirationSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(TOKEN_TYPE_CLAIM, TOKEN_TYPE_REFRESH)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public void validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
        } catch (ExpiredJwtException e) {
            String tokenType = getTokenTypeFromExpiredToken(e);
            if (TOKEN_TYPE_REFRESH.equals(tokenType)) {
                log.error("Refresh token expired: {}", e.getMessage());
                throw new RefreshTokenExpiredException();
            } else {
                log.error("Access token expired: {}", e.getMessage());
                throw new AccessTokenExpiredException();
            }
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            String tokenType = getTokenTypeFromInvalidToken(token);
            if (TOKEN_TYPE_REFRESH.equals(tokenType)) {
                log.error("Invalid refresh token: {}", e.getMessage());
                throw new RefreshTokenInvalidException();
            } else {
                log.error("Invalid access token: {}", e.getMessage());
                throw new AccessTokenInvalidException();
            }
        } catch (IllegalArgumentException e) {
            log.error("JWT token is empty or malformed: {}", e.getMessage());
            throw new AccessTokenInvalidException();
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }

    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get(ROLE_CLAIM, String.class);
    }

    public String getTokenType(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get(TOKEN_TYPE_CLAIM, String.class);
    }

    private String getTokenTypeFromExpiredToken(ExpiredJwtException e) {
        try {
            Claims claims = e.getClaims();
            return claims.get(TOKEN_TYPE_CLAIM, String.class);
        } catch (NullPointerException | ClassCastException ex) {
            log.debug("Failed to extract token type from expired token, defaulting to ACCESS");
            return TOKEN_TYPE_ACCESS;
        }
    }

    private String getTokenTypeFromInvalidToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                if (payload.contains(TOKEN_TYPE_REFRESH)) {
                    return TOKEN_TYPE_REFRESH;
                }
            }
        } catch (IllegalArgumentException e) {
            log.debug("Failed to decode token payload, defaulting to ACCESS");
        }
        return TOKEN_TYPE_ACCESS;
    }
}
