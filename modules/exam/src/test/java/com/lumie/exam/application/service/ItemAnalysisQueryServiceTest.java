package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.ItemAnalysisResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.test.ExamFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemAnalysisQueryService 단위 테스트")
class ItemAnalysisQueryServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @org.mockito.Spy private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();

    @InjectMocks private ItemAnalysisQueryService service;

    private static final Long EXAM_ID = 1L;


    @Test
    @DisplayName("문항 분석: 각 문항의 p값과 변별도, quadrant가 올바르게 계산된다")
    void getItemAnalysis_basicQuadrants() {
        Exam exam = ExamFixtures.aGradedExam();

        List<ExamResult> results = new ArrayList<>();
        int[] scores = {100, 90, 80, 70, 60, 50, 40, 30, 20, 10};
        for (int i = 0; i < scores.length; i++) {
            ExamResult r = ExamResult.create(exam, (long) (i + 1), scores[i]);
            r.addQuestionResult(QuestionResult.create(1, "1", "1", 20));
            r.addQuestionResult(QuestionResult.create(2, "2", "2", 20));
            r.addQuestionResult(QuestionResult.create(3, i < 5 ? "3" : "0", "3", 20));
            r.addQuestionResult(QuestionResult.create(4, i < 3 ? "4" : "0", "4", 20));
            r.addQuestionResult(QuestionResult.create(5, "0", "5", 20));
            results.add(r);
        }

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);

        ItemAnalysisResponse response = service.getItemAnalysis(EXAM_ID);

        assertThat(response.participantCount()).isEqualTo(10);
        assertThat(response.sampleSize()).isEqualTo(3);
        assertThat(response.reliabilityWarning()).isFalse();

        var byQ = response.items().stream()
                .collect(java.util.stream.Collectors.toMap(ItemAnalysisResponse.ItemPoint::questionNumber, p -> p));

        assertThat(byQ.get(1).pValue()).isEqualTo(1.0);
        assertThat(byQ.get(1).quadrant()).isEqualTo(ItemAnalysisResponse.Quadrant.TOO_EASY);

        assertThat(byQ.get(3).pValue()).isEqualTo(0.5);
        assertThat(byQ.get(3).discrimination()).isEqualTo(1.0);
        assertThat(byQ.get(3).quadrant()).isEqualTo(ItemAnalysisResponse.Quadrant.GOOD);

        assertThat(byQ.get(5).pValue()).isEqualTo(0.0);
        assertThat(byQ.get(5).quadrant()).isEqualTo(ItemAnalysisResponse.Quadrant.TOO_HARD);
    }

    @Test
    @DisplayName("문항 분석: 표본이 10명 미만이면 reliabilityWarning=true")
    void getItemAnalysis_smallSampleWarning() {
        Exam exam = ExamFixtures.aGradedExam();
        List<ExamResult> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            results.add(ExamFixtures.anExamResult(exam, (long) (i + 1), 60, List.of(true, true, true, false, false)));
        }
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);

        ItemAnalysisResponse response = service.getItemAnalysis(EXAM_ID);

        assertThat(response.participantCount()).isEqualTo(8);
        assertThat(response.reliabilityWarning()).isTrue();
    }

    @Test
    @DisplayName("문항 분석: 응시자 0명이면 빈 결과 + warning=true")
    void getItemAnalysis_empty() {
        Exam exam = ExamFixtures.aGradedExam();
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of());

        ItemAnalysisResponse response = service.getItemAnalysis(EXAM_ID);

        assertThat(response.participantCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.reliabilityWarning()).isTrue();
    }
}
