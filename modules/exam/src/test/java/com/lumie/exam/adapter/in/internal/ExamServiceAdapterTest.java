package com.lumie.exam.adapter.in.internal;

import com.lumie.exam.api.ExamService;
import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ResultCommandService;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.test.ExamFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamServiceAdapter 단위 테스트")
class ExamServiceAdapterTest {

    @Mock
    private ExamResultRepository examResultRepository;

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ResultCommandService resultCommandService;

    @Mock
    private ExamCommandService examCommandService;

    @InjectMocks
    private ExamServiceAdapter examServiceAdapter;

    @Test
    @DisplayName("과제용 시험 생성은 시험 관리에 노출되지 않는 private 생성으로 위임한다")
    void createExamForAssignment_delegatesToCommandService() {
        given(examCommandService.createAssignmentExam(any(CreateExamRequest.class))).willReturn(99L);

        Long examId = examServiceAdapter.createExamForAssignment("tenant-a", new ExamService.CreateExamData(
                "전용 시험",
                "GRADED",
                "ABSOLUTE",
                "NINE_GRADE",
                2,
                Map.of("1", "2", "2", "3"),
                Map.of("1", 50, "2", 50),
                Map.of("1", "객관식", "2", "객관식"),
                60
        ));

        assertThat(examId).isEqualTo(99L);
        verify(examCommandService).createAssignmentExam(new CreateExamRequest(
                "전용 시험",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                2,
                Map.of("1", "2", "2", "3"),
                Map.of("1", 50, "2", 50),
                Map.of("1", "객관식", "2", "객관식"),
                60
        ));
    }

    @Test
    @DisplayName("과제용 시험 조회는 정답 없이 문항 메타데이터만 반환한다")
    void getExamForAssignment_returnsSafeMetadata() {
        given(examRepository.findById(1L)).willReturn(Optional.of(ExamFixtures.aGradedExam()));

        Optional<ExamService.ExamAssignmentData> result = examServiceAdapter.getExamForAssignment("tenant-a", 1L);

        assertThat(result).isPresent();
        ExamService.ExamAssignmentData exam = result.orElseThrow();
        assertThat(exam.name()).isEqualTo("테스트 시험");
        assertThat(exam.totalPossibleScore()).isEqualTo(100);
        assertThat(exam.questions()).hasSize(5);
        assertThat(exam.questions().get(0).questionNumber()).isEqualTo(1);
        assertThat(exam.questions().get(0).score()).isEqualTo(20);
        assertThat(exam.questions().get(0).questionType()).isEqualTo("단답");
    }

    @Test
    @DisplayName("수동 채점은 command service 결과를 그대로 전달한다")
    void gradeManualAnswers_delegatesToCommandService() {
        ExamService.ManualGradeResult expected = new ExamService.ManualGradeResult(
                10L,
                1L,
                100L,
                80,
                100,
                2,
                null,
                List.of(new ExamService.QuestionGradeData(11L, 1, "1", true, 20, 20, "1", "단답"))
        );
        given(resultCommandService.gradeManualAnswers(1L, 100L, Map.of("1", "1"))).willReturn(expected);

        ExamService.ManualGradeResult result = examServiceAdapter.gradeManualAnswers(
                "tenant-a",
                1L,
                100L,
                Map.of("1", "1")
        );

        verify(resultCommandService).gradeManualAnswers(1L, 100L, Map.of("1", "1"));
        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("학생 등록 backfill은 후보가 정확히 하나일 때만 연결한다")
    void linkUnregisteredResultsByPhone_singleCandidate_linksResult() {
        Exam exam = ExamFixtures.aGradedExam();
        org.springframework.test.util.ReflectionTestUtils.setField(exam, "id", 1L);
        ExamResult candidate = ExamResult.createForUnregistered(exam, "01012345678", 80);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate, "id", 10L);

        given(examResultRepository.findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse("01012345678"))
                .willReturn(List.of(candidate));
        given(examResultRepository.findByExamIdAndStudentId(1L, 200L)).willReturn(Optional.empty());

        int linked = examServiceAdapter.linkUnregisteredResultsByPhone("010-1234-5678", 200L);

        assertThat(linked).isEqualTo(1);
        assertThat(candidate.getStudentId()).isEqualTo(200L);
        assertThat(candidate.getPhoneNumber()).isEqualTo("01012345678");
        verify(examResultRepository).save(candidate);
    }

    @Test
    @DisplayName("학생 등록 backfill은 같은 전화번호 후보가 여러 시험에 하나씩 있으면 모두 연결한다")
    void linkUnregisteredResultsByPhone_multipleExams_linksEachSingleCandidate() {
        Exam exam1 = ExamFixtures.aGradedExam();
        Exam exam2 = ExamFixtures.aGradedExam();
        org.springframework.test.util.ReflectionTestUtils.setField(exam1, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(exam2, "id", 2L);
        ExamResult candidate1 = ExamResult.createForUnregistered(exam1, "01012345678", 80);
        ExamResult candidate2 = ExamResult.createForUnregistered(exam2, "01012345678", 90);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate1, "id", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(candidate2, "id", 11L);

        given(examResultRepository.findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse("01012345678"))
                .willReturn(List.of(candidate1, candidate2));
        given(examResultRepository.findByExamIdAndStudentId(1L, 200L)).willReturn(Optional.empty());
        given(examResultRepository.findByExamIdAndStudentId(2L, 200L)).willReturn(Optional.empty());

        int linked = examServiceAdapter.linkUnregisteredResultsByPhone("01012345678", 200L);

        assertThat(linked).isEqualTo(2);
        assertThat(candidate1.getStudentId()).isEqualTo(200L);
        assertThat(candidate2.getStudentId()).isEqualTo(200L);
        verify(examResultRepository).save(candidate1);
        verify(examResultRepository).save(candidate2);
    }

    @Test
    @DisplayName("학생 등록 backfill은 같은 시험에 같은 전화번호 후보가 여러 개이면 해당 시험만 건너뛴다")
    void linkUnregisteredResultsByPhone_sameExamAmbiguity_skipsOnlyAmbiguousExam() {
        Exam ambiguousExam = ExamFixtures.aGradedExam();
        Exam clearExam = ExamFixtures.aGradedExam();
        org.springframework.test.util.ReflectionTestUtils.setField(ambiguousExam, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(clearExam, "id", 2L);
        ExamResult ambiguousCandidate1 = ExamResult.createForUnregistered(ambiguousExam, "01012345678", 80);
        ExamResult ambiguousCandidate2 = ExamResult.createForUnregistered(ambiguousExam, "01012345678", 90);
        ExamResult clearCandidate = ExamResult.createForUnregistered(clearExam, "01012345678", 95);
        org.springframework.test.util.ReflectionTestUtils.setField(ambiguousCandidate1, "id", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(ambiguousCandidate2, "id", 11L);
        org.springframework.test.util.ReflectionTestUtils.setField(clearCandidate, "id", 12L);

        given(examResultRepository.findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse("01012345678"))
                .willReturn(List.of(ambiguousCandidate1, ambiguousCandidate2, clearCandidate));
        given(examResultRepository.findByExamIdAndStudentId(2L, 200L)).willReturn(Optional.empty());

        int linked = examServiceAdapter.linkUnregisteredResultsByPhone("01012345678", 200L);

        assertThat(linked).isEqualTo(1);
        assertThat(ambiguousCandidate1.getStudentId()).isNull();
        assertThat(ambiguousCandidate2.getStudentId()).isNull();
        assertThat(clearCandidate.getStudentId()).isEqualTo(200L);
        verify(examResultRepository, never()).findByExamIdAndStudentId(1L, 200L);
        verify(examResultRepository).save(clearCandidate);
    }

    @Test
    @DisplayName("학생 등록 backfill은 이미 등록 결과가 있는 시험만 건너뛴다")
    void linkUnregisteredResultsByPhone_duplicateRegisteredResult_skipsOnlyConflictingExam() {
        Exam conflictingExam = ExamFixtures.aGradedExam();
        Exam clearExam = ExamFixtures.aGradedExam();
        org.springframework.test.util.ReflectionTestUtils.setField(conflictingExam, "id", 1L);
        org.springframework.test.util.ReflectionTestUtils.setField(clearExam, "id", 2L);
        ExamResult conflictingCandidate = ExamResult.createForUnregistered(conflictingExam, "01012345678", 80);
        ExamResult existingRegistered = ExamResult.create(conflictingExam, 200L, 85);
        ExamResult clearCandidate = ExamResult.createForUnregistered(clearExam, "01012345678", 95);
        org.springframework.test.util.ReflectionTestUtils.setField(conflictingCandidate, "id", 10L);
        org.springframework.test.util.ReflectionTestUtils.setField(existingRegistered, "id", 11L);
        org.springframework.test.util.ReflectionTestUtils.setField(clearCandidate, "id", 12L);

        given(examResultRepository.findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse("01012345678"))
                .willReturn(List.of(conflictingCandidate, clearCandidate));
        given(examResultRepository.findByExamIdAndStudentId(1L, 200L)).willReturn(Optional.of(existingRegistered));
        given(examResultRepository.findByExamIdAndStudentId(2L, 200L)).willReturn(Optional.empty());

        int linked = examServiceAdapter.linkUnregisteredResultsByPhone("01012345678", 200L);

        assertThat(linked).isEqualTo(1);
        assertThat(conflictingCandidate.getStudentId()).isNull();
        assertThat(clearCandidate.getStudentId()).isEqualTo(200L);
        verify(examResultRepository, never()).save(conflictingCandidate);
        verify(examResultRepository).save(clearCandidate);
    }
}
