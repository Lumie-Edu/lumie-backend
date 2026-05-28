package com.lumie.billing.adapter.in.scheduling;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.SubscriptionTransitionHelper;
import com.lumie.billing.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Daily processor for scheduled subscription state transitions (Phase E D6).
 *
 * <p>Three transitions are handled in one tick:
 * <ul>
 *   <li>{@code expireTrials} — billingKeyId IS NULL + expiresAt &le; now → EXPIRED (B-flow)</li>
 *   <li>{@code applyPendingDowngrades} — scheduledChangeAt &le; now → plan swapped (F-flow)</li>
 *   <li>{@code finalizeCancellations} — cancelledAt IS NOT NULL + nextChargeAt &le; now → CANCELLED (G-flow)</li>
 * </ul>
 *
 * <p>Per-row transactions are owned by {@link SubscriptionTransitionHelper} (REQUIRES_NEW),
 * which is a separate Spring bean to honour the AOP proxy contract. One row failure does
 * not abort the entire cycle — each failure is logged and the loop continues.
 *
 * <p>ShedLock name {@code billing-scheduled-transitions} ensures single execution across pods.
 * Cron runs daily at 09:05 KST (00:05 UTC), staggered after midnight to avoid write spikes
 * coinciding with {@link MonthlySubscriptionChargeScheduler}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduledChangeScheduler {

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionTransitionHelper transitionHelper;

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    @SchedulerLock(name = "billing-scheduled-transitions",
                   lockAtMostFor = "PT15M", lockAtLeastFor = "PT5M")
    public void processScheduledTransitions() {
        Instant now = Instant.now();
        log.info("SubscriptionScheduledChangeScheduler tick at {}", now);
        expireTrials(now);
        applyPendingDowngrades(now);
        finalizeCancellations(now);
    }

    private void expireTrials(Instant now) {
        List<Subscription> candidates = subscriptionPersistencePort.findTrialsToExpire(now);
        log.info("SubscriptionScheduledChangeScheduler: expiring {} trial(s)", candidates.size());
        for (Subscription sub : candidates) {
            try {
                transitionHelper.expireOneTrial(sub.getId());
            } catch (Exception e) {
                log.error("SubscriptionScheduledChangeScheduler: failed to expire trial subscriptionId={}",
                        sub.getId(), e);
            }
        }
    }

    private void applyPendingDowngrades(Instant now) {
        List<Subscription> candidates = subscriptionPersistencePort.findPendingDowngradesToApply(now);
        log.info("SubscriptionScheduledChangeScheduler: applying {} pending downgrade(s)", candidates.size());
        for (Subscription sub : candidates) {
            try {
                transitionHelper.applyOnePendingChange(sub.getId());
            } catch (Exception e) {
                log.error("SubscriptionScheduledChangeScheduler: failed to apply pending change subscriptionId={}",
                        sub.getId(), e);
            }
        }
    }

    private void finalizeCancellations(Instant now) {
        List<Subscription> candidates = subscriptionPersistencePort.findCancellationsToFinalize(now);
        log.info("SubscriptionScheduledChangeScheduler: finalizing {} cancellation(s)", candidates.size());
        for (Subscription sub : candidates) {
            try {
                transitionHelper.finalizeOneCancellation(sub.getId());
            } catch (Exception e) {
                log.error("SubscriptionScheduledChangeScheduler: failed to finalize cancellation subscriptionId={}",
                        sub.getId(), e);
            }
        }
    }
}
