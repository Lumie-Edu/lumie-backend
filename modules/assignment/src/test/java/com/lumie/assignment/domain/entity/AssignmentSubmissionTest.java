package com.lumie.assignment.domain.entity;

import com.lumie.assignment.domain.vo.SubmissionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
    @DisplayName("연결 시험 결과 제출 시 공식 점수와 합불을 투영한다")
    void submitExam_projectsOfficialResult() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submitExam(Map.of("1", "2"), 30L, 8, true, null);

        assertThat(submission.getStatus()).isEqualTo(SubmissionStatus.GRADED);
        assertThat(submission.getExamResultId()).isEqualTo(30L);
        assertThat(submission.getScore()).isEqualTo(8);
        assertThat(submission.getPassed()).isTrue();
        assertThat(submission.getGradedAt()).isNotNull();
    }

    @Test
    @DisplayName("공식 결과 변경 시 과제 투영을 갱신한다")
    void synchronizeExamResult_updatesProjection() {
        AssignmentSubmission submission = AssignmentSubmission.create(1L, 42L);
        submission.submitExam(Map.of("1", "2"), 30L, 8, true, null);

        submission.synchronizeExamResult(Map.of("1", "1"), 4, false, null);

        assertThat(submission.getAnswers()).containsEntry("1", "1");
        assertThat(submission.getScore()).isEqualTo(4);
        assertThat(submission.getPassed()).isFalse();
    }
}
