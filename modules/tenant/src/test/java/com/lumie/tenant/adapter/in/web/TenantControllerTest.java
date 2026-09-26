package com.lumie.tenant.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    @DisplayName("POST /v1/tenants - 테넌트 생성 성공")
    void createTenantSuccess() throws Exception {
        CreateTenantRequest request = CreateTenantRequest.builder()
                .slug("test-academy")
                .name("Test Academy")
                .displayName("테스트 학원")
                .build();

        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .displayName("테스트 학원")
                .status(TenantStatus.PENDING)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantCommandService.createTenant(any())).thenReturn(response);

        mockMvc.perform(post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("test-academy"))
                .andExpect(jsonPath("$.name").value("Test Academy"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /v1/tenants - 잘못된 slug 형식")
    void createTenantInvalidSlug() throws Exception {
        CreateTenantRequest request = CreateTenantRequest.builder()
                .slug("AB")
                .name("Test Academy")
                .build();

        mockMvc.perform(post("/v1/tenants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/tenants/{slug} - 테넌트 조회 성공")
    void getTenantSuccess() throws Exception {
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
        when(tenantQueryService.getTenantBySlug("non-existent"))
                .thenThrow(new ResourceNotFoundException("Tenant", "non-existent"));

        mockMvc.perform(get("/v1/tenants/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /v1/tenants/{slug} - 숨김 sidebar 항목 저장")
    void updateTenantHiddenSidebarItemsSuccess() throws Exception {
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
        mockMvc.perform(delete("/v1/tenants/test-academy"))
                .andExpect(status().isNoContent());
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
}
