package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.entity.Assignment;
import com.lumie.assignment.domain.entity.AssignmentRecipient;
import com.lumie.assignment.application.port.out.AssignmentAudienceReadPort;
import com.lumie.assignment.application.port.out.AssignmentRecipientSourcePersistencePort;
import com.lumie.assignment.domain.repository.AssignmentRecipientRepository;
import com.lumie.assignment.domain.repository.AssignmentRepository;
import com.lumie.assignment.domain.vo.AssignmentStatus;
import com.lumie.assignment.domain.vo.EvaluationMode;
import com.lumie.assignment.domain.vo.SubmissionMode;
import com.lumie.classroom.api.ClassService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
    @Mock private AssignmentAudienceReadPort audienceReadPort;
    @Mock private AssignmentRecipientSourcePersistencePort sourcePort;
    @InjectMocks private AssignmentRecipientService service;

    @Test
    void creationSnapshotsDirectAndCurrentClassStudentsWithoutDuplicates() {
        given(classService.getEnrolledStudentIds("tenant", 10L)).willReturn(List.of(2L, 3L));
        given(recipientRepository.findByAssignmentId(7L)).willReturn(List.of());
        given(audienceReadPort.findClassTargetId(7L, 10L)).willReturn(Optional.of(90L));
        given(audienceReadPort.findStudentTargetId(7L, 1L)).willReturn(Optional.of(91L));
        given(audienceReadPort.findStudentTargetId(7L, 2L)).willReturn(Optional.of(92L));
        given(recipientRepository.saveAll(any())).willAnswer(inv -> {
            List<AssignmentRecipient> rows = inv.getArgument(0);
            for (int i = 0; i < rows.size(); i++) {
                org.springframework.test.util.ReflectionTestUtils.setField(rows.get(i), "id", (long) i + 1);
            }
            return rows;
        });

        service.snapshotRecipients("tenant", 7L, List.of(10L), List.of(1L, 2L));

        ArgumentCaptor<List<AssignmentRecipient>> captor = recipientListCaptor();
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(AssignmentRecipient::getStudentId)
                .containsExactlyInAnyOrder(1L, 2L, 3L);
        verify(sourcePort).recordStudentSource(1L, 7L, 1L, 91L, true);
        verify(sourcePort).recordClassSource(2L, 7L, 2L, 90L, true);
    }

    @Test
    void newEnrollmentAddsOnlyToOpenClassAssignments() throws Exception {
        Assignment assignment = Assignment.create(10L, Instant.now().plusSeconds(3_600), 100, null,
                AssignmentStatus.ACTIVE, SubmissionMode.EXAM_MANUAL, EvaluationMode.EXAM_AUTO, 5L,
                true, true, false, true, false);
        org.springframework.test.util.ReflectionTestUtils.setField(assignment, "id", 7L);
        given(assignmentRepository.findActiveByTargetClassId(eq(10L), any())).willReturn(List.of(assignment));
        AssignmentRecipient existing = AssignmentRecipient.create(7L, 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", 100L);
        given(recipientRepository.findByAssignmentId(7L)).willReturn(List.of(existing));
        given(audienceReadPort.findClassTargetId(7L, 10L)).willReturn(Optional.of(90L));
        given(recipientRepository.saveAll(any())).willAnswer(inv -> {
            List<AssignmentRecipient> rows = inv.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(rows.get(0), "id", 101L);
            return rows;
        });

        service.addNewClassEnrollees("tenant", 10L, List.of(1L, 2L));

        ArgumentCaptor<List<AssignmentRecipient>> captor = recipientListCaptor();
        verify(recipientRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(AssignmentRecipient::getStudentId).containsExactly(2L);
        verify(sourcePort).recordClassSource(100L, 7L, 1L, 90L, false);
        verify(sourcePort).recordClassSource(101L, 7L, 2L, 90L, true);
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
