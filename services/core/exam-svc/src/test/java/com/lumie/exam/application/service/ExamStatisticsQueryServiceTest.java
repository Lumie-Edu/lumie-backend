package com.lumie.exam.application.service;

import com.lumie.exam.application.dto.response.statistics.ClassComparisonResponse;
import com.lumie.exam.application.dto.response.statistics.ItemAnalysisResponse;
import com.lumie.exam.application.port.out.ClassEnrollmentPort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.exam.domain.service.StatisticsCalculator;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamStatisticsQueryService - class-comparison & item-analysis 단위 테스트")
class ExamStatisticsQueryServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @Mock private GradeCalculator gradeCalculator;
    @Mock private ClassEnrollmentPort classEnrollmentPort;

    @InjectMocks private ExamStatisticsQueryService service;

    // 도메인 서비스는 실제 구현을 사용 (스파이성 단위 검증)
    @org.mockito.Spy private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();

    private static final Long EXAM_ID = 1L;

    private Exam buildGradedExam() {
        return Exam.create(
                "수학 모의고사",
                ExamCategory.GRADED,
                GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE,
                5,
                Map.of("1", "1", "2", "2", "3", "3", "4", "4", "5", "5"),
                Map.of("1", 20, "2", 20, "3", 20, "4", 20, "5", 20),
                Map.of("1", "단답", "2", "단답", "3", "단답", "4", "단답", "5", "단답"),
                null
        );
    }

    private ExamResult resultWith(Exam exam, Long studentId, int score, List<Boolean> corrects) {
        ExamResult r = ExamResult.create(exam, studentId, score);
        for (int i = 0; i < corrects.size(); i++) {
            String correctAnswer = String.valueOf(i + 1);
            String selected = corrects.get(i) ? correctAnswer : "0"; // wrong choice
            r.addQuestionResult(QuestionResult.create(i + 1, selected, correctAnswer, 20));
        }
        return r;
    }

    @BeforeEach
    void resetSpy() {
        // statisticsCalculator는 실제 동작
    }

    @Test
    @DisplayName("반별 비교: 배정된 반과 미배정 그룹이 모두 카드로 반환된다")
    void getClassComparison_assignsAndUnassigned() {
        Exam exam = buildGradedExam();
        ExamResult r1 = resultWith(exam, 100L, 100, List.of(true, true, true, true, true));
        ExamResult r2 = resultWith(exam, 200L, 60,  List.of(true, true, true, false, false));
        ExamResult r3 = resultWith(exam, null, 40,  List.of(true, true, false, false, false));   // 미등록(전화)
        ExamResult r4 = resultWith(exam, 300L, 80,  List.of(true, true, true, true, false));    // 어떤 반에도 enrolled 안된 학생

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of(r1, r2, r3, r4));
        given(classEnrollmentPort.findEnrolledClassesForStudents(any(Collection.class)))
                .willReturn(List.of(
                        new ClassEnrollmentPort.ClassParticipant(10L, "고2 A반", 100L),
                        new ClassEnrollmentPort.ClassParticipant(10L, "고2 A반", 200L),
                        new ClassEnrollmentPort.ClassParticipant(20L, "고2 B반", 200L) // 200번은 두 반에 속함
                ));

        ClassComparisonResponse response = service.getClassComparison(EXAM_ID);

        assertThat(response.totalParticipants()).isEqualTo(4);
        assertThat(response.classes()).hasSize(3); // A반, B반, 미배정
        assertThat(response.classes()).extracting(ClassComparisonResponse.ClassMetric::className)
                .contains("고2 A반", "고2 B반", "미배정");

        ClassComparisonResponse.ClassMetric unassigned = response.classes().stream()
                .filter(c -> c.classId() == null).findFirst().orElseThrow();
        assertThat(unassigned.participantCount()).isEqualTo(2); // r3 + r4
    }

    @Test
    @DisplayName("반별 비교: 응시자 0명이면 빈 리스트를 반환한다")
    void getClassComparison_empty() {
        Exam exam = buildGradedExam();
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of());

        ClassComparisonResponse response = service.getClassComparison(EXAM_ID);

        assertThat(response.totalParticipants()).isZero();
        assertThat(response.classes()).isEmpty();
    }

    @Test
    @DisplayName("문항 분석: 각 문항의 p값과 변별도, quadrant가 올바르게 계산된다")
    void getItemAnalysis_basicQuadrants() {
        Exam exam = buildGradedExam();

        List<ExamResult> results = new ArrayList<>();
        // 10명. 점수 내림차순으로 만들고 문항별 정답 패턴 통제.
        // Q1: 모두 정답 → p=1.0, d=0 → TOO_EASY
        // Q3: 상위 27%(상위3명) 모두 맞고, 하위 27%(하위3명) 모두 틀림 → p중간, d=1 → GOOD
        // Q5: 상하 모두 틀림 → p=0, d=0 → TOO_HARD
        // Q2: 모두 정답 → TOO_EASY
        // Q4: 상위만 약간 → 적절히 분포

        // 점수 100,90,80,70,60,50,40,30,20,10
        int[] scores = {100, 90, 80, 70, 60, 50, 40, 30, 20, 10};
        for (int i = 0; i < scores.length; i++) {
            ExamResult r = ExamResult.create(exam, (long) (i + 1), scores[i]);
            // Q1: 모두 정답
            r.addQuestionResult(QuestionResult.create(1, "1", "1", 20));
            // Q2: 모두 정답
            r.addQuestionResult(QuestionResult.create(2, "2", "2", 20));
            // Q3: 상위 5명 정답, 하위 5명 오답 → 상위3 모두/하위3 0
            r.addQuestionResult(QuestionResult.create(3, i < 5 ? "3" : "0", "3", 20));
            // Q4: 상위 3명만 정답
            r.addQuestionResult(QuestionResult.create(4, i < 3 ? "4" : "0", "4", 20));
            // Q5: 모두 오답
            r.addQuestionResult(QuestionResult.create(5, "0", "5", 20));
            results.add(r);
        }

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);

        ItemAnalysisResponse response = service.getItemAnalysis(EXAM_ID);

        assertThat(response.participantCount()).isEqualTo(10);
        assertThat(response.sampleSize()).isEqualTo(3); // ceil(10*0.27) = 3
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
        Exam exam = buildGradedExam();
        List<ExamResult> results = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            results.add(resultWith(exam, (long) (i + 1), 60, List.of(true, true, true, false, false)));
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
        Exam exam = buildGradedExam();
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of());

        ItemAnalysisResponse response = service.getItemAnalysis(EXAM_ID);

        assertThat(response.participantCount()).isZero();
        assertThat(response.items()).isEmpty();
        assertThat(response.reliabilityWarning()).isTrue();
    }
}
