package com.lumie.assignment.adapter.in.event;

import com.lumie.assignment.application.port.out.AssignmentResultPersistencePort;
import com.lumie.assignment.domain.exception.AssignmentErrorCode;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.api.ExamResultChangedEvent;
import com.lumie.exam.api.ExamResultDeletingEvent;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AssignmentExamResultListener {
    private final AssignmentResultPersistencePort persistence;

    @EventListener
    public void onChanged(ExamResultChangedEvent event) {
        requireTenant(event.tenantId());
        persistence.findByResult(event.resultId()).forEach(submission ->
                submission.synchronizeExamResult(event.answers(), event.score()));
    }

    @EventListener
    public void onDeleting(ExamResultDeletingEvent event) {
        requireTenant(event.tenantId());
        persistence.deleteAndFlush(persistence.findByResult(event.resultId()));
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null || !Objects.equals(tenantId, TenantContextHolder.getTenantId())) {
            throw new BusinessException(AssignmentErrorCode.INVALID_SUBMISSION, "Result tenant mismatch");
        }
    }
}
