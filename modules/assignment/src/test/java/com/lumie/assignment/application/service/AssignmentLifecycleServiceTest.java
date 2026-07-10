package com.lumie.assignment.application.service;

import com.lumie.assignment.domain.repository.AssignmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssignmentLifecycleService 테스트")
class AssignmentLifecycleServiceTest {

    @Mock private AssignmentRepository assignmentRepository;
    @InjectMocks private AssignmentLifecycleService lifecycleService;

    @Test
    @DisplayName("마감 시간이 지난 ACTIVE 과제를 CLOSED로 일괄 변경한다")
    void closeOverdueAssignments_closesActiveAssignmentsPastDueDate() {
        Instant now = Instant.parse("2026-07-11T00:00:00Z");
        given(assignmentRepository.closeOverdueActiveAssignments(now)).willReturn(2);

        int closed = lifecycleService.closeOverdueAssignments(now);

        assertThat(closed).isEqualTo(2);
        verify(assignmentRepository).closeOverdueActiveAssignments(now);
    }

    @Test
    @DisplayName("특정 과제 한 건만 마감 종료 처리한다")
    void closeOverdueAssignment_closesSingleAssignment() {
        given(assignmentRepository.closeOverdueActiveAssignment(1L, Instant.parse("2026-07-11T00:00:00Z")))
                .willReturn(1);

        int closed = lifecycleService.closeOverdueAssignment(1L, Instant.parse("2026-07-11T00:00:00Z"));

        assertThat(closed).isEqualTo(1);
        verify(assignmentRepository).closeOverdueActiveAssignment(1L, Instant.parse("2026-07-11T00:00:00Z"));
    }
}
