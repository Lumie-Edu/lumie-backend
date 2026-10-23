package com.lumie.billing.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.ConfigureAutoRechargeRequest;
import com.lumie.billing.application.dto.request.RechargeAlimtalkCreditRequest;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.AlimtalkCreditQueryService;
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

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AlimtalkCreditController")
class AlimtalkCreditControllerTest {

    private static final Long TENANT_ID = 30L;
    private static final String IDEMPOTENCY_KEY = "alimtalk-op-01";

    @Mock private AlimtalkCreditCommandService commandService;
    @Mock private AlimtalkCreditQueryService queryService;
    @Mock private IdempotencyService idempotencyService;
    @InjectMocks private AlimtalkCreditController controller;

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

    // ── recharge ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/alimtalk/credits/recharge — Idempotency-Key 누락 시 400")
    void recharge_missingIdempotencyKey_returns400() throws Exception {
        RechargeAlimtalkCreditRequest request = new RechargeAlimtalkCreditRequest(500);

        mockMvc.perform(post("/v1/alimtalk/credits/recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── configureAutoRecharge ────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /v1/alimtalk/credits/auto-recharge — Idempotency-Key 누락 시 400")
    void configureAutoRecharge_missingIdempotencyKey_returns400() throws Exception {
        ConfigureAutoRechargeRequest request = new ConfigureAutoRechargeRequest(100, 500);

        mockMvc.perform(put("/v1/alimtalk/credits/auto-recharge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── disableAutoRecharge ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/alimtalk/credits/auto-recharge — Idempotency-Key 누락 시 400")
    void disableAutoRecharge_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/v1/alimtalk/credits/auto-recharge"))
                .andExpect(status().isBadRequest());
    }
}
