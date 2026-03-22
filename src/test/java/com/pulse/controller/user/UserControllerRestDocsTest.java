package com.pulse.controller.user;

import com.pulse.dto.user.UserInfoResponse;
import com.pulse.service.user.UserService;
import com.pulse.support.RestDocsSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerRestDocsTest extends RestDocsSupport {

    private final UserService userService = mock(UserService.class);

    @Override
    protected Object controller() {
        return new UserController(userService);
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
    @DisplayName("현재 사용자 정보 조회")
    void getCurrentUser() throws Exception {
        // given
        UserInfoResponse response = new UserInfoResponse(1L, "홍길동");
        given(userService.getUserInfo(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/user/me"))
                .andExpect(status().isOk())
                .andDo(restDocs.document(
                        responseFields(
                                fieldWithPath("id").description("사용자 ID"),
                                fieldWithPath("nickname").description("사용자 닉네임")
                        )
                ));
    }
}
