package com.lumie.tenant.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.port.out.TenantLogoStoragePort.LogoStream;
import com.lumie.tenant.application.service.TenantCommandService;
import com.lumie.tenant.application.service.TenantLogoService;
import com.lumie.tenant.application.service.TenantQueryService;
import com.lumie.tenant.domain.exception.TenantErrorCode;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TenantCommandService tenantCommandService;

    @Mock
    private TenantQueryService tenantQueryService;

    @Mock
    private TenantLogoService tenantLogoService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        TenantController rawController = new TenantController(
                tenantCommandService, tenantQueryService, tenantLogoService);

        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        ProxyFactory proxyFactory = new ProxyFactory(rawController);
        proxyFactory.addAdvice(new MethodValidationInterceptor(validatorFactory.getValidator()));
        TenantController proxiedController = (TenantController) proxyFactory.getProxy();

        mockMvc = MockMvcBuilders.standaloneSetup(proxiedController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        UserContextHolder.clear();
    }

    private static void setOwnerTenantContext() {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.OWNER);
    }

    @Test
    void directTenantCreationEndpointIsRemoved() throws Exception {
        mockMvc.perform(post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(tenantCommandService);
    }

    @Test
    @DisplayName("GET /v1/tenants/{slug} - 테넌트 조회 성공")
    void getTenantSuccess() throws Exception {
        TenantContextHolder.setTenant("test-academy");
        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .status(TenantStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantQueryService.getTenantBySlug("test-academy")).thenReturn(response);

        mockMvc.perform(get("/v1/tenants/test-academy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-academy"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /v1/tenants/{slug} - 존재하지 않는 테넌트")
    void getTenantNotFound() throws Exception {
        TenantContextHolder.setTenant("non-existent");
        when(tenantQueryService.getTenantBySlug("non-existent"))
                .thenThrow(new ResourceNotFoundException("Tenant", "non-existent"));

        mockMvc.perform(get("/v1/tenants/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void privateTenantReadRejectsAnotherSlug() throws Exception {
        TenantContextHolder.setTenant("test-academy");

        mockMvc.perform(get("/v1/tenants/another-academy"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_019"));

        verifyNoInteractions(tenantQueryService);
    }

    @Test
    void tenantListReturnsOnlyBoundTenant() throws Exception {
        TenantContextHolder.setTenant("test-academy");
        when(tenantQueryService.getTenantBySlug("test-academy"))
                .thenReturn(TenantResponse.builder().slug("test-academy").name("Own academy").build());

        mockMvc.perform(get("/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].slug").value("test-academy"));

        verify(tenantQueryService).getTenantBySlug("test-academy");
    }

    @Test
    @DisplayName("PATCH /v1/tenants/{slug} - 숨김 sidebar 항목 저장")
    void updateTenantHiddenSidebarItemsSuccess() throws Exception {
        setOwnerTenantContext();
        UpdateTenantRequest request = UpdateTenantRequest.builder()
                .hiddenSidebarItemIds(List.of("classes", "reviews"))
                .build();
        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .status(TenantStatus.ACTIVE)
                .hiddenSidebarItemIds(List.of("classes", "reviews"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantCommandService.updateTenant(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenSidebarItemIds[0]").value("classes"))
                .andExpect(jsonPath("$.hiddenSidebarItemIds[1]").value("reviews"));

        ArgumentCaptor<UpdateTenantRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTenantRequest.class);
        verify(tenantCommandService).updateTenant(any(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().hiddenSidebarItemIds()).containsExactly("classes", "reviews");
    }

    @Test
    @DisplayName("PATCH /v1/tenants/{slug} - 잘못된 sidebar 항목 형식")
    void updateTenantInvalidHiddenSidebarItemFormat() throws Exception {
        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hiddenSidebarItemIds":["Classes"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /v1/tenants/{slug} - 지원하지 않는 sidebar 항목")
    void updateTenantUnsupportedHiddenSidebarItem() throws Exception {
        setOwnerTenantContext();
        doThrow(new BusinessException(TenantErrorCode.INVALID_SIDEBAR_ITEM_ID))
                .when(tenantCommandService).updateTenant(any(), any());

        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hiddenSidebarItemIds":["not-a-menu"]}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/tenants/{slug}/complete-onboarding - 온보딩 완료")
    void completeOnboardingSuccess() throws Exception {
        CompleteTenantOnboardingRequest request = CompleteTenantOnboardingRequest.builder()
                .phone("02-1234-5678")
                .hiddenSidebarItemIds(List.of("classes", "reviews"))
                .build();
        Instant completedAt = Instant.now();
        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .status(TenantStatus.ACTIVE)
                .phone("02-1234-5678")
                .hiddenSidebarItemIds(List.of("classes", "reviews"))
                .onboardingCompletedAt(completedAt)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantCommandService.completeOnboarding(any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/tenants/test-academy/complete-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.hiddenSidebarItemIds[0]").value("classes"))
                .andExpect(jsonPath("$.onboardingCompletedAt").exists());

        ArgumentCaptor<CompleteTenantOnboardingRequest> requestCaptor =
                ArgumentCaptor.forClass(CompleteTenantOnboardingRequest.class);
        verify(tenantCommandService).completeOnboarding(any(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().hiddenSidebarItemIds()).containsExactly("classes", "reviews");
    }

    @Test
    @DisplayName("POST /v1/tenants/{slug}/complete-onboarding - 다른 테넌트 slug는 403")
    void completeOnboardingDifferentTenantForbidden() throws Exception {
        doThrow(new BusinessException(TenantErrorCode.TENANT_CONTEXT_MISMATCH,
                "요청 학원과 인증 학원이 일치하지 않습니다."))
                .when(tenantCommandService).completeOnboarding(any(), any());

        mockMvc.perform(post("/v1/tenants/test-academy/complete-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_019"));
    }

    @Test
    @DisplayName("POST /v1/tenants/{slug}/complete-onboarding - 학생은 403")
    void completeOnboardingStudentForbidden() throws Exception {
        doThrow(new BusinessException(CommonErrorCode.FORBIDDEN, "Owner role required"))
                .when(tenantCommandService).completeOnboarding(any(), any());

        mockMvc.perform(post("/v1/tenants/test-academy/complete-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON_006"));
    }

    @Test
    @DisplayName("POST /v1/tenants/{slug}/complete-onboarding - 매니저와 강사는 403")
    void completeOnboardingStaffForbidden() throws Exception {
        doThrow(new BusinessException(CommonErrorCode.FORBIDDEN, "Owner role required"))
                .when(tenantCommandService).completeOnboarding(any(), any());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/v1/tenants/test-academy/complete-onboarding")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {}
                                    """))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("COMMON_006"));
        }
    }

    @Test
    @DisplayName("DELETE /v1/tenants/{slug} - 테넌트 삭제 성공")
    void deleteTenantSuccess() throws Exception {
        setOwnerTenantContext();
        mockMvc.perform(delete("/v1/tenants/test-academy"))
                .andExpect(status().isNoContent());
    }

    @Test
    void ownerCannotMutateAnotherTenant() throws Exception {
        setOwnerTenantContext();
        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        mockMvc.perform(patch("/v1/tenants/another-academy")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));
        mockMvc.perform(delete("/v1/tenants/another-academy"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));
        mockMvc.perform(post("/v1/tenants/another-academy/suspend"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));
        mockMvc.perform(post("/v1/tenants/another-academy/reactivate"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));
        mockMvc.perform(multipart("/v1/tenants/another-academy/logo").file(logo))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));
        mockMvc.perform(delete("/v1/tenants/another-academy/logo"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("TENANT_019"));

        verifyNoInteractions(tenantCommandService, tenantLogoService, tenantQueryService);
    }

    @Test
    void managerCannotMutateOwnTenant() throws Exception {
        TenantContextHolder.setTenant("test-academy");
        UserContextHolder.setUserRole(Role.MANAGER);
        MockMultipartFile logo = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1});

        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));
        mockMvc.perform(delete("/v1/tenants/test-academy"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));
        mockMvc.perform(post("/v1/tenants/test-academy/suspend"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));
        mockMvc.perform(post("/v1/tenants/test-academy/reactivate"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));
        mockMvc.perform(multipart("/v1/tenants/test-academy/logo").file(logo))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));
        mockMvc.perform(delete("/v1/tenants/test-academy/logo"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("COMMON_006"));

        verifyNoInteractions(tenantCommandService, tenantLogoService, tenantQueryService);
    }

    @Test
    @DisplayName("GET /v1/tenants/{slug}/logo - 공개 캐시 가능한 로고 반환")
    void getLogoReturnsPublicCacheHeaders() throws Exception {
        byte[] logoBytes = new byte[] {1, 2, 3};
        when(tenantLogoService.downloadLogo("test-academy"))
                .thenReturn(new LogoStream(new ByteArrayInputStream(logoBytes), "image/png", logoBytes.length));

        mockMvc.perform(get("/v1/tenants/test-academy/logo"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "public, max-age=300"))
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void missingLogoUsesProblemDetail() throws Exception {
        mockMvc.perform(get("/v1/tenants/test-academy/logo"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/problem+json")))
                .andExpect(jsonPath("$.code").value("COMMON_003"));
    }

    @Test
    @DisplayName("GET /v1/tenants/public/current-branding - host tenant 로그인 브랜딩 반환")
    void getCurrentPublicTenantBrandingSuccess() throws Exception {
        PublicTenantResponse response = PublicTenantResponse.builder()
                .slug("test-academy")
                .name("Test Academy")
                .phone("02-1234-5678")
                .logoUrl(null)
                .loginTheme("default")
                .build();

        TenantContextHolder.setTenant("test-academy");
        when(tenantQueryService.getCurrentPublicTenant("test-academy"))
                .thenReturn(response);

        mockMvc.perform(get("/v1/tenants/public/current-branding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-academy"))
                .andExpect(jsonPath("$.name").value("Test Academy"))
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.loginTheme").value("default"));
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("GET /v1/tenants/public/current-branding - host tenant 없음 → 404")
    void getCurrentPublicTenantBrandingNotFound() throws Exception {
        mockMvc.perform(get("/v1/tenants/public/current-branding"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /v1/tenants/public/current-branding?host - SSR 공개 브랜딩을 host로 조회")
    void getCurrentPublicTenantBrandingForExplicitHost() throws Exception {
        PublicTenantResponse response = PublicTenantResponse.builder()
                .slug("inst-joossameng")
                .name("주혜연T")
                .phone(null)
                .logoUrl("https://assets.example.com/joossameng.png")
                .loginTheme("ocean")
                .build();
        when(tenantQueryService.getPublicTenantForHost("joossameng.com")).thenReturn(response);

        mockMvc.perform(get("/v1/tenants/public/current-branding")
                        .queryParam("host", "joossameng.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("inst-joossameng"))
                .andExpect(jsonPath("$.loginTheme").value("ocean"));
    }
}
