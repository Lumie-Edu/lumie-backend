package com.lumie.exam.adapter.in.event;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link StudentRegisteredEvent} after the student-registration
 * commit and backfills any unregistered exam results that match the student's
 * phone number.
 *
 * <p>Replaces the direct {@code student.StudentCommandService → exam.ExamService.linkUnregisteredResultsByPhone}
 * call that created the student↔exam circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 2).
 *
 * <p>Runs in {@code AFTER_COMMIT} phase on the same thread so
 * {@link TenantContextHolder} is still populated; if this is ever switched to
 * {@code @Async} the {@code TaskDecorator} fix from ARCHITECTURE_AUDIT 3.4 must
 * land first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentRegisteredListener {

    private final ExamService examService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentRegistered(StudentRegisteredEvent event) {
        if (event.phone() == null || event.phone().isBlank()) {
            return;
        }
        Runnable backfill = () -> {
            try {
                int linked = examService.linkUnregisteredResultsByPhone(event.phone(), event.studentId());
                if (linked > 0) {
                    log.info("Backfilled {} exam results for student {} (phone={})",
                            linked, event.studentId(), event.phone());
                }
            } catch (Exception e) {
                // Non-fatal: registration must not be undone if backfill fails.
                log.warn("Failed to backfill exam results for student {} (phone={}): {}",
                        event.studentId(), event.phone(), e.getMessage());
            }
        };
        // Re-establish tenant context if the listener is invoked outside the original request thread.
        if (TenantContextHolder.getTenant() == null && event.tenantSlug() != null) {
            TenantContextHolder.withinContext(event.tenantSlug(), null, () -> {
                backfill.run();
                return null;
            });
        } else {
            backfill.run();
        }
    }
}
