package com.lumie.billing.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Cleanup scheduler for stale billing operation lock rows.
 *
 * <p>Runs every 5 minutes and deletes rows older than 15 minutes. This is a
 * defense-in-depth backstop against crashes that leave a lock row without a
 * corresponding {@code DELETE} from {@link BillingOperationLockService}.
 *
 * <p>{@link SchedulerLock} ensures only one pod executes the cleanup at a time
 * in a multi-pod deployment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BillingOperationLockCleanupScheduler {

    private final JdbcTemplate jdbcTemplate;

    @Scheduled(fixedDelayString = "PT5M")
    @SchedulerLock(name = "billing-operation-lock-cleanup",
                   lockAtMostFor = "PT2M", lockAtLeastFor = "PT30S")
    public void cleanupStaleOperations() {
        int deleted = jdbcTemplate.update(
                "DELETE FROM public.billing_operation_locks " +
                "WHERE acquired_at < now() - INTERVAL '15 minutes'");
        if (deleted > 0) {
            log.warn("Cleaned up {} stale billing operation lock(s)", deleted);
        }
    }
}
