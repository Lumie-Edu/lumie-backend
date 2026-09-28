package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.classroom.api.ClassService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssignmentRecipientServiceTest {
    @Mock private AssignmentRecipientRepository recipientRepository;
    @Mock private AssignmentRepository assignmentRepository;
    @Mock private ClassService classService;
    @InjectMocks private AssignmentRecipientService service;

    @Test
    void creationSnapshotsDirectAndCurrentClassStudentsWithoutDuplicates() {
        given(classService.getEnrolledStudentIds("tenant", 10L)).willReturn(List.of(2L, 3L));
        given(recipientRepository.findStudentIdsByAssignmentId(7L)).willReturn(List.of());

        service.snapshotRecipients("tenant", 7L, List.of(10L), List.of(1L, 2L));

        ArgumentCaptor<List<AssignmentRecipient>> captor = recipientListCaptor();
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(AssignmentRecipient::getStudentId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void newEnrollmentAddsOnlyToOpenClassAssignments() throws Exception {
        Assignment assignment = Assignment.create(10L, Instant.now().plusSeconds(3_600), 100, null,
                AssignmentStatus.ACTIVE, SubmissionMode.EXAM_MANUAL, EvaluationMode.EXAM_AUTO, 5L,
                true, true, false, true, false);
        org.springframework.test.util.ReflectionTestUtils.setField(assignment, "id", 7L);
        given(assignmentRepository.findActiveByTargetClassId(eq(10L), any())).willReturn(List.of(assignment));
        given(recipientRepository.findStudentIdsByAssignmentId(7L)).willReturn(List.of(1L));

        service.addNewClassEnrollees("tenant", 10L, List.of(1L, 2L));

        ArgumentCaptor<List<AssignmentRecipient>> captor = recipientListCaptor();
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(AssignmentRecipient::getStudentId).containsExactly(2L);
    }

    @Test
    void emptyEnrollmentDoesNotQueryAssignments() {
        service.addNewClassEnrollees("tenant", 10L, List.of());

        verify(assignmentRepository, never()).findActiveByTargetClassId(any(), any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<List<AssignmentRecipient>> recipientListCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }
}
