package com.lumie.exam.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.test.ReflectionTestUtils;
import com.lumie.exam.application.dto.response.statistics.StudentGradeResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
import com.lumie.exam.domain.vo.ExamSource;
import com.lumie.exam.domain.vo.GradeScale;
import com.lumie.exam.domain.vo.GradingType;
import com.lumie.exam.test.ExamFixtures;
import com.lumie.student.api.StudentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamExportService 단위 테스트")
class ExamExportServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @Spy private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();
    @Mock private GradeCalculator gradeCalculator;
    @Mock private StudentService studentService;
    @Mock private StudentGradeQueryService studentGradeQueryService;

    @InjectMocks private ExamExportService service;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenant("test-academy");
        TenantContextHolder.setTenantId(101L);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("prepareTenantWideCsv: 기존 전역 export는 시험 관리 시험 결과를 CSV로 유지한다")
    void prepareTenantWideCsv_writesTenantWideRows() throws Exception {
        Exam exam = ExamFixtures.aGradedExam(2);
        ReflectionTestUtils.setField(exam, "id", 10L);

        ExamResult result = ExamFixtures.anExamResult(exam, 7L, 20, List.of(true, false));
        ReflectionTestUtils.setField(result, "id", 100L);
        ReflectionTestUtils.setField(result, "createdAt", Instant.parse("2026-08-01T00:00:00Z"));

        given(examRepository.findBySource(ExamSource.EXAM_MANAGEMENT)).willReturn(List.of(exam));
        given(resultRepository.findByExamId(10L)).willReturn(List.of(result));
        given(studentService.getStudentsByIds(anyString(), anyList())).willReturn(
            new StudentService.StudentListData(List.of(
                new StudentService.StudentData(7L, null, "홍길동", "010-1111-2222", true, null, null)
            ), 1, 0, 1)
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.prepareTenantWideCsv().writeTo(out);

        String csv = csv(out);
        assertThat(csv).contains("학생명,휴대폰,시험명,응시일,점수,만점,정답수,오답수,등수,백분위,등급");
        assertThat(csv).contains("홍길동,010-1111-2222,테스트 시험,2026-08-01,20,40,1,1,1,0.0,");
    }

    @Test
    @DisplayName("prepareCsvForExam: 상세 화면 정렬 순서와 필터 결과만 CSV로 내보낸다")
    void prepareCsvForExam_usesFilteredSortedStudentGrades() throws Exception {
        Exam exam = ExamFixtures.aGradedExam(2);
        ReflectionTestUtils.setField(exam, "id", 1L);

        ExamResult topResult = ExamFixtures.anExamResult(exam, 2L, 40, List.of(true, true));
        ReflectionTestUtils.setField(topResult, "id", 200L);
        ReflectionTestUtils.setField(topResult, "createdAt", Instant.parse("2026-08-02T00:00:00Z"));

        ExamResult secondResult = ExamFixtures.anExamResult(exam, 1L, 20, List.of(true, false));
        ReflectionTestUtils.setField(secondResult, "id", 100L);
        ReflectionTestUtils.setField(secondResult, "createdAt", Instant.parse("2026-08-01T00:00:00Z"));

        ExamResult filteredOut = ExamFixtures.anExamResult(exam, 3L, 0, List.of(false, false));
        ReflectionTestUtils.setField(filteredOut, "id", 300L);
        ReflectionTestUtils.setField(filteredOut, "createdAt", Instant.parse("2026-08-03T00:00:00Z"));

        Sort sort = Sort.by(Sort.Direction.DESC, StudentGradeQueryService.SORT_FIELD_TOTAL_SCORE);
        given(examRepository.findById(1L)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(1L)).willReturn(List.of(secondResult, topResult, filteredOut));
        given(studentGradeQueryService.getStudentGrades(
            exam,
            List.of(secondResult, topResult, filteredOut),
            55L,
            true,
            false,
            PageRequest.of(0, Integer.MAX_VALUE, sort)
        )).willReturn(new PageImpl<>(List.of(
            new StudentGradeResponse(2L, "010-2222-0002", true, "학생 2", 40, 1, 66.7,
                1, ExamCategory.GRADED, false, Instant.parse("2026-08-02T00:00:00Z"), 200L),
            new StudentGradeResponse(1L, "010-1111-0001", true, "학생 1", 20, 2, 33.3,
                3, ExamCategory.GRADED, false, Instant.parse("2026-08-01T00:00:00Z"), 100L)
        )));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.prepareCsvForExam(1L, 55L, true, false, sort).writeTo(out);

        String csv = csv(out);
        String[] lines = csv.split("\\r\\n");
        assertThat(lines).hasSize(3);
        assertThat(lines[1]).isEqualTo("학생 2,010-2222-0002,테스트 시험,2026-08-02,40,40,2,0,1,66.7,1");
        assertThat(lines[2]).isEqualTo("학생 1,010-1111-0001,테스트 시험,2026-08-01,20,40,1,1,2,33.3,3");
        assertThat(csv).doesNotContain("학생 3");
    }

    @Test
    @DisplayName("prepareCsvForExam: 시험 관리 출처가 아니면 거부한다")
    void prepareCsvForExam_rejectsNonManagementExam() {
        Exam exam = Exam.createForAssignment(
            "과제 시험",
            ExamCategory.GRADED,
            GradingType.ABSOLUTE,
            GradeScale.NINE_GRADE,
            1,
            Map.of("1", "1"),
            Map.of("1", 100),
            Map.of("1", "단답"),
            null
        );
        given(examRepository.findById(9L)).willReturn(Optional.of(exam));

        assertThatThrownBy(() -> service.prepareCsvForExam(
            9L,
            null,
            null,
            null,
            Sort.unsorted()
        ))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ExamErrorCode.INVALID_EXAM_DATA));

        then(studentGradeQueryService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("prepareCsvForExam: 존재하지 않는 시험이면 EXAM_NOT_FOUND를 반환한다")
    void prepareCsvForExam_throwsWhenExamDoesNotExist() {
        given(examRepository.findById(404L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.prepareCsvForExam(
            404L,
            null,
            null,
            null,
            Sort.unsorted()
        ))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ExamErrorCode.EXAM_NOT_FOUND));

        then(studentGradeQueryService).shouldHaveNoInteractions();
    }

    private static String csv(ByteArrayOutputStream out) {
        return out.toString(StandardCharsets.UTF_8).replace("\uFEFF", "");
    }
}
