package com.lumie.exam.adapter.in.event;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.service.OmrSubmissionService;
import com.lumie.student.api.StudentRegisteredEvent;
import com.lumie.tenant.api.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link StudentRegisteredEvent} after the student-registration
 * commit and backfills a single unmatched OMR submission when its phone number
 * resolves to the newly registered student.
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

    private final OmrSubmissionService omrSubmissionService;
    private final TenantService tenantService;

    @ApplicationModuleListener
    public void onStudentRegistered(StudentRegisteredEvent event) {
        if (event.phone() == null || event.phone().isBlank()) {
            return;
        }
        Runnable backfill = () -> {
            int linkedSubmissions = omrSubmissionService.linkSingleUnmatchedByPhone(event.phone(), event.studentId());
            if (linkedSubmissions > 0) {
                log.info("Backfilled {} OMR submissions for student {} (phone={})",
                        linkedSubmissions, event.studentId(), event.phone());
            }
        };
        if (event.tenantSlug() == null || event.tenantSlug().isBlank()) {
            throw new IllegalStateException("StudentRegisteredEvent tenantSlug is required for exam backfill");
        }
        Long tenantId = resolveTenantId(event);
        TenantContextHolder.withinContext(event.tenantSlug(), tenantId, () -> {
            backfill.run();
            return null;
        });
    }

    private Long resolveTenantId(StudentRegisteredEvent event) {
        if (event.tenantId() != null) {
            return event.tenantId();
        }
        return tenantService.getTenantBySlug(event.tenantSlug())
                .map(TenantService.TenantData::id)
                .orElseThrow(() -> new IllegalStateException(
                        "StudentRegisteredEvent tenantId is missing and tenant slug cannot be resolved: "
                                + event.tenantSlug()));
    }
}
