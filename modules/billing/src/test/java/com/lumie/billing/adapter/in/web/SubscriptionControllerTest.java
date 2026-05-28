package com.lumie.billing.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.request.CreateSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SubscriptionController — mutation endpoints")
class SubscriptionControllerTest {

    private static final String TENANT_SLUG = "academy-b";
    private static final String IDEMPOTENCY_KEY = "sub-op-01";

    @Mock private SubscriptionCommandService commandService;
    @Mock private SubscriptionQueryService queryService;
    @Mock private IdempotencyService idempotencyService;
    @InjectMocks private SubscriptionController controller;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(idempotencyService.executeOnce(any(), any(), any(), any())).thenAnswer(inv -> {
            Supplier<ResponseEntity<?>> supplier = inv.getArgument(3);
            return supplier.get();
        });
    }

    // ── createSubscription ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /v1/subscriptions — Idempotency-Key 누락 시 400")
    void createSubscription_missingIdempotencyKey_returns400() throws Exception {
        CreateSubscriptionRequest request = CreateSubscriptionRequest.builder()
                .tenantId(5L)
                .tenantSlug(TENANT_SLUG)
                .planId("PRO")
                .build();

        mockMvc.perform(post("/v1/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── changePlan ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /v1/subscriptions/{slug} — Idempotency-Key 누락 시 400")
    void changePlan_missingIdempotencyKey_returns400() throws Exception {
        ChangeSubscriptionRequest request = ChangeSubscriptionRequest.builder()
                .newPlanId("BASIC")
                .build();

        mockMvc.perform(patch("/v1/subscriptions/{slug}", TENANT_SLUG)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── cancelSubscription ──────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/subscriptions/{slug} — Idempotency-Key 누락 시 400")
    void cancelSubscription_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/v1/subscriptions/{slug}", TENANT_SLUG))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /v1/subscriptions/{slug} — 204 happy path")
    void cancelSubscription_success() throws Exception {
        mockMvc.perform(delete("/v1/subscriptions/{slug}", TENANT_SLUG)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isNoContent());
    }

    // ── undoCancellation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /v1/subscriptions/{slug}/cancellation — Idempotency-Key 누락 시 400")
    void undoCancellation_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(delete("/v1/subscriptions/{slug}/cancellation", TENANT_SLUG))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /v1/subscriptions/{slug}/cancellation — 200 happy path")
    void undoCancellation_happyPath_returns200() throws Exception {
        SubscriptionResponse mockResponse = SubscriptionResponse.builder()
                .id(1L)
                .tenantId(7L)
                .tenantSlug(TENANT_SLUG)
                .planId("PRO")
                .planName("Pro")
                .status(SubscriptionStatus.ACTIVE)
                .suspensionReason(SuspensionReason.NONE)
                .startedAt(Instant.now())
                .limits(new com.lumie.billing.application.dto.response.PlanLimitsResponse(null, null, null, null))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(commandService.undoCancellation(eq(TENANT_SLUG))).willReturn(mockResponse);

        mockMvc.perform(delete("/v1/subscriptions/{slug}/cancellation", TENANT_SLUG)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY))
                .andExpect(status().isOk());
    }
}
