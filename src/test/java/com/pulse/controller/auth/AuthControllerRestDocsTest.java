package com.pulse.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulse.dto.auth.LoginResponse;
import com.pulse.dto.auth.RefreshTokenRequest;
import com.pulse.dto.auth.SocialLoginRequest;
import com.pulse.dto.auth.TokenRefreshResponse;
import com.pulse.entity.user.ProviderType;
import com.pulse.service.auth.AuthService;
import com.pulse.support.RestDocsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerRestDocsTest extends RestDocsSupport {

    private final AuthService authService = mock(AuthService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected Object controller() {
        return new AuthController(authService);
    }

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(1L, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("소셜 로그인")
    void login() throws Exception {
        // given
        SocialLoginRequest request = new SocialLoginRequest(
                ProviderType.KAKAO, "홍길동", "auth-code-example", "https://example.com/callback"
        );
        LoginResponse response = LoginResponse.of(
                "access-token-example",
                "refresh-token-example",
                3600L,
                new LoginResponse.UserInfo(1L, "홍길동", "USER")
        );
        given(authService.login(any(), any(), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("providerType").description("소셜 로그인 제공자 (KAKAO, GOOGLE)"),
                                fieldWithPath("nickname").description("사용자 닉네임 (선택)").optional(),
                                fieldWithPath("authorizationCode").description("소셜 인가 코드"),
                                fieldWithPath("redirectUri").description("리다이렉트 URI (선택)").optional()
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("액세스 토큰"),
                                fieldWithPath("refreshToken").description("리프레시 토큰"),
                                fieldWithPath("tokenType").description("토큰 타입 (Bearer)"),
                                fieldWithPath("expiresIn").description("액세스 토큰 만료 시간 (초)"),
                                fieldWithPath("user.id").description("사용자 ID"),
                                fieldWithPath("user.nickname").description("사용자 닉네임"),
                                fieldWithPath("user.role").description("사용자 권한")
                        )
                ));
    }

    @Test
    @DisplayName("토큰 갱신")
    void refresh() throws Exception {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token-example");
        TokenRefreshResponse response = TokenRefreshResponse.of(
                "new-access-token-example", "new-refresh-token-example", 3600L
        );
        given(authService.refreshTokens(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        requestFields(
                                fieldWithPath("refreshToken").description("갱신에 사용할 리프레시 토큰")
                        ),
                        responseFields(
                                fieldWithPath("accessToken").description("새로 발급된 액세스 토큰"),
                                fieldWithPath("refreshToken").description("새로 발급된 리프레시 토큰"),
                                fieldWithPath("tokenType").description("토큰 타입 (Bearer)"),
                                fieldWithPath("expiresIn").description("액세스 토큰 만료 시간 (초)")
                        )
                ));
    }

    @Test
    @DisplayName("로그아웃")
    void logout() throws Exception {
        // given
        willDoNothing().given(authService).logout(1L);

        // when & then
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }
}
