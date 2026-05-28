package com.lumie.billing.domain.entity;

import com.lumie.billing.domain.exception.BillingException;
import com.lumie.billing.domain.vo.Money;
import com.lumie.billing.domain.vo.PlanLimits;
import com.lumie.billing.domain.vo.SubscriptionStatus;
import com.lumie.billing.domain.vo.SuspensionReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriptionTest {

    // ─── Existing lifecycle tests (preserved) ─────────────────────────────────

    @Test
    @DisplayName("createFree: status=ACTIVE, suspensionReason=NONE, failureCount=0")
    void createFree_initializes_as_ACTIVE() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.NONE);
        assertThat(sub.getPaymentFailureCount()).isZero();
    }

    @Test
    @DisplayName("pause(ACTIVE) → PAUSED with reason")
    void pause_transitions_ACTIVE_to_PAUSED() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.pause(SuspensionReason.MANUAL_PAUSE);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.PAUSED);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.MANUAL_PAUSE);
    }

    @Test
    @DisplayName("resume(PAUSED) → ACTIVE, failureCount reset")
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
    @DisplayName("resume(ACTIVE) → BillingException")
    void resume_non_paused_throws() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        assertThatThrownBy(sub::resume).isInstanceOf(BillingException.class);
    }

    @Test
    @DisplayName("chargeAttemptFailed x3 → PAUSED(PAYMENT_FAILED_3X)")
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
    @DisplayName("attachBillingKey: sets billingKeyId, hasBillingKey()=true")
    void attachBillingKey_sets_billingKeyId() {
        Plan plan = mockFreePlan();
        Subscription sub = Subscription.createFree(1L, "test-academy", plan);

        sub.attachBillingKey(42L);

        assertThat(sub.getBillingKeyId()).isEqualTo(42L);
        assertThat(sub.hasBillingKey()).isTrue();
    }

    // ─── B-flow: createTrial ──────────────────────────────────────────────────

    @Test
    @DisplayName("createTrial: billingKeyId=null, expiresAt≈now+30d, status=ACTIVE")
    void createTrial_setsTrialFields() {
        Plan proPlan = mockPaidPlan("PRO");
        Instant before = Instant.now();

        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getBillingKeyId()).isNull();
        assertThat(sub.getPlanId()).isEqualTo("PRO");
        assertThat(sub.getExpiresAt()).isNotNull();
        assertThat(sub.getExpiresAt()).isAfter(before.plus(Duration.ofDays(29)));
        assertThat(sub.getExpiresAt()).isBefore(before.plus(Duration.ofDays(31)));
        assertThat(sub.isTrial()).isTrue();
    }

    // ─── C-flow: subscribe (Trial → Paid) ────────────────────────────────────

    @Test
    @DisplayName("subscribe(Trial→Paid): billingKeyId set, expiresAt cleared, status=ACTIVE")
    void subscribe_fromTrial_setsBillingKey_clearsExpires() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);
        assertThat(sub.getBillingKeyId()).isNull();
        assertThat(sub.getExpiresAt()).isNotNull();

        BillingKey key = mockBillingKey(99L);
        Instant nextCharge = Instant.now().plus(Duration.ofDays(30));
        Instant startedAtBefore = sub.getStartedAt();

        sub.subscribe(key, proPlan, nextCharge);

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getBillingKeyId()).isEqualTo(99L);
        assertThat(sub.getExpiresAt()).isNull();
        assertThat(sub.getCancelledAt()).isNull();
        assertThat(sub.getNextChargeAt()).isEqualTo(nextCharge);
        assertThat(sub.getStartedAt()).isCloseTo(startedAtBefore, within(2, SECONDS));
    }

    @Test
    @DisplayName("subscribe(ACTIVE→Paid): startedAt preserved")
    void subscribe_fromActive_keepsStartedAt() {
        Plan freePlan = mockFreePlan();
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", freePlan);
        Instant startedAtBefore = sub.getStartedAt();

        BillingKey key = mockBillingKey(1L);
        sub.subscribe(key, proPlan, Instant.now().plus(Duration.ofDays(30)));

        assertThat(sub.getStartedAt()).isCloseTo(startedAtBefore, within(2, SECONDS));
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getPlanId()).isEqualTo("PRO");
    }

    @Test
    @DisplayName("subscribe clears any pending downgrade")
    void subscribe_clearsPendingDowngrade() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(10L);
        sub.scheduleDowngrade(basicPlan, Instant.now().plus(Duration.ofDays(15)));
        assertThat(sub.getPendingPlan()).isNotNull();

        BillingKey key = mockBillingKey(20L);
        sub.subscribe(key, proPlan, Instant.now().plus(Duration.ofDays(30)));

        assertThat(sub.getPendingPlan()).isNull();
        assertThat(sub.getScheduledChangeAt()).isNull();
    }

    // ─── D-flow: subscribe (EXPIRED/CANCELLED → Paid fresh start) ─────────────

    @Test
    @DisplayName("subscribe(EXPIRED→Paid): startedAt reset, status=ACTIVE")
    void subscribe_fromExpired_resetsStartedAt() {
        Plan freePlan = mockFreePlan();
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", freePlan);
        sub.expire();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        Instant startedAtBefore = sub.getStartedAt();

        BillingKey key = mockBillingKey(5L);
        sub.subscribe(key, proPlan, Instant.now().plus(Duration.ofDays(30)));

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getPlanId()).isEqualTo("PRO");
        assertThat(sub.getStartedAt()).isAfter(startedAtBefore);
        assertThat(sub.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("subscribe(CANCELLED→Paid): startedAt reset, cancelledAt cleared, status=ACTIVE")
    void subscribe_fromCancelled_resetsStartedAt_clearsCancelledAt() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleCancellation();
        sub.finalizeCancellation();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        Instant startedAtBefore = sub.getStartedAt();

        BillingKey key = mockBillingKey(10L);
        sub.subscribe(key, proPlan, Instant.now().plus(Duration.ofDays(30)));

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.getCancelledAt()).isNull();
        assertThat(sub.getStartedAt()).isAfter(startedAtBefore);
        assertThat(sub.getSuspensionReason()).isEqualTo(SuspensionReason.NONE);
        assertThat(sub.getPaymentFailureCount()).isZero();
    }

    // ─── E-flow: changePlan (immediate swap) ──────────────────────────────────

    @Test
    @DisplayName("changePlan: swaps plan, clears cancelledAt and pendingPlan")
    void changePlan_swapsPlan_clearsCancelledAt_clearsPending() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Plan elitePlan = mockPaidPlan("ELITE");

        // Arrange: ACTIVE + pending downgrade + cancel grace
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleDowngrade(basicPlan, Instant.now().plus(Duration.ofDays(15)));
        assertThat(sub.getPendingPlan()).isNotNull();  // downgrade scheduled
        sub.scheduleCancellation();
        assertThat(sub.getCancelledAt()).isNotNull();  // cancel grace active

        // Act: changePlan overrides everything
        sub.changePlan(elitePlan);

        // Assert
        assertThat(sub.getPlanId()).isEqualTo("ELITE");
        assertThat(sub.getCancelledAt()).isNull();
        assertThat(sub.getPendingPlan()).isNull();
        assertThat(sub.getScheduledChangeAt()).isNull();
    }

    @Test
    @DisplayName("changePlan on Trial (no billing key) → BillingException(mustBePaid)")
    void changePlan_onTrial_throwsMustBePaid() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);

        assertThatThrownBy(() -> sub.changePlan(proPlan))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("paid subscription");
    }

    @Test
    @DisplayName("changePlan on PAUSED → BillingException(INVALID_SUBSCRIPTION_STATE)")
    void changePlan_onPaused_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.pause(SuspensionReason.MANUAL_PAUSE);

        assertThatThrownBy(() -> sub.changePlan(basicPlan))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("ACTIVE");
    }

    // ─── F-flow: scheduleDowngrade ─────────────────────────────────────────────

    @Test
    @DisplayName("scheduleDowngrade(lowerPrice): sets pendingPlan+scheduledAt, clears cancelledAt")
    void scheduleDowngrade_lowerPrice_setsFields_clearsCancelledAt() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        // Put in cancel-grace
        sub.scheduleCancellation();
        assertThat(sub.getCancelledAt()).isNotNull();

        Instant scheduledAt = Instant.now().plus(Duration.ofDays(30));
        sub.scheduleDowngrade(basicPlan, scheduledAt);

        assertThat(sub.getPendingPlan()).isEqualTo(basicPlan);
        assertThat(sub.getScheduledChangeAt()).isEqualTo(scheduledAt);
        assertThat(sub.getCancelledAt()).isNull();
        assertThat(sub.getPlanId()).isEqualTo("PRO");  // current plan unchanged
    }

    @Test
    @DisplayName("scheduleDowngrade(samePrice) → BillingException")
    void scheduleDowngrade_samePrice_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan samePlan = mockPaidPlan("PRO2");  // same price 99_000
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        assertThatThrownBy(() -> sub.scheduleDowngrade(samePlan, Instant.now().plus(Duration.ofDays(30))))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("lower-priced");
    }

    @Test
    @DisplayName("scheduleDowngrade(higherPrice) → BillingException")
    void scheduleDowngrade_higherPrice_throws() {
        Plan basicPlan = mockCheaperPlan("BASIC");
        Plan proPlan = mockPaidPlan("PRO");  // more expensive
        Subscription sub = Subscription.createFree(1L, "demo", basicPlan);
        sub.attachBillingKey(1L);

        assertThatThrownBy(() -> sub.scheduleDowngrade(proPlan, Instant.now().plus(Duration.ofDays(30))))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("lower-priced");
    }

    @Test
    @DisplayName("scheduleDowngrade on Trial → BillingException(mustBePaid)")
    void scheduleDowngrade_onTrial_throwsMustBePaid() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);

        assertThatThrownBy(() -> sub.scheduleDowngrade(basicPlan, Instant.now().plus(Duration.ofDays(30))))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("paid subscription");
    }

    @Test
    @DisplayName("scheduleDowngrade on PAUSED → BillingException")
    void scheduleDowngrade_onPaused_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.pause(SuspensionReason.MANUAL_PAUSE);

        assertThatThrownBy(() -> sub.scheduleDowngrade(basicPlan, Instant.now().plus(Duration.ofDays(30))))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("scheduleDowngrade(null args) → BillingException")
    void scheduleDowngrade_nullArgs_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        assertThatThrownBy(() -> sub.scheduleDowngrade(null, Instant.now().plus(Duration.ofDays(30))))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("required");

        assertThatThrownBy(() -> sub.scheduleDowngrade(mockCheaperPlan("BASIC"), null))
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("required");
    }

    // ─── G-flow: scheduleCancellation / finalizeCancellation ─────────────────

    @Test
    @DisplayName("scheduleCancellation(ACTIVE paid): sets cancelledAt, status stays ACTIVE")
    void scheduleCancellation_active_paid_setsCancelledAt() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        sub.scheduleCancellation();

        assertThat(sub.getCancelledAt()).isNotNull();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.isCancellationScheduled()).isTrue();
    }

    @Test
    @DisplayName("scheduleCancellation on Trial → BillingException(mustBePaid)")
    void scheduleCancellation_onTrial_throwsMustBePaid() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);

        assertThatThrownBy(sub::scheduleCancellation)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("paid subscription");
    }

    @Test
    @DisplayName("scheduleCancellation when already scheduled → BillingException")
    void scheduleCancellation_alreadyCancelled_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleCancellation();

        assertThatThrownBy(sub::scheduleCancellation)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("already scheduled");
    }

    @Test
    @DisplayName("scheduleCancellation on PAUSED → BillingException")
    void scheduleCancellation_onPaused_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.pause(SuspensionReason.MANUAL_PAUSE);

        assertThatThrownBy(sub::scheduleCancellation)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("ACTIVE");
    }

    @Test
    @DisplayName("finalizeCancellation: status→CANCELLED")
    void finalizeCancellation_setsCancelled() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleCancellation();

        sub.finalizeCancellation();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("finalizeCancellation when already CANCELLED → no-op (idempotent)")
    void finalizeCancellation_alreadyCancelled_noOp() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleCancellation();
        sub.finalizeCancellation();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);

        // Second call should not throw
        sub.finalizeCancellation();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("finalizeCancellation without cancelledAt → BillingException")
    void finalizeCancellation_noCancelledAt_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        assertThatThrownBy(sub::finalizeCancellation)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("cancelled_at is null");
    }

    // ─── H-flow: undoScheduledCancellation ───────────────────────────────────

    @Test
    @DisplayName("undoScheduledCancellation: clears cancelledAt")
    void undoScheduledCancellation_clearsCancelledAt() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        sub.scheduleCancellation();
        assertThat(sub.getCancelledAt()).isNotNull();

        sub.undoScheduledCancellation();

        assertThat(sub.getCancelledAt()).isNull();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(sub.isCancellationScheduled()).isFalse();
    }

    @Test
    @DisplayName("undoScheduledCancellation when nothing scheduled → BillingException")
    void undoScheduledCancellation_noCancellation_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        assertThatThrownBy(sub::undoScheduledCancellation)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("No scheduled cancellation");
    }

    // ─── B-flow: expireTrial ──────────────────────────────────────────────────

    @Test
    @DisplayName("expireTrial(ACTIVE trial, no billing key): status→EXPIRED")
    void expireTrial_active_paidIsNull_transitionsExpired() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);
        assertThat(sub.getBillingKeyId()).isNull();

        sub.expireTrial();

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("expireTrial already EXPIRED → no-op (idempotent)")
    void expireTrial_alreadyExpired_noOp() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createTrial(1L, "demo", proPlan);
        sub.expireTrial();
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);

        sub.expireTrial();  // idempotent — no throw
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    @DisplayName("expireTrial on paid subscription (billingKeyId != null) → BillingException")
    void expireTrial_onPaid_throws() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(99L);

        assertThatThrownBy(sub::expireTrial)
                .isInstanceOf(BillingException.class)
                .hasMessageContaining("trial subscriptions");
    }

    // ─── F-flow continuation: applyPendingPlanChange ─────────────────────────

    @Test
    @DisplayName("applyPendingPlanChange: swaps to pendingPlan, clears pending state")
    void applyPendingPlanChange_swapsToPending_clears() {
        Plan proPlan = mockPaidPlan("PRO");
        Plan basicPlan = mockCheaperPlan("BASIC");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);
        Instant scheduledAt = Instant.now().plus(Duration.ofDays(30));
        sub.scheduleDowngrade(basicPlan, scheduledAt);
        assertThat(sub.getPendingPlan()).isNotNull();
        assertThat(sub.getPlanId()).isEqualTo("PRO");

        sub.applyPendingPlanChange();

        assertThat(sub.getPlanId()).isEqualTo("BASIC");
        assertThat(sub.getPendingPlan()).isNull();
        assertThat(sub.getScheduledChangeAt()).isNull();
    }

    @Test
    @DisplayName("applyPendingPlanChange when no pending → no-op (idempotent)")
    void applyPendingPlanChange_noPending_noOp() {
        Plan proPlan = mockPaidPlan("PRO");
        Subscription sub = Subscription.createFree(1L, "demo", proPlan);
        sub.attachBillingKey(1L);

        sub.applyPendingPlanChange();  // no-op, should not throw

        assertThat(sub.getPlanId()).isEqualTo("PRO");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Plan mockFreePlan() {
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn("FREE");
        when(plan.getName()).thenReturn("Free Plan");
        when(plan.getMonthlyPrice()).thenReturn(Money.zero());
        when(plan.toLimits()).thenReturn(
                PlanLimits.builder()
                        .maxStudents(50)
                        .maxAcademies(1)
                        .maxAdmins(2)
                        .omrMonthlyQuota(10)
                        .build());
        return plan;
    }

    private Plan mockPaidPlan(String id) {
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn(id);
        when(plan.getName()).thenReturn(id + " Plan");
        when(plan.getMonthlyPrice()).thenReturn(Money.of(99_000L));
        return plan;
    }

    private Plan mockCheaperPlan(String id) {
        Plan plan = mock(Plan.class);
        when(plan.getId()).thenReturn(id);
        when(plan.getName()).thenReturn(id + " Plan");
        when(plan.getMonthlyPrice()).thenReturn(Money.of(39_000L));
        return plan;
    }

    private BillingKey mockBillingKey(Long id) {
        BillingKey key = mock(BillingKey.class);
        when(key.getId()).thenReturn(id);
        return key;
    }
}
