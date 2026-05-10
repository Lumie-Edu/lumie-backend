package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.statistics.ClassComparisonResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.cls.api.ClassService;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.entity.QuestionResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.service.GradeCalculator;
import com.lumie.exam.domain.service.StatisticsCalculator;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassComparisonQueryService 단위 테스트")
class ClassComparisonQueryServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @Mock private GradeCalculator gradeCalculator;
    @Mock private ClassService classService;

    @org.junit.jupiter.api.BeforeEach
    void setupTenant() {
        TenantContextHolder.setTenant("test-tenant");
    }

    @org.junit.jupiter.api.AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }
    @org.mockito.Spy private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();

    @InjectMocks private ClassComparisonQueryService service;

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
            String selected = corrects.get(i) ? correctAnswer : "0";
            r.addQuestionResult(QuestionResult.create(i + 1, selected, correctAnswer, 20));
        }
        return r;
    }

    @Test
    @DisplayName("반별 비교: 배정된 반과 미배정 그룹이 모두 카드로 반환된다")
    void getClassComparison_assignsAndUnassigned() {
        Exam exam = buildGradedExam();
        ExamResult r1 = resultWith(exam, 100L, 100, List.of(true, true, true, true, true));
        ExamResult r2 = resultWith(exam, 200L, 60,  List.of(true, true, true, false, false));
        ExamResult r3 = resultWith(exam, null, 40,  List.of(true, true, false, false, false));
        ExamResult r4 = resultWith(exam, 300L, 80,  List.of(true, true, true, true, false));

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of(r1, r2, r3, r4));
        given(classService.getEnrolledClassesForStudents(any(), any(Collection.class)))
                .willReturn(List.of(
                        new ClassService.EnrolledClassData(10L, "고2 A반", 100L),
                        new ClassService.EnrolledClassData(10L, "고2 A반", 200L),
                        new ClassService.EnrolledClassData(20L, "고2 B반", 200L)
                ));

        ClassComparisonResponse response = service.getClassComparison(EXAM_ID);

        assertThat(response.totalParticipants()).isEqualTo(4);
        assertThat(response.classes()).hasSize(3);
        assertThat(response.classes()).extracting(ClassComparisonResponse.ClassMetric::className)
                .contains("고2 A반", "고2 B반", "미배정");

        ClassComparisonResponse.ClassMetric unassigned = response.classes().stream()
                .filter(c -> c.classId() == null).findFirst().orElseThrow();
        assertThat(unassigned.participantCount()).isEqualTo(2);
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
}
