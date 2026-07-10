package com.lumie.exam.adapter.in.internal;

import com.lumie.exam.api.ExamService;
import com.lumie.exam.application.dto.request.CreateExamRequest;
import com.lumie.exam.application.dto.response.ExamResponse;
import com.lumie.exam.application.service.ExamCommandService;
import com.lumie.exam.application.service.ResultCommandService;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
    @DisplayName("과제용 시험 생성은 exam command service로 매핑 위임한다")
    void createExamForAssignment_delegatesToCommandService() {
        given(examCommandService.createExam(any(CreateExamRequest.class))).willReturn(new ExamResponse(
                99L,
                "전용 시험",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                2,
                100,
                60,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z")
        ));

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
        verify(examCommandService).createExam(new CreateExamRequest(
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
}
