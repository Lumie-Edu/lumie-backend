package com.lumie.auth.adapter.in.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.adapter.out.config.SessionConfig;
import com.lumie.auth.application.dto.request.LoginRequest;
import com.lumie.auth.application.exception.AuthErrorCode;
import com.lumie.auth.application.exception.AuthErrors.InvalidCredentialsException;
import com.lumie.auth.application.service.AuthQueryService;
import com.lumie.auth.application.service.AuthRegistrationService;
import com.lumie.auth.application.service.AuthSessionService;
import com.lumie.auth.application.service.AvatarService;
import com.lumie.auth.application.service.UserProfileService;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerLoginErrorTest {

    @Mock private AuthRegistrationService authRegistrationService;
    @Mock private AuthSessionService authSessionService;
    @Mock private AuthQueryService authQueryService;
    @Mock private UserProfileService userProfileService;
    @Mock private AvatarService avatarService;
    @Mock private CookieUtils cookieUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(
                authRegistrationService,
                authSessionService,
                authQueryService,
                userProfileService,
                avatarService,
                cookieUtils,
                new SessionConfig());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void loginFailureReturnsNeutralCredentialProblemDetail() throws Exception {
        when(authSessionService.login(any(LoginRequest.class), any()))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userLoginId":"unknown_user","password":"ValidPass123!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(result -> {
                    JsonNode body = new ObjectMapper().readTree(result.getResponse().getContentAsString());
                    assertThat(body.path("detail").asText())
                            .isEqualTo("아이디 또는 비밀번호가 올바르지 않습니다.");
                    String code = body.path("code").asText();
                    if (code.isBlank()) code = body.path("properties").path("code").asText();
                    assertThat(code).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS.getCode());
                });
    }
}
