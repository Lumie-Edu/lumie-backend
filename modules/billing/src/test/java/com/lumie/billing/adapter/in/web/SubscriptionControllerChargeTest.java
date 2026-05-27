package com.lumie.billing.adapter.in.web;

import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.InvoiceStatus;
import com.lumie.common.exception.GlobalExceptionHandler;
import com.lumie.common.idempotency.IdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionController.chargeFirstInvoice")
class SubscriptionControllerChargeTest {

    private static final String TENANT_SLUG = "academy-a";
    private static final String IDEMPOTENCY_KEY = "charge-01HQXY";

    @Mock private SubscriptionCommandService commandService;
    @Mock private SubscriptionQueryService queryService;
    @Mock private IdempotencyService idempotencyService;

    @InjectMocks private SubscriptionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Pass-through: invoke the supplier so the test exercises the service path.
        when(idempotencyService.executeOnce(any(), any(), any(), any())).thenAnswer(inv -> {
            Supplier<ResponseEntity<?>> supplier = inv.getArgument(3);
            return supplier.get();
        });
    }

    @Test
    @DisplayName("POST /v1/subscriptions/{slug}/charge — 결제 성공")
    void chargeFirstInvoice_success() throws Exception {
        InvoiceResponse paid = InvoiceResponse.builder()
                .id(500L)
                .invoiceNumber("INV-42-20260527-ABC")
                .subscriptionId(101L)
                .tenantId(42L)
                .amount(29_000L)
                .vatAmount(0L)
                .status(InvoiceStatus.PAID)
                .paidAt(Instant.now())
                .paymentKey("toss_payment_key_abc")
                .createdAt(Instant.now())
                .build();
        when(commandService.chargeFirstInvoice(TENANT_SLUG)).thenReturn(paid);

        mockMvc.perform(post("/v1/subscriptions/{slug}/charge", TENANT_SLUG)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.amount").value(29_000));

        verify(idempotencyService).executeOnce(eq(IDEMPOTENCY_KEY), any(), eq(TENANT_SLUG), any());
        verify(commandService).chargeFirstInvoice(TENANT_SLUG);
    }

    @Test
    @DisplayName("POST /v1/subscriptions/{slug}/charge — Idempotency-Key 누락 시 400")
    void chargeFirstInvoice_missingIdempotencyKey() throws Exception {
        mockMvc.perform(post("/v1/subscriptions/{slug}/charge", TENANT_SLUG))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /v1/subscriptions/{slug}/charge — BillingKey 미연결 → 404 매핑")
    void chargeFirstInvoice_billingKeyMissing() throws Exception {
        when(commandService.chargeFirstInvoice(TENANT_SLUG))
                .thenThrow(new BillingException(BillingErrorCode.BILLING_KEY_NOT_FOUND,
                        "Subscription has no billing key attached: " + TENANT_SLUG));

        mockMvc.perform(post("/v1/subscriptions/{slug}/charge", TENANT_SLUG)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /v1/subscriptions/{slug}/charge — PG 결제 실패 → 402 매핑")
    void chargeFirstInvoice_pgFailure() throws Exception {
        when(commandService.chargeFirstInvoice(TENANT_SLUG))
                .thenThrow(new BillingException(BillingErrorCode.PAYMENT_FAILED, "INVALID_CARD"));

        mockMvc.perform(post("/v1/subscriptions/{slug}/charge", TENANT_SLUG)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isPaymentRequired());
    }
}
