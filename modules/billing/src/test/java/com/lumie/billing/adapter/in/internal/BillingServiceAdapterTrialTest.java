package com.lumie.billing.adapter.in.internal;

import com.lumie.billing.application.dto.response.PlanLimitsResponse;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.service.AlimtalkCreditCommandService;
import com.lumie.billing.application.service.PlanQueryService;
import com.lumie.billing.application.service.SubscriptionCommandService;
import com.lumie.billing.application.service.SubscriptionQueryService;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.common.tenant.TenantContextHolder;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingServiceAdapterTrialTest {

    @Mock private SubscriptionQueryService subscriptionQueryService;
    @Mock private SubscriptionCommandService subscriptionCommandService;
    @Mock private PlanQueryService planQueryService;
    @Mock private AlimtalkCreditCommandService alimtalkCreditCommandService;
    @InjectMocks private BillingServiceAdapter adapter;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void createsTrialInsideRequestedTenantContextAndMapsResponse() {
        given(subscriptionCommandService.provisionTrial(22L, "academy-a")).willAnswer(invocation -> {
            assertThat(TenantContextHolder.getTenant()).isEqualTo("academy-a");
            assertThat(TenantContextHolder.getTenantId()).isEqualTo(22L);
            return SubscriptionResponse.builder()
                    .id(9L)
                    .tenantId(22L)
                    .tenantSlug("academy-a")
                    .planId("PRO")
                    .planName("Trial")
                    .status(SubscriptionStatus.ACTIVE)
                    .startedAt(Instant.EPOCH)
                    .expiresAt(null)
                    .limits(new PlanLimitsResponse(100, 1, 10, 500))
                    .build();
        });

        var result = adapter.createTrialSubscription(22L, "academy-a");

        assertThat(result.id()).isEqualTo(9L);
        assertThat(result.tenantSlug()).isEqualTo("academy-a");
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.expiresAt()).isNull();
        assertThat(result.limits().maxStudents()).isEqualTo(100);
        assertThat(TenantContextHolder.getTenant()).isNull();
        verify(subscriptionCommandService).provisionTrial(22L, "academy-a");
    }
}
