package com.lumie.billing.adapter.in.web;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.UserContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.SubscribeRequest;
import com.lumie.billing.application.dto.response.BillingKeyResponse;
import com.lumie.billing.application.dto.response.InvoiceResponse;
import com.lumie.billing.application.dto.response.PlanLimitsResponse;
import com.lumie.billing.application.dto.response.SubscribeResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.BillingSubscribeService;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.BillingKeyStatus;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.billing.domain.vo.SubscriptionStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("BillingSubscribeController")
class BillingSubscribeControllerTest {

    private static final Long TENANT_ID = 15L;
    private static final String TENANT_SLUG = "demo";
    private static final String IDEMPOTENCY_KEY = "subscribe-01";

    @Mock private BillingSubscribeService billingSubscribeService;
    @Mock private IdempotencyService idempotencyService;
    @InjectMocks private BillingSubscribeController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserRole(Role.OWNER);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        TenantContextHolder.setTenantId(TENANT_ID);
        TenantContextHolder.setTenant(TENANT_SLUG);

        // Pass-through executeOnce so we exercise the real service stub.
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
    @DisplayName("200 + body shape — happy path")
    void subscribe_success() throws Exception {
        SubscribeRequest request = new SubscribeRequest("bln_abc", "lumie-tenant-15", "PRO");
        SubscribeResponse mockResponse = stubSubscribeResponse();
        when(billingSubscribeService.subscribe(eq(TENANT_ID), eq(TENANT_SLUG), any(), eq(IDEMPOTENCY_KEY)))
                .thenReturn(mockResponse);

        mockMvc.perform(post("/v1/billing/subscribe")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subscription.planId").value("PRO"))
                .andExpect(jsonPath("$.billingKey.id").value(99))
                .andExpect(jsonPath("$.invoice.amount").value(99000));

        verify(billingSubscribeService).subscribe(eq(TENANT_ID), eq(TENANT_SLUG), any(), eq(IDEMPOTENCY_KEY));
    }

    @Test
    @DisplayName("Idempotency-Key 헤더 누락 → 400")
    void subscribe_missingIdempotencyKey() throws Exception {
        SubscribeRequest request = new SubscribeRequest("bln_abc", "lumie-tenant-15", "PRO");

        mockMvc.perform(post("/v1/billing/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BillingException PAYMENT_FAILED → 402")
    void subscribe_paymentFailed() throws Exception {
        SubscribeRequest request = new SubscribeRequest("bln_abc", "lumie-tenant-15", "PRO");
        when(billingSubscribeService.subscribe(any(), any(), any(), any()))
                .thenThrow(new BillingException(BillingErrorCode.PAYMENT_FAILED, "INVALID_CARD"));

        mockMvc.perform(post("/v1/billing/subscribe")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    @DisplayName("invalid body (blank planId) → 400 validation error")
    void subscribe_invalidBody() throws Exception {
        SubscribeRequest request = new SubscribeRequest("bln_abc", "lumie-tenant-15", "");

        mockMvc.perform(post("/v1/billing/subscribe")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private SubscribeResponse stubSubscribeResponse() {
        Instant now = Instant.now();
        SubscriptionResponse sub = SubscriptionResponse.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .tenantSlug(TENANT_SLUG)
                .planId("PRO")
                .planName("Pro")
                .status(SubscriptionStatus.ACTIVE)
                .billingKeyId(99L)
                .limits(PlanLimitsResponse.from(PlanLimits.pro()))
                .startedAt(now)
                .nextChargeAt(now.plusSeconds(86_400 * 30))
                .createdAt(now)
                .updatedAt(now)
                .build();
        BillingKeyResponse key = new BillingKeyResponse(99L, TENANT_ID,
                "1234-****-****-5678", "현대",
                BillingKeyStatus.ACTIVE, null, now);
        InvoiceResponse inv = InvoiceResponse.builder()
                .id(500L)
                .invoiceNumber("INV-15-20260528-AB")
                .subscriptionId(1L)
                .tenantId(TENANT_ID)
                .amount(99_000L)
                .vatAmount(0L)
                .createdAt(now)
                .build();
        return new SubscribeResponse(sub, key, inv);
    }
}
