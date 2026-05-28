package com.lumie.billing.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.ConfirmPaymentRequest;
import com.lumie.billing.application.service.PaymentService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PaymentController")
class PaymentControllerTest {

    private static final Long TENANT_ID = 20L;
    private static final String IDEMPOTENCY_KEY = "payment-confirm-01";

    @Mock private PaymentService paymentService;
    @Mock private IdempotencyService idempotencyService;
    @InjectMocks private PaymentController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
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
    @DisplayName("POST /v1/payments/confirm — Idempotency-Key 누락 시 400")
    void confirmPayment_missingIdempotencyKey_returns400() throws Exception {
        ConfirmPaymentRequest request = new ConfirmPaymentRequest("toss_key_abc", "order-001", 99_000L);

        mockMvc.perform(post("/v1/payments/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
