package com.lumie.exam.adapter.in.event;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamService;
import com.lumie.student.api.StudentRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link StudentRegisteredEvent} after the student-registration
 * commit and backfills any unregistered exam results that match the student's
 * phone number.
 *
 * <p>Replaces the direct {@code student.StudentCommandService → exam.ExamService.linkUnregisteredResultsByPhone}
 * call that created the student↔exam circular dependency
 * (see {@code temp/audits-cleanup/CIRCULAR_MODULE_DEPS_AUDIT.md}, Resolution A pair 2).
 *
 * <p>Uses Spring Modulith's {@link ApplicationModuleListener}: each invocation
 * is recorded in the {@code event_publication} outbox before the originating
 * transaction commits, then dispatched in a fresh transaction. Failed dispatches
 * stay incomplete and are retried on application restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StudentRegisteredListener {

    private final ExamService examService;

    @ApplicationModuleListener
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
