package com.lumie.assignment.adapter.in.event;

import com.lumie.assignment.application.port.out.AssignmentResultPersistencePort;
import com.lumie.assignment.domain.entity.AssignmentSubmission;
import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.api.ExamResultChangedEvent;
import com.lumie.exam.api.ExamResultDeletingEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
class AssignmentExamResultListenerTest {
    @Mock AssignmentResultPersistencePort persistence;
    @InjectMocks AssignmentExamResultListener listener;

    @Test
    void answerCorrectionPreservesSubmissionTimeAndSynchronizesScore() {
        var submission = AssignmentSubmission.create(1L, 5L);
        submission.submitExam(Map.of("1", "1"), 7L, 0, false, "9");
        var submittedAt = submission.getSubmittedAt();
        given(persistence.findByResult(7L)).willReturn(List.of(submission));
        listener.onChanged(new ExamResultChangedEvent(TenantContextHolder.getTenantId(), 3L, 7L,
                Map.of("1", "2"), 100, true, 1));
        assertThat(submission.getScore()).isEqualTo(100);
        assertThat(submission.getAnswers()).containsEntry("1", "2");
        assertThat(submission.getGradeValue()).isEqualTo("1");
        assertThat(submission.getSubmittedAt()).isEqualTo(submittedAt);
    }

    @Test
    void deletionRemovesSubmissionBeforeResultForeignKeyIsDeleted() {
        var submissions = List.of(AssignmentSubmission.create(1L, 5L));
        given(persistence.findByResult(7L)).willReturn(submissions);
        listener.onDeleting(new ExamResultDeletingEvent(TenantContextHolder.getTenantId(), 3L, 7L));
        verify(persistence).deleteAndFlush(submissions);
    }

    @Test
    void mismatchCannotMutateAnotherTenant() {
        assertThatThrownBy(() -> listener.onDeleting(new ExamResultDeletingEvent(-1L, 3L, 7L)))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(persistence);
    }
}
