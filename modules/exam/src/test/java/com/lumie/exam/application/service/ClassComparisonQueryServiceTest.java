package com.lumie.exam.application.service;

import com.lumie.common.test.TenantContextExtension;
import com.lumie.exam.application.dto.response.statistics.ClassComparisonResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.classroom.api.ClassService;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.test.ExamFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith({MockitoExtension.class, TenantContextExtension.class})
@DisplayName("ClassComparisonQueryService 단위 테스트")
class ClassComparisonQueryServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @Mock private GradeCalculator gradeCalculator;
    @Mock private ClassService classService;

    @org.mockito.Spy private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();

    @InjectMocks private ClassComparisonQueryService service;

    private static final Long EXAM_ID = 1L;


    @Test
    @DisplayName("반별 비교: 배정된 반과 미배정 그룹이 모두 카드로 반환된다")
    void getClassComparison_assignsAndUnassigned() {
        Exam exam = ExamFixtures.aGradedExam();
        ExamResult r1 = ExamFixtures.anExamResult(exam, 100L, 100, List.of(true, true, true, true, true));
        ExamResult r2 = ExamFixtures.anExamResult(exam, 200L, 60,  List.of(true, true, true, false, false));
        ExamResult r3 = ExamFixtures.anExamResult(exam, null, 40,  List.of(true, true, false, false, false));
        ExamResult r4 = ExamFixtures.anExamResult(exam, 300L, 80,  List.of(true, true, true, true, false));

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
        Exam exam = ExamFixtures.aGradedExam();
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of());

        ClassComparisonResponse response = service.getClassComparison(EXAM_ID);

        assertThat(response.totalParticipants()).isZero();
        assertThat(response.classes()).isEmpty();
    }
}
