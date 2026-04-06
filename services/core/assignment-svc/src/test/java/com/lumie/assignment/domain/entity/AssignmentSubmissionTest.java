package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssignmentSubmission 도메인 엔티티 테스트")
class AssignmentSubmissionTest {

    @Test
    @DisplayName("create() 호출 시 기본 상태는 PENDING이다")
    void create_defaultStatusIsPending() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);

        assertThat(submission.getAssignmentId()).isEqualTo(1L);
        assertThat(submission.getStudentId()).isEqualTo(42L);
        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.PENDING);
        assertThat(submission.getSubmittedAt()).isNull();
        assertThat(submission.getGradedAt()).isNull();
        assertThat(submission.getContent()).isNull();
    }

    @Test
    @DisplayName("submit() 호출 시 상태가 SUBMITTED로 변경되고 submittedAt이 설정된다")
    void submit_statusChangesToSubmitted() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);

        submission.submit("풀이 내용입니다.", List.of(10L, 11L));

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(submission.getContent()).isEqualTo("풀이 내용입니다.");
        assertThat(submission.getAttachments()).containsExactly(10L, 11L);
        assertThat(submission.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("grade() 호출 시 상태가 GRADED로 변경되고 점수와 피드백이 설정된다")
    void grade_statusChangesToGraded() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submit("답안", null);

        submission.grade(85, "잘했습니다.");

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getScore()).isEqualTo(85);
        assertThat(submission.getFeedback()).isEqualTo("잘했습니다.");
        assertThat(submission.getGradedAt()).isNotNull();
    }

    @Test
    @DisplayName("returnToStudent() 호출 시 상태가 RETURNED로 변경된다")
    void returnToStudent_statusChangesToReturned() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submit("답안", null);

        submission.returnToStudent("다시 제출해주세요.");

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.RETURNED);
        assertThat(submission.getFeedback()).isEqualTo("다시 제출해주세요.");
    }
}
