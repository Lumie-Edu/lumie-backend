package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionTest {

    @Test
    void createFree_initializes_as_ACTIVE() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.NONE);
        assertThat(sub.getPaymentFailureCount()).isZero();
    }

    @Test
    void cancel_transitions_to_CANCELLED() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.cancel();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        assertThat(sub.getCancelledAt()).isNotNull();
    }

    @Test
    void cancel_already_cancelled_throws() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);
        sub.cancel();

        assertThatThrownBy(sub::cancel).isInstanceOf(BillingException.class);
    }

    @Test
    void pause_transitions_ACTIVE_to_PAUSED() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.pause(SuspensionReason.MANUAL_PAUSE);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.MANUAL_PAUSE);
    }

    @Test
    void resume_transitions_PAUSED_to_ACTIVE() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);
        sub.pause(SuspensionReason.MANUAL_PAUSE);

        sub.resume();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.NONE);
        assertThat(sub.getPaymentFailureCount()).isZero();
    }

    @Test
    void resume_non_paused_throws() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        assertThatThrownBy(sub::resume).isInstanceOf(BillingException.class);
    }

    @Test
    void chargeAttemptFailed_three_times_pauses_subscription() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.chargeAttemptFailed();
        sub.chargeAttemptFailed();
        assertThat(sub.getStatus()).isNotEqualTo(SubscriptionStatus.PAUSED);

        sub.chargeAttemptFailed();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.PAYMENT_FAILED_3X);
        assertThat(sub.getPaymentFailureCount()).isEqualTo(3);
    }

    @Test
    void attachBillingKey_sets_billingKeyId() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.attachBillingKey(42L);

        assertThat(sub.getBillingKeyId()).isEqualTo(42L);
        assertThat(sub.hasBillingKey()).isTrue();
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Plan mockFreePlan() {
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn("FREE");
        when(plan.getName()).thenReturn("Free Plan");
        when(plan.getMonthlyPrice()).thenReturn(com.lumie.billing.domain.vo.Money.zero());
        when(plan.toLimits()).thenReturn(
                com.lumie.billing.domain.vo.PlanLimits.builder()
                        .maxStudents(50)
                        .maxAcademies(1)
                        .maxAdmins(2)
                        .omrMonthlyQuota(10)
                        .build());
        return plan;
    }
}
