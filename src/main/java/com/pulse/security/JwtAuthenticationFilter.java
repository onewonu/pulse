package com.pulse.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.ErrorResponse;
import com.pulse.exception.ErrorCode;
import com.pulse.exception.auth.AccessTokenExpiredException;
import com.pulse.exception.auth.AccessTokenInvalidException;
import com.pulse.exception.auth.InvalidTokenTypeException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                jwtTokenProvider.validateToken(jwt);

                String tokenType = jwtTokenProvider.getTokenType(jwt);
                if (!"ACCESS".equals(tokenType)) {
                    log.warn("Attempted to use non-access token for API request: tokenType={}", tokenType);
                    throw new InvalidTokenTypeException("Refresh token cannot be used for API access");
                }

                Long userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String role = jwtTokenProvider.getRoleFromToken(jwt);

                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(authority)
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (AccessTokenExpiredException e) {
            log.warn("Access token expired: {}", e.getMessage());
            sendErrorResponse(response, ErrorCode.ACCESS_TOKEN_EXPIRED, "Access token has expired");
            return;
        } catch (AccessTokenInvalidException | InvalidTokenTypeException e) {
            log.warn("Invalid access token: {}", e.getMessage());
            sendErrorResponse(response, ErrorCode.ACCESS_TOKEN_INVALID, e.getMessage());
            return;
        } catch (RuntimeException e) {
            log.error("Unexpected authentication error: {}", e.getMessage());
            sendErrorResponse(response, ErrorCode.ACCESS_TOKEN_INVALID, "Access token is invalid or malformed");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(errorCode, message);
        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
