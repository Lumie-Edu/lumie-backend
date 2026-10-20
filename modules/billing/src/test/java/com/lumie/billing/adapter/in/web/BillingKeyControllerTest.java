package com.lumie.billing.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.RegisterBillingKeyRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.service.BillingKeyCommandService;
import com.lumie.billing.application.service.BillingKeyQueryService;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import com.lumie.common.tenant.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingKeyController")
class BillingKeyControllerTest {

    private static final Long TENANT_ID = 10L;
    private static final String IDEMPOTENCY_KEY = "billing-key-01";

    @Mock private BillingKeyCommandService billingKeyCommandService;
    @Mock private BillingKeyQueryService billingKeyQueryService;
    @Mock private IdempotencyService idempotencyService;
    @InjectMocks private BillingKeyController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserRole(Role.OWNER);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(TENANT_ID);

        when(idempotencyService.executeOnce(any(), any(), any(), any())).thenAnswer(inv -> {
            Supplier<ResponseEntity<?>> supplier = inv.getArgument(3);
            return supplier.get();
        });
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("POST /v1/billing-keys — Idempotency-Key 누락 시 400")
    void registerBillingKey_missingIdempotencyKey_returns400() throws Exception {
        RegisterBillingKeyRequest request = new RegisterBillingKeyRequest("auth_key_abc", "customer_key_xyz");

        mockMvc.perform(post("/v1/billing-keys")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/billing-keys — 201 happy path")
    void registerBillingKey_success() throws Exception {
        RegisterBillingKeyRequest request = new RegisterBillingKeyRequest("auth_key_abc", "customer_key_xyz");
        BillingKeyResponse response = new BillingKeyResponse(
                1L, TENANT_ID, "1234-****-****-5678", "현대",
                BillingKeyStatus.ACTIVE, null, Instant.now());
        when(billingKeyCommandService.registerBillingKey(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/billing-keys")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /v1/billing-keys/{id} — Idempotency-Key 누락 시 400")
    void revokeBillingKey_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/v1/billing-keys/42"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /v1/billing-keys/{id} — 204 happy path")
    void revokeBillingKey_success() throws Exception {
        mockMvc.perform(delete("/v1/billing-keys/42")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isNoContent());
    }
}
