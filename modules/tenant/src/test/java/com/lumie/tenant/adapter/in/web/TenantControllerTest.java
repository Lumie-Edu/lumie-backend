package com.lumie.tenant.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.exception.CommonErrorCode;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.tenant.application.dto.request.CompleteTenantOnboardingRequest;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.request.UpdateTenantRequest;
import com.lumie.tenant.application.dto.response.PublicTenantResponse;
import com.lumie.tenant.application.dto.response.TenantResponse;
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
        // Wrap the controller in a Spring AOP proxy so that @Validated method-level
        // constraints (@NotBlank on @RequestParam) fire as ConstraintViolationException.
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
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .build();
        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .status(TenantStatus.ACTIVE)
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantCommandService.updateTenant(any(), any())).thenReturn(response);

        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hiddenSidebarItemIds[0]").value("attendance"))
                .andExpect(jsonPath("$.hiddenSidebarItemIds[1]").value("reviews"));

        ArgumentCaptor<UpdateTenantRequest> requestCaptor = ArgumentCaptor.forClass(UpdateTenantRequest.class);
        verify(tenantCommandService).updateTenant(any(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().hiddenSidebarItemIds()).containsExactly("attendance", "reviews");
    }

    @Test
    @DisplayName("PATCH /v1/tenants/{slug} - 잘못된 sidebar 항목 형식")
    void updateTenantInvalidHiddenSidebarItemFormat() throws Exception {
        mockMvc.perform(patch("/v1/tenants/test-academy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hiddenSidebarItemIds":["Attendance"]}
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
                .customId("academy")
                .phone("02-1234-5678")
                .address("서울시 강남구")
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .build();
        Instant completedAt = Instant.now();
        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .status(TenantStatus.ACTIVE)
                .customId("academy")
                .phone("02-1234-5678")
                .address("서울시 강남구")
                .hiddenSidebarItemIds(List.of("attendance", "reviews"))
                .onboardingCompletedAt(completedAt)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(tenantCommandService.completeOnboarding(any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/tenants/test-academy/complete-onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customId").value("academy"))
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.hiddenSidebarItemIds[0]").value("attendance"))
                .andExpect(jsonPath("$.onboardingCompletedAt").exists());

        ArgumentCaptor<CompleteTenantOnboardingRequest> requestCaptor =
                ArgumentCaptor.forClass(CompleteTenantOnboardingRequest.class);
        verify(tenantCommandService).completeOnboarding(any(), requestCaptor.capture());
        assertThat(requestCaptor.getValue().hiddenSidebarItemIds()).containsExactly("attendance", "reviews");
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
                                {"customId":"academy"}
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
                                {"customId":"academy"}
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
                                    {"customId":"academy"}
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
    @DisplayName("GET /v1/tenants/public/by-domain?host=... - 활성 테넌트 반환 (공개 필드만)")
    void getPublicTenantByCustomDomainSuccess() throws Exception {
        PublicTenantResponse response = PublicTenantResponse.builder()
                .slug("test-academy")
                .name("Test Academy")
                .phone("02-1234-5678")
                .address("서울시 강남구")
                .customId("test-academy")
                .logoUrl(null)
                .build();

        when(tenantQueryService.getPublicTenantByCustomDomain("academy.example.com"))
                .thenReturn(response);

        mockMvc.perform(get("/v1/tenants/public/by-domain")
                        .param("host", "academy.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("test-academy"))
                .andExpect(jsonPath("$.name").value("Test Academy"))
                .andExpect(jsonPath("$.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.address").value("서울시 강남구"));
    }

    @Test
    @DisplayName("GET /v1/tenants/public/by-domain?host=... - 일치하는 테넌트 없음 → 404")
    void getPublicTenantByCustomDomainNotFound() throws Exception {
        when(tenantQueryService.getPublicTenantByCustomDomain("unknown.example.com"))
                .thenThrow(new ResourceNotFoundException("Tenant", "unknown.example.com"));

        mockMvc.perform(get("/v1/tenants/public/by-domain")
                        .param("host", "unknown.example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /v1/tenants/public/by-domain - host 파라미터 누락 → 400")
    void getPublicTenantByCustomDomainMissingParam() throws Exception {
        mockMvc.perform(get("/v1/tenants/public/by-domain"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/tenants/public/by-domain?host=  ABC.com  - 컨트롤러가 소문자+트림 후 서비스 호출")
    void getPublicTenantByCustomDomainNormalizesHostBeforeService() throws Exception {
        PublicTenantResponse response = PublicTenantResponse.builder()
                .slug("test-academy")
                .name("Test Academy")
                .phone("02-1234-5678")
                .address("서울시 강남구")
                .customId("test-academy")
                .logoUrl(null)
                .build();

        // The controller normalizes "  ABC.com  " → "abc.com" before calling the service.
        when(tenantQueryService.getPublicTenantByCustomDomain("abc.com")).thenReturn(response);

        mockMvc.perform(get("/v1/tenants/public/by-domain")
                        .param("host", "  ABC.com  "))
                .andExpect(status().isOk());

        ArgumentCaptor<String> hostCaptor = ArgumentCaptor.forClass(String.class);
        verify(tenantQueryService).getPublicTenantByCustomDomain(hostCaptor.capture());
        assertThat(hostCaptor.getValue()).isEqualTo("abc.com");
    }

    @Test
    @DisplayName("GET /v1/tenants/public/by-domain?host= (빈 문자열) → 400 (@NotBlank violation)")
    void blankHostReturns400() throws Exception {
        // @NotBlank on the @RequestParam rejects an empty string.
        // ConstraintViolationException is mapped to 400 by GlobalExceptionHandler.
        mockMvc.perform(get("/v1/tenants/public/by-domain")
                        .param("host", ""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /v1/tenants/public/by-domain?host=%20%20 (공백만) → 400 (@NotBlank violation)")
    void whitespaceOnlyHostReturns400() throws Exception {
        // @NotBlank treats whitespace-only strings as blank.
        // After normalization "  ".trim() == "" but @NotBlank fires before any domain logic.
        mockMvc.perform(get("/v1/tenants/public/by-domain")
                        .param("host", "  "))
                .andExpect(status().isBadRequest());
    }
}
