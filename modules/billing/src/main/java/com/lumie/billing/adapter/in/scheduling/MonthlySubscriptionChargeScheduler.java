package com.lumie.billing.adapter.in.scheduling;

import com.lumie.billing.application.port.out.SubscriptionPersistencePort;
import com.lumie.billing.application.service.SubscriptionChargeExecutor;
import com.lumie.billing.application.service.SubscriptionChargeExecutor.ChargeOutcome;
import com.lumie.billing.domain.entity.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Monthly subscription charge job.
 *
 * <p>Runs every 1st of month at 09:00 KST (00:00 UTC). Finds ACTIVE subscriptions with
 * {@code nextChargeAt} overdue and delegates each to {@link SubscriptionChargeExecutor}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonthlySubscriptionChargeScheduler {

    private final SubscriptionPersistencePort subscriptionPersistencePort;
    private final SubscriptionChargeExecutor chargeExecutor;

    @Scheduled(cron = "0 0 0 1 * *", zone = "UTC")
    @SchedulerLock(name = "billing-monthly-charge", lockAtMostFor = "PT30M", lockAtLeastFor = "PT5M")
    public void chargeMonthlySubscriptions() {
        log.info("MonthlySubscriptionChargeScheduler: starting monthly charge run");

        List<Subscription> subscriptions = subscriptionPersistencePort
                .findActiveWithNextChargeAtOrBefore(Instant.now());

        log.info("MonthlySubscriptionChargeScheduler: found {} subscriptions to charge", subscriptions.size());

        int succeeded = 0;
        int failed = 0;

        for (Subscription subscription : subscriptions) {
            try {
                ChargeOutcome outcome = chargeExecutor.charge(subscription);
                if (outcome.isSuccess()) {
                    succeeded++;
                } else {
                    failed++;
                }
            } catch (Exception e) {
                log.error("MonthlySubscriptionChargeScheduler: unexpected error for subscriptionId={}: {}",
                        subscription.getId(), e.getMessage(), e);
                failed++;
            }
        }

        log.info("MonthlySubscriptionChargeScheduler: completed. succeeded={}, failed={}", succeeded, failed);
    }
}
