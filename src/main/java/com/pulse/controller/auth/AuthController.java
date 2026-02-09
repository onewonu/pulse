package com.pulse.controller.auth;

import com.pulse.dto.auth.LoginResponse;
import com.pulse.dto.auth.RefreshTokenRequest;
import com.pulse.dto.auth.SocialLoginRequest;
import com.pulse.dto.auth.TokenRefreshResponse;
import com.pulse.service.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody SocialLoginRequest request) {
        LoginResponse response = authService.login(
                request.providerType(),
                request.nickname(),
                request.authorizationCode(),
                request.redirectUri()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenRefreshResponse response = authService.refreshTokens(request.refreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = (Long) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.ok("Logout successful");
    }
}
