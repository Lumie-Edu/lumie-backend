package com.lumie.billing.application.service;

import com.lumie.billing.application.dto.request.ChangeSubscriptionRequest;
import com.lumie.billing.application.dto.response.SubscriptionResponse;
import com.lumie.billing.application.port.out.InvoicePersistencePort;
import com.lumie.billing.application.port.out.PlanPersistencePort;
import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.domain.entity.Plan;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingErrorCode;
import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionCommandService — changePlan routing + undoCancellation")
class SubscriptionCommandServiceTest {

    private static final String TENANT_SLUG = "academy-test";
    private static final Long TENANT_ID = 7L;
    private static final Long BILLING_KEY_ID = 42L;

    @Mock private SubscriptionPersistencePort subscriptionPersistencePort;
    @Mock private PlanPersistencePort planPersistencePort;
    @Mock private InvoicePersistencePort invoicePersistencePort;
    @Mock private SubscriptionChargeExecutor chargeExecutor;
    @Mock private com.lumie.billing.application.port.out.BillingConfigPort billingConfig;

    @InjectMocks private SubscriptionCommandService service;

    private Plan proPlan;
    private Plan basicPlan;

    @BeforeEach
    void setUp() {
        proPlan = Plan.builder()
                .id("PRO").name("Pro")
                .monthlyPrice(Money.of(100_000L))
                .displayOrder(3).build();

        basicPlan = Plan.builder()
                .id("BASIC").name("Basic")
                .monthlyPrice(Money.of(50_000L))
                .displayOrder(2).build();
    }

    // ─── changePlan routing ────────────────────────────────────────────────────

    @Test
    @DisplayName("downgrade (100k → 50k) → scheduleDowngrade 호출, nextChargeAt 있을 때")
    void changePlan_downgradeRoutesToScheduleDowngrade() {
        Instant nextCharge = Instant.now().plusSeconds(86_400 * 15);
        Subscription subscription = paidActiveSubscription(proPlan, nextCharge);

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(subscription));
        given(planPersistencePort.findById("BASIC")).willReturn(Optional.of(basicPlan));
        given(subscriptionPersistencePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.changePlan(TENANT_SLUG,
                ChangeSubscriptionRequest.builder().newPlanId("BASIC").build());

        // plan stays PRO (not yet applied), pending is BASIC
        assertThat(response.planId()).isEqualTo("PRO");
        assertThat(response.pendingPlanId()).isEqualTo("BASIC");
        assertThat(response.scheduledChangeAt()).isEqualTo(nextCharge);
        verify(subscriptionPersistencePort).save(subscription);
    }

    @Test
    @DisplayName("upgrade (50k → 100k) → changePlan 즉시 적용")
    void changePlan_upgradeRoutesToChangePlan() {
        Instant nextCharge = Instant.now().plusSeconds(86_400 * 15);
        Subscription subscription = paidActiveSubscription(basicPlan, nextCharge);

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(subscription));
        given(planPersistencePort.findById("PRO")).willReturn(Optional.of(proPlan));
        given(subscriptionPersistencePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.changePlan(TENANT_SLUG,
                ChangeSubscriptionRequest.builder().newPlanId("PRO").build());

        assertThat(response.planId()).isEqualTo("PRO");
        assertThat(response.pendingPlanId()).isNull();
        assertThat(response.scheduledChangeAt()).isNull();
    }

    @Test
    @DisplayName("same price (50k → 50k different id) → changePlan 즉시 적용")
    void changePlan_samePriceRoutesToChangePlan() {
        Plan basicV2 = Plan.builder()
                .id("BASIC_V2").name("Basic v2")
                .monthlyPrice(Money.of(50_000L))
                .displayOrder(2).build();

        Instant nextCharge = Instant.now().plusSeconds(86_400 * 15);
        Subscription subscription = paidActiveSubscription(basicPlan, nextCharge);

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(subscription));
        given(planPersistencePort.findById("BASIC_V2")).willReturn(Optional.of(basicV2));
        given(subscriptionPersistencePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.changePlan(TENANT_SLUG,
                ChangeSubscriptionRequest.builder().newPlanId("BASIC_V2").build());

        assertThat(response.planId()).isEqualTo("BASIC_V2");
        assertThat(response.pendingPlanId()).isNull();
    }

    @Test
    @DisplayName("downgrade + nextChargeAt null → INVALID_SUBSCRIPTION_STATE (no silent fallback)")
    void changePlan_downgradeWithoutNextChargeAt_throws() {
        // A paid ACTIVE subscription with no next_charge_at signals corrupted
        // state — refuse rather than silently apply immediately. HARD_RULES
        // line 21: no workarounds around broken contracts.
        Subscription subscription = paidActiveSubscription(proPlan, null);

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(subscription));
        given(planPersistencePort.findById("BASIC")).willReturn(Optional.of(basicPlan));

        assertThatThrownBy(() -> service.changePlan(TENANT_SLUG,
                ChangeSubscriptionRequest.builder().newPlanId("BASIC").build()))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE)
                .hasMessageContaining("next_charge_at");
    }

    // ─── undoCancellation ─────────────────────────────────────────────────────

    @Test
    @DisplayName("undoCancellation: domain undoScheduledCancellation 호출 + save")
    void undoCancellation_callsDomainUndo() {
        Subscription subscription = paidActiveSubscription(proPlan,
                Instant.now().plusSeconds(86_400 * 10));
        // Schedule a cancellation first
        subscription.scheduleCancellation();

        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(subscription));
        given(subscriptionPersistencePort.save(any())).willAnswer(inv -> inv.getArgument(0));

        SubscriptionResponse response = service.undoCancellation(TENANT_SLUG);

        assertThat(response.cancelledAt()).isNull();
        verify(subscriptionPersistencePort).save(subscription);
    }

    @Test
    @DisplayName("undoCancellation: tenant 없음 → ResourceNotFoundException")
    void undoCancellation_tenantNotFound_throwsNotFound() {
        given(subscriptionPersistencePort.findActiveByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> service.undoCancellation(TENANT_SLUG))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── cancelSubscription: trial guard (domain mustBePaid) ─────────────────

    @Test
    @DisplayName("cancelSubscription on trial (no billingKeyId) → INVALID_SUBSCRIPTION_STATE")
    void cancelSubscription_onTrial_throws() {
        // Trial: ACTIVE, no billing key, has expiresAt
        Subscription trialSubscription = Subscription.create(TENANT_ID, TENANT_SLUG, proPlan,
                Instant.now().plusSeconds(86_400 * 30));
        // billingKeyId stays null → isTrial() = true

        given(subscriptionPersistencePort.findByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(trialSubscription));

        assertThatThrownBy(() -> service.cancelSubscription(TENANT_SLUG))
                .isInstanceOf(BillingException.class)
                .hasFieldOrPropertyWithValue("errorCode", BillingErrorCode.INVALID_SUBSCRIPTION_STATE);
    }

    // ─── provisionTrial (A-flow) ──────────────────────────────────────────────

    @Test
    @DisplayName("provisionTrial_newTenant_savesTrialProSubscription")
    void provisionTrial_newTenant_savesTrialProSubscription() {
        given(subscriptionPersistencePort.findByTenantSlug(TENANT_SLUG)).willReturn(Optional.empty());
        given(planPersistencePort.findById("PRO")).willReturn(Optional.of(proPlan));
        given(subscriptionPersistencePort.save(any(Subscription.class)))
                .willAnswer(inv -> {
                    Subscription saved = inv.getArgument(0);
                    ReflectionTestUtils.setField(saved, "id", 999L);
                    return saved;
                });

        SubscriptionResponse response = service.provisionTrial(TENANT_ID, TENANT_SLUG);

        assertThat(response.planId()).isEqualTo("PRO");
        org.mockito.ArgumentCaptor<Subscription> captor = org.mockito.ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionPersistencePort).save(captor.capture());
        Subscription saved = captor.getValue();
        assertThat(saved.isTrial()).isTrue();
        assertThat(saved.getBillingKeyId()).isNull();
        assertThat(saved.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("provisionTrial_existingSubscription_returnsExisting_noSave")
    void provisionTrial_existingSubscription_returnsExisting_noSave() {
        Subscription existing = Subscription.create(TENANT_ID, TENANT_SLUG, proPlan, null);
        ReflectionTestUtils.setField(existing, "id", 1L);
        given(subscriptionPersistencePort.findByTenantSlug(TENANT_SLUG))
                .willReturn(Optional.of(existing));

        SubscriptionResponse response = service.provisionTrial(TENANT_ID, TENANT_SLUG);

        assertThat(response.id()).isEqualTo(1L);
        verify(subscriptionPersistencePort, org.mockito.Mockito.never()).save(any());
        verify(planPersistencePort, org.mockito.Mockito.never()).findById(any());
    }

    @Test
    @DisplayName("provisionTrial_proPlanMissing_throwsIllegalState")
    void provisionTrial_proPlanMissing_throwsIllegalState() {
        given(subscriptionPersistencePort.findByTenantSlug(TENANT_SLUG)).willReturn(Optional.empty());
        given(planPersistencePort.findById("PRO")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.provisionTrial(TENANT_ID, TENANT_SLUG))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PRO")
                .hasMessageContaining("V34");

        verify(subscriptionPersistencePort, org.mockito.Mockito.never()).save(any());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Subscription paidActiveSubscription(Plan plan, Instant nextChargeAt) {
        Subscription sub = Subscription.create(TENANT_ID, TENANT_SLUG, plan, null);
        ReflectionTestUtils.setField(sub, "id", 1L);
        sub.attachBillingKey(BILLING_KEY_ID);
        if (nextChargeAt != null) {
            sub.updateNextChargeAt(nextChargeAt);
        }
        return sub;
    }
}
