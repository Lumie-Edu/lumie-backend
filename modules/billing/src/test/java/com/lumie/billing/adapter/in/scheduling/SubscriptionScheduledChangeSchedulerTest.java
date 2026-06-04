package com.lumie.billing.adapter.in.scheduling;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.SubscriptionTransitionHelper;
import com.lumie.billing.domain.entity.Subscription;
import com.lumie.billing.domain.exception.BillingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionScheduledChangeScheduler")
class SubscriptionScheduledChangeSchedulerTest {

    @Mock private SubscriptionPersistencePort subscriptionPersistencePort;
    @Mock private SubscriptionTransitionHelper transitionHelper;

    @InjectMocks private SubscriptionScheduledChangeScheduler scheduler;

    // ─── processScheduledTransitions ─────────────────────────────────────────

    @Test
    @DisplayName("processScheduledTransitions: queries all three candidate lists")
    void processScheduledTransitions_callsAllThreeHelpers() {
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());

        scheduler.processScheduledTransitions();

        verify(subscriptionPersistencePort).findTrialsToExpire(any(Instant.class));
        verify(subscriptionPersistencePort).findPendingDowngradesToApply(any(Instant.class));
        verify(subscriptionPersistencePort).findCancellationsToFinalize(any(Instant.class));
    }

    // ─── expireTrials ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("expireTrials: delegates each candidate to transitionHelper.expireOneTrial")
    void expireTrials_iteratesCandidates() {
        Subscription sub1 = mockSubscription(11L);
        Subscription sub2 = mockSubscription(12L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of(sub1, sub2));
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());

        scheduler.processScheduledTransitions();

        verify(transitionHelper).expireOneTrial(11L);
        verify(transitionHelper).expireOneTrial(12L);
    }

    @Test
    @DisplayName("expireTrials: one failure does not abort sibling rows")
    void expireTrials_oneFailure_othersStillProcess() {
        Subscription sub1 = mockSubscription(21L);
        Subscription sub2 = mockSubscription(22L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of(sub1, sub2));
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());
        willThrow(new RuntimeException("DB error")).given(transitionHelper).expireOneTrial(21L);

        scheduler.processScheduledTransitions();

        verify(transitionHelper).expireOneTrial(21L);
        verify(transitionHelper).expireOneTrial(22L);
    }

    @Test
    @DisplayName("expireTrials: empty list → no helper calls")
    void expireTrials_emptyCandidates_noHelperCalls() {
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());

        scheduler.processScheduledTransitions();

        verify(transitionHelper, never()).expireOneTrial(any());
    }

    // ─── applyPendingDowngrades ───────────────────────────────────────────────

    @Test
    @DisplayName("applyPendingDowngrades: delegates each candidate to transitionHelper.applyOnePendingChange")
    void applyPendingDowngrades_iteratesCandidates() {
        Subscription sub1 = mockSubscription(31L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of(sub1));
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());

        scheduler.processScheduledTransitions();

        verify(transitionHelper).applyOnePendingChange(31L);
    }

    @Test
    @DisplayName("applyPendingDowngrades: one failure does not abort sibling rows")
    void applyPendingDowngrades_oneFailure_othersStillProcess() {
        Subscription sub1 = mockSubscription(41L);
        Subscription sub2 = mockSubscription(42L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of(sub1, sub2));
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of());
        willThrow(new RuntimeException("timeout")).given(transitionHelper).applyOnePendingChange(41L);

        scheduler.processScheduledTransitions();

        verify(transitionHelper).applyOnePendingChange(41L);
        verify(transitionHelper).applyOnePendingChange(42L);
    }

    // ─── finalizeCancellations ────────────────────────────────────────────────

    @Test
    @DisplayName("finalizeCancellations: delegates each candidate to transitionHelper.finalizeOneCancellation")
    void finalizeCancellations_iteratesCandidates() {
        Subscription sub1 = mockSubscription(51L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of(sub1));

        scheduler.processScheduledTransitions();

        verify(transitionHelper).finalizeOneCancellation(51L);
    }

    @Test
    @DisplayName("finalizeCancellations: one failure does not abort sibling rows")
    void finalizeCancellations_oneFailure_othersStillProcess() {
        Subscription sub1 = mockSubscription(61L);
        Subscription sub2 = mockSubscription(62L);
        given(subscriptionPersistencePort.findTrialsToExpire(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findPendingDowngradesToApply(any())).willReturn(List.of());
        given(subscriptionPersistencePort.findCancellationsToFinalize(any())).willReturn(List.of(sub1, sub2));
        willThrow(new BillingException(
                com.lumie.billing.domain.exception.BillingErrorCode.SUBSCRIPTION_NOT_FOUND, "not found"))
                .given(transitionHelper).finalizeOneCancellation(61L);

        scheduler.processScheduledTransitions();

        verify(transitionHelper).finalizeOneCancellation(61L);
        verify(transitionHelper).finalizeOneCancellation(62L);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Subscription mockSubscription(Long id) {
        Subscription sub = org.mockito.Mockito.mock(Subscription.class);
        given(sub.getId()).willReturn(id);
        return sub;
    }
}
