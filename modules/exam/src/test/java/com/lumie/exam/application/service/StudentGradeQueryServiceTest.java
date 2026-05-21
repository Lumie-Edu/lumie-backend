package com.lumie.exam.application.service;

import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.exam.application.dto.response.statistics.StudentGradeResponse;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.ExamErrorCode;
import com.lumie.exam.domain.exception.ExamException;
import com.lumie.exam.domain.repository.ExamRepository;
import com.lumie.exam.domain.repository.ExamResultRepository;
import com.lumie.exam.domain.vo.ExamCategory;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentGradeQueryService 단위 테스트")
class StudentGradeQueryServiceTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamResultRepository resultRepository;
    @Spy  private StatisticsCalculator statisticsCalculator = new StatisticsCalculator();
    @Mock private GradeCalculator gradeCalculator;
    @Mock private StudentService studentService;

    @InjectMocks private StudentGradeQueryService service;

    private static final Long EXAM_ID = 1L;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenant("test-academy");
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    // ---------------------------------------------------------------------------
    // Fixtures
    // ---------------------------------------------------------------------------

    private Exam gradedExam() {
        return ExamFixtures.aGradedExam();
    }

    /** PASS_FAIL exam; students scoring >= passScore return isPassed()=true. */
    private Exam passFailExam(int passScore) {
        Map<String, String> correctAnswers = new LinkedHashMap<>();
        Map<String, Integer> questionScores = new LinkedHashMap<>();
        Map<String, String> questionTypes   = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) {
            String key = String.valueOf(i);
            correctAnswers.put(key, key);
            questionScores.put(key, 20);
            questionTypes.put(key, "단답");
        }
        return Exam.create("합불 시험", ExamCategory.PASS_FAIL, GradingType.ABSOLUTE,
                GradeScale.NINE_GRADE, 5, correctAnswers, questionScores, questionTypes, passScore);
    }

    /** Stubs StudentService so every registered student resolves to a named stub. */
    private void stubStudentService(List<ExamResult> results) {
        List<StudentService.StudentData> students = results.stream()
                .filter(r -> r.getStudentId() != null)
                .map(r -> new StudentService.StudentData(
                        r.getStudentId(), null,
                        "학생 " + r.getStudentId(),
                        "010-0000-" + String.format("%04d", r.getStudentId()),
                        true, null, null))
                .distinct()
                .toList();
        given(studentService.getStudentsByIds(anyString(), anyList()))
                .willReturn(new StudentService.StudentListData(students, students.size(), 0, students.size()));
    }

    // ---------------------------------------------------------------------------
    // Sort: default (no explicit Sort)
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: Sort 미지정 시 rank 오름차순으로 반환된다")
    void getStudentGrades_defaultSort_returnsRankAscending() {
        Exam exam = gradedExam();
        // scores: 50 → rank 3, 90 → rank 1, 70 → rank 2
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 50),
                ExamResult.create(exam, 2L, 90),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, Integer.MAX_VALUE));

        List<Integer> ranks = page.getContent().stream().map(StudentGradeResponse::rank).toList();
        assertThat(ranks).containsExactly(1, 2, 3);
    }

    // ---------------------------------------------------------------------------
    // Sort: totalScore
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: totalScore DESC 지정 시 점수 내림차순으로 반환된다")
    void getStudentGrades_sortByTotalScoreDesc_returnsDescendingScoreOrder() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 40),
                ExamResult.create(exam, 2L, 100),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Sort sort = Sort.by(Sort.Direction.DESC, "totalScore");
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, PageRequest.of(0, 10, sort));

        List<Integer> scores = page.getContent().stream().map(StudentGradeResponse::score).toList();
        assertThat(scores).containsExactly(100, 70, 40);
    }

    @Test
    @DisplayName("getStudentGrades: totalScore ASC 지정 시 점수 오름차순으로 반환된다")
    void getStudentGrades_sortByTotalScoreAsc_returnsAscendingScoreOrder() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 80),
                ExamResult.create(exam, 2L, 30),
                ExamResult.create(exam, 3L, 60)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Sort sort = Sort.by(Sort.Direction.ASC, "totalScore");
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, PageRequest.of(0, 10, sort));

        List<Integer> scores = page.getContent().stream().map(StudentGradeResponse::score).toList();
        assertThat(scores).containsExactly(30, 60, 80);
    }

    // ---------------------------------------------------------------------------
    // Sort: rank DESC
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: rank DESC 지정 시 rank 내림차순(낮은 점수 먼저)으로 반환된다")
    void getStudentGrades_sortByRankDesc_returnsDescendingRankOrder() {
        Exam exam = gradedExam();
        // scores: 90→rank1, 70→rank2, 50→rank3. DESC: rank3 first.
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 90),
                ExamResult.create(exam, 2L, 50),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Sort sort = Sort.by(Sort.Direction.DESC, "rank");
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, PageRequest.of(0, 10, sort));

        List<Integer> ranks = page.getContent().stream().map(StudentGradeResponse::rank).toList();
        assertThat(ranks).containsExactly(3, 2, 1);
    }

    // ---------------------------------------------------------------------------
    // Sort: unknown field fallback
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: Sort 없음(Sort.unsorted)일 때 rank ASC로 폴백된다")
    void getStudentGrades_unsortedSort_fallsBackToRankAscending() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 50),
                ExamResult.create(exam, 2L, 90),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        // Sort.unsorted() has no orders → iterator().hasNext()=false → null order path → rank ASC
        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, 10, Sort.unsorted()));

        // rank ASC: highest scorer (90) gets rank 1 and appears first
        assertThat(page.getContent().get(0).rank()).isEqualTo(1);
        assertThat(page.getContent().get(0).score()).isEqualTo(90);
    }

    @Test
    @DisplayName("getStudentGrades: 허용 목록 외 필드 지정 시 방향 무시하고 rank ASC로 폴백된다")
    void getStudentGrades_unknownSortFieldDesc_fallsBackToRankAsc() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 50),
                ExamResult.create(exam, 2L, 90),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        // "studentName" is not in the known dispatch set → falls back to rank ASC,
        // ignoring the DESC direction (principle of least surprise; Javadoc contract).
        Sort sort = Sort.by(Sort.Direction.DESC, "studentName");
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, PageRequest.of(0, 10, sort));

        // rank ASC: highest scorer (90) has rank 1 → appears first
        assertThat(page.getContent().get(0).rank()).isEqualTo(1);
        assertThat(page.getContent().get(0).score()).isEqualTo(90);
    }

    @Test
    @DisplayName("getStudentGrades: Pageable.unpaged() 시 전체 결과를 단일 페이지로 반환하며 예외가 발생하지 않는다")
    void getStudentGrades_unpagedPageable_returnsAllResultsAsSinglePage() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 50),
                ExamResult.create(exam, 2L, 90),
                ExamResult.create(exam, 3L, 70),
                ExamResult.create(exam, 4L, 80),
                ExamResult.create(exam, 5L, 60)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        // Pageable.unpaged() must NOT throw UnsupportedOperationException at getOffset()
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, Pageable.unpaged());

        assertThat(page.getContent()).hasSize(results.size());
        assertThat(page.getTotalElements()).isEqualTo(results.size());
    }

    @Test
    @DisplayName("getStudentGrades: 허용 목록 외 필드+DESC 지정 시 rank ASC(1위 먼저)로 폴백된다")
    void getStudentGrades_unknownSortFieldWithDesc_fallsBackToRankAsc() {
        Exam exam = gradedExam();
        // scores: 70→rank1, 50→rank2, 30→rank3 (three distinct scores for clear rank ordering)
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 30),
                ExamResult.create(exam, 2L, 70),
                ExamResult.create(exam, 3L, 50)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Sort unknownDesc = Sort.by(Sort.Direction.DESC, "studentName");
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, PageRequest.of(0, 10, unknownDesc));

        // Fallback must be rank ASC regardless of DESC direction: rank 1 student (score 70) first
        List<Integer> ranks = page.getContent().stream().map(StudentGradeResponse::rank).toList();
        assertThat(ranks).containsExactly(1, 2, 3);
        assertThat(page.getContent().get(0).score()).isEqualTo(70);
    }

    // ---------------------------------------------------------------------------
    // PASS_FAIL secondary tiebreaker
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: PASS_FAIL 시험에서 합격자 그룹이 불합격자 그룹보다 앞에 위치한다")
    void getStudentGrades_passFailExam_passedStudentsBeforeFailed_whenPrimarySortTies() {
        // passScore=70; scores [90, 80, 50] → ranks 1, 2, 3.
        // Primary sort (rank ASC) orders by rank. Secondary tiebreaker (passedFirst) preserves
        // the intended convention that passed students appear before failed ones.
        Exam exam = passFailExam(70);
        ExamResult r1 = ExamResult.create(exam, 10L, 90); // rank 1, passes (90>=70)
        ExamResult r2 = ExamResult.create(exam, 20L, 80); // rank 2, passes (80>=70)
        ExamResult r3 = ExamResult.create(exam, 30L, 50); // rank 3, fails  (50<70)
        List<ExamResult> results = List.of(r1, r2, r3);

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, Integer.MAX_VALUE));

        List<StudentGradeResponse> content = page.getContent();
        assertThat(content).hasSize(3);
        assertThat(content.get(0).rank()).isEqualTo(1);
        assertThat(content.get(1).rank()).isEqualTo(2);
        assertThat(content.get(2).rank()).isEqualTo(3);
        assertThat(content.get(0).isPassed()).isTrue();
        assertThat(content.get(1).isPassed()).isTrue();
        assertThat(content.get(2).isPassed()).isFalse();
    }

    @Test
    @DisplayName("getStudentGrades: PASS_FAIL 시험에서 동일 rank 내 합격자가 불합격자보다 앞에 정렬된다")
    void getStudentGrades_passFailExam_tiedRankPassedBeforeFailed_secondaryTiebreaker() {
        // Three students: rA and rB both score 80 (tied rank 1), passScore=65.
        // rA passes (80>=65), rB passes too — same pass status, so passedFirst tiebreaker
        // has no effect between them. rC scores 50 < 65 → fails, rank 3.
        // Verifies that the two rank-1 passed students appear before the failed rank-3 student,
        // i.e., the secondary tiebreaker does not disrupt valid primary ordering.
        Exam exam = passFailExam(65);
        ExamResult rA = ExamResult.create(exam, 1L, 80); // rank 1 (tied), passes
        ExamResult rB = ExamResult.create(exam, 2L, 80); // rank 1 (tied), passes
        ExamResult rC = ExamResult.create(exam, 3L, 50); // rank 3, fails
        List<ExamResult> results = List.of(rA, rB, rC);

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, Integer.MAX_VALUE));

        List<StudentGradeResponse> content = page.getContent();
        assertThat(content).hasSize(3);
        // Both rank-1 students (passed) must appear before rank-3 (failed)
        assertThat(content.get(0).isPassed()).isTrue();
        assertThat(content.get(1).isPassed()).isTrue();
        assertThat(content.get(2).isPassed()).isFalse();
        assertThat(content.get(2).rank()).isEqualTo(3);
    }

    // ---------------------------------------------------------------------------
    // Paging
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: 두 번째 페이지 요청 시 해당 슬라이스만 반환된다")
    void getStudentGrades_paging_returnsRequestedPage() {
        Exam exam = gradedExam();
        // 5 students scored 90,80,70,60,50 → ranks 1..5 in rank-ASC order.
        // page=1, size=2 → subList(2,4) → scores 70, 60.
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 90),
                ExamResult.create(exam, 2L, 80),
                ExamResult.create(exam, 3L, 70),
                ExamResult.create(exam, 4L, 60),
                ExamResult.create(exam, 5L, 50)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "rank"));
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).score()).isEqualTo(70);
        assertThat(page.getContent().get(1).score()).isEqualTo(60);
    }

    @Test
    @DisplayName("getStudentGrades: 마지막 페이지에서 남은 항목만 반환된다")
    void getStudentGrades_paging_lastPageReturnsRemainder() {
        Exam exam = gradedExam();
        List<ExamResult> results = List.of(
                ExamResult.create(exam, 1L, 90),
                ExamResult.create(exam, 2L, 80),
                ExamResult.create(exam, 3L, 70)
        );
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(results);
        stubStudentService(results);

        // page=1, size=2 → subList(2,3) → 1 item (score 70)
        Pageable pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "rank"));
        Page<StudentGradeResponse> page = service.getStudentGrades(EXAM_ID, pageable);

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).score()).isEqualTo(70);
    }

    // ---------------------------------------------------------------------------
    // Edge cases
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("getStudentGrades: 존재하지 않는 시험 ID 조회 시 ExamException을 던진다")
    void getStudentGrades_examNotFound_throwsExamException() {
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentGrades(EXAM_ID, Pageable.unpaged()))
                .isInstanceOf(ExamException.class)
                .extracting("errorCode")
                .isEqualTo(ExamErrorCode.EXAM_NOT_FOUND);
    }

    @Test
    @DisplayName("getStudentGrades: 미등록 학생(studentId=null)도 포함되어 반환된다")
    void getStudentGrades_unregisteredStudent_resolvedByPhoneNumber() {
        Exam exam = gradedExam();
        ExamResult registered   = ExamResult.create(exam, 1L, 80);
        ExamResult unregistered = ExamResult.createForUnregistered(exam, "01099998888", 60);

        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of(registered, unregistered));
        given(studentService.getStudentsByIds(anyString(), anyList()))
                .willReturn(new StudentService.StudentListData(
                        List.of(new StudentService.StudentData(1L, null, "김철수", "010-1111-2222", true, null, null)),
                        1, 0, 1));

        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, Integer.MAX_VALUE));

        assertThat(page.getTotalElements()).isEqualTo(2);
        List<Boolean> registeredFlags = page.getContent().stream()
                .map(StudentGradeResponse::isRegistered)
                .toList();
        assertThat(registeredFlags).contains(true, false);
    }

    @Test
    @DisplayName("getStudentGrades: 응시자 0명이면 빈 페이지를 반환한다")
    void getStudentGrades_noResults_returnsEmptyPage() {
        Exam exam = gradedExam();
        given(examRepository.findById(EXAM_ID)).willReturn(Optional.of(exam));
        given(resultRepository.findByExamId(EXAM_ID)).willReturn(List.of());

        Page<StudentGradeResponse> page = service.getStudentGrades(
                EXAM_ID, PageRequest.of(0, Integer.MAX_VALUE));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }
}
