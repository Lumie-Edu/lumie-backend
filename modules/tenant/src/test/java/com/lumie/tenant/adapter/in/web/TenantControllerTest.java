package com.lumie.tenant.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.common.exception.ResourceNotFoundException;
import com.lumie.tenant.application.dto.request.CreateTenantRequest;
import com.lumie.tenant.application.dto.response.TenantResponse;
import com.lumie.tenant.application.service.TenantCommandService;
import com.lumie.tenant.application.service.TenantLogoService;
import com.lumie.tenant.application.service.TenantQueryService;
import com.lumie.tenant.api.TenantStatus;
import com.lumie.common.exception.GlobalExceptionHandler;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TenantControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TenantCommandService tenantCommandService;

    @Mock
    private TenantQueryService tenantQueryService;

    @Mock
    private TenantLogoService tenantLogoService;

    @InjectMocks
    private TenantController tenantController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tenantController)
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
                .ownerEmail("owner@test.com")
                .build();

        TenantResponse response = TenantResponse.builder()
                .id(1L)
                .slug("test-academy")
                .name("Test Academy")
                .displayName("테스트 학원")
                .status(TenantStatus.PENDING)
                .ownerEmail("owner@test.com")
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
    @DisplayName("DELETE /v1/tenants/{slug} - 테넌트 삭제 성공")
    void deleteTenantSuccess() throws Exception {
        mockMvc.perform(delete("/v1/tenants/test-academy"))
                .andExpect(status().isNoContent());
    }
}
