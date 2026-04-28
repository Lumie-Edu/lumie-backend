package com.lumie.auth.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.auth.application.dto.response.UserResponse;
import com.lumie.auth.application.port.in.ChangePasswordUseCase;
import com.lumie.auth.application.port.in.GetMeUseCase;
import com.lumie.auth.application.port.in.GetSessionsUseCase;
import com.lumie.auth.application.port.in.LoginUseCase;
import com.lumie.auth.application.port.in.LogoutUseCase;
import com.lumie.auth.application.port.in.OwnerRegisterUseCase;
import com.lumie.auth.application.port.in.RefreshTokenUseCase;
import com.lumie.auth.application.port.in.RegisterUseCase;
import com.lumie.auth.application.port.in.RevokeSessionUseCase;
import com.lumie.auth.application.port.in.UpdateAvatarUseCase;
import com.lumie.auth.application.port.in.UpdateProfileUseCase;
import com.lumie.auth.domain.vo.Role;
import com.lumie.auth.infrastructure.config.CookieConfig;
import com.lumie.auth.infrastructure.util.CookieUtils;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController /v1/me 단위 테스트")
class AuthControllerMeTest {

    @Mock private RegisterUseCase registerUseCase;
    @Mock private OwnerRegisterUseCase ownerRegisterUseCase;
    @Mock private LoginUseCase loginUseCase;
    @Mock private LogoutUseCase logoutUseCase;
    @Mock private RefreshTokenUseCase refreshTokenUseCase;
    @Mock private UpdateAvatarUseCase updateAvatarUseCase;
    @Mock private GetMeUseCase getMeUseCase;
    @Mock private UpdateProfileUseCase updateProfileUseCase;
    @Mock private ChangePasswordUseCase changePasswordUseCase;
    @Mock private GetSessionsUseCase getSessionsUseCase;
    @Mock private RevokeSessionUseCase revokeSessionUseCase;
    @Mock private CookieUtils cookieUtils;
    @Mock private CookieConfig cookieConfig;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearContext() {
        UserContextHolder.clear();
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/me - 인증된 사용자에게 200과 UserResponse를 반환한다")
    void getMe_authenticated_returns200() throws Exception {
        // given
        UserContextHolder.setUserId(42L);
        TenantContextHolder.setTenant("acme");
        UserResponse response = UserResponse.builder()
                .id(42L)
                .userLoginId("alice")
                .name("Alice")
                .phone("01000000000")
                .email("alice@example.com")
                .role(Role.OWNER)
                .tenantSlug("acme")
                .tenantId(7L)
                .avatarSeed("seed-1")
                .build();
        given(getMeUseCase.getMe(eq(42L), eq("acme"))).willReturn(response);

        // when / then
        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.userLoginId").value("alice"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.phone").value("01000000000"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.tenantSlug").value("acme"))
                .andExpect(jsonPath("$.tenantId").value(7))
                .andExpect(jsonPath("$.avatarSeed").value("seed-1"));
    }

    @Test
    @DisplayName("GET /v1/me - userId가 컨텍스트에 없으면 401을 반환한다")
    void getMe_missingUserId_returns401() throws Exception {
        // given: no UserContextHolder.setUserId
        TenantContextHolder.setTenant("acme");

        // when / then
        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized());
        then(getMeUseCase).should(never()).getMe(eq(0L), eq("acme"));
    }

    @Test
    @DisplayName("GET /v1/me - tenantSlug가 컨텍스트에 없으면 401을 반환한다")
    void getMe_missingTenant_returns401() throws Exception {
        // given
        UserContextHolder.setUserId(42L);

        // when / then
        mockMvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /v1/me - 인증된 사용자의 프로필을 업데이트하고 200과 갱신된 UserResponse를 반환한다")
    void updateProfile_authenticated_returns200() throws Exception {
        // given
        UserContextHolder.setUserId(42L);
        TenantContextHolder.setTenant("acme");
        UserResponse response = UserResponse.builder()
                .id(42L).userLoginId("alice").name("Alice Updated").phone("01099998888")
                .email("alice@example.com")
                .role(Role.OWNER).tenantSlug("acme").tenantId(7L).avatarSeed("seed-1")
                .build();
        given(updateProfileUseCase.updateProfile(eq(42L), eq("acme"), org.mockito.ArgumentMatchers.any()))
                .willReturn(response);

        // when / then
        mockMvc.perform(patch("/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProfileBody("Alice Updated", "01099998888", "alice@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice Updated"))
                .andExpect(jsonPath("$.phone").value("01099998888"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("PATCH /v1/me - 컨텍스트 누락 시 401을 반환한다")
    void updateProfile_missingContext_returns401() throws Exception {
        mockMvc.perform(patch("/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileBody("Alice", "01000000000", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /v1/me - name이 빈 문자열이면 400을 반환한다")
    void updateProfile_blankName_returns400() throws Exception {
        UserContextHolder.setUserId(42L);
        TenantContextHolder.setTenant("acme");

        mockMvc.perform(patch("/v1/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProfileBody("", "01000000000", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/me/password - 인증된 사용자의 비밀번호 변경 시 204를 반환한다")
    void changePassword_authenticated_returns204() throws Exception {
        UserContextHolder.setUserId(42L);
        TenantContextHolder.setTenant("acme");

        mockMvc.perform(post("/v1/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordBody("Old@Pass1", "New@Pass2"))))
                .andExpect(status().isNoContent());
        then(changePasswordUseCase).should()
                .changePassword(eq(42L), eq("acme"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("POST /v1/me/password - userId 누락 시 401을 반환한다")
    void changePassword_missingUserId_returns401() throws Exception {
        mockMvc.perform(post("/v1/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordBody("Old@Pass1", "New@Pass2"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /v1/me/password - newPassword가 정책에 안 맞으면 400을 반환한다")
    void changePassword_weakNewPassword_returns400() throws Exception {
        UserContextHolder.setUserId(42L);

        mockMvc.perform(post("/v1/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangePasswordBody("Old@Pass1", "weak"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /v1/me/avatar - 인증된 사용자의 avatar 업데이트 시 204를 반환한다")
    void updateAvatar_authenticated_returns204() throws Exception {
        // given
        UserContextHolder.setUserId(42L);

        // when / then
        mockMvc.perform(patch("/v1/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvatarBody("seed-2"))))
                .andExpect(status().isNoContent());
        then(updateAvatarUseCase).should().updateAvatar(42L, "seed-2");
    }

    @Test
    @DisplayName("PATCH /v1/me/avatar - userId가 컨텍스트에 없으면 401을 반환한다")
    void updateAvatar_missingUserId_returns401() throws Exception {
        mockMvc.perform(patch("/v1/me/avatar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AvatarBody("seed-2"))))
                .andExpect(status().isUnauthorized());
    }

    private record AvatarBody(String avatarSeed) {}
    private record ProfileBody(String name, String phone, String email) {}
    private record ChangePasswordBody(String currentPassword, String newPassword) {}
}
