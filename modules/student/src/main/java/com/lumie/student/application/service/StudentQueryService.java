package com.lumie.student.application.service;

import com.lumie.common.csv.CsvWriter;
import com.lumie.classroom.api.ClassService;
import com.lumie.auth.api.AuthService;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.student.application.dto.response.DropoutSummaryResponse;
import com.lumie.student.application.dto.response.EnrollmentTrendResponse;
import com.lumie.student.application.dto.response.StudentResponse;
import com.lumie.student.domain.entity.Student;
import com.lumie.common.exception.BusinessException;
import com.lumie.student.domain.exception.StudentErrorCode;
import com.lumie.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQueryService {

    private final StudentRepository studentRepository;
    private final AuthService authService;
    private final ClassService classService;

    public StudentResponse getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with id: %d".formatted(id)));
        return enrich(student);
    }

    public List<StudentResponse> getStudentsByIds(List<Long> ids) {
        return enrichList(studentRepository.findAllById(ids));
    }

    public List<StudentResponse> getStudentsByUserIds(List<Long> userIds) {
        return enrichList(studentRepository.findByUserIdIn(userIds));
    }

    public StudentResponse getStudentByUserLoginId(String userLoginId) {
        Student student = studentRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with email: %s".formatted(userLoginId)));
        return enrich(student);
    }

    public StudentResponse getStudentByPhone(String phone) {
        Student student = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new BusinessException(StudentErrorCode.STUDENT_NOT_FOUND, "Student not found with email: phone: %s".formatted(phone)));
        return enrich(student);
    }

    public Optional<StudentResponse> findStudentByPhone(String phone) {
        return studentRepository.findByPhone(phone)
                .map(this::enrich);
    }

    public Page<StudentResponse> getAllActiveStudents(Pageable pageable) {
        return enrichPage(studentRepository.findByIsActive(true, pageable));
    }

    public Page<StudentResponse> getAllStudents(Boolean isActive, Pageable pageable) {
        if (isActive == null) {
            return enrichPage(studentRepository.findAll(pageable));
        }
        return enrichPage(studentRepository.findByIsActive(isActive, pageable));
    }

    public Page<StudentResponse> getStudents(Boolean isActive, Pageable pageable) {
        return searchStudents(isActive, null, null, null, pageable);
    }

    public Page<StudentResponse> searchStudents(
            Boolean isActive,
            String search,
            Long classId,
            Boolean hasActiveEnrollment,
            Pageable pageable
    ) {
        List<Long> includedStudentIds = null;
        List<Long> excludedStudentIds = null;

        if (classId != null) {
            includedStudentIds = classService.getEnrolledStudentIds(
                    TenantContextHolder.getRequiredTenant(),
                    TenantContextHolder.getRequiredTenantId(),
                    classId
            );
            if (includedStudentIds.isEmpty()) {
                return Page.empty(pageable);
            }
        } else if (hasActiveEnrollment != null) {
            List<Long> activeEnrollmentStudentIds = classService
                    .getActiveEnrolledStudentIds(
                            TenantContextHolder.getRequiredTenant(),
                            TenantContextHolder.getRequiredTenantId()
                    );
            if (Boolean.TRUE.equals(hasActiveEnrollment)) {
                if (activeEnrollmentStudentIds.isEmpty()) {
                    return Page.empty(pageable);
                }
                includedStudentIds = activeEnrollmentStudentIds;
            } else if (!activeEnrollmentStudentIds.isEmpty()) {
                excludedStudentIds = activeEnrollmentStudentIds;
            }
        }

        return enrichPage(studentRepository.search(
                isActive,
                search,
                includedStudentIds,
                excludedStudentIds,
                pageable
        ));
    }

    private StudentResponse enrich(Student student) {
        Map<Long, String> seeds = authService.getAvatarSeeds(List.of(student.getUserId()));
        return StudentResponse.from(student, seeds.get(student.getUserId()));
    }

    private List<StudentResponse> enrichList(List<Student> students) {
        Map<Long, String> seeds = avatarSeedsFor(students);
        return students.stream()
                .map(s -> StudentResponse.from(s, seeds.get(s.getUserId())))
                .toList();
    }

    private Page<StudentResponse> enrichPage(Page<Student> page) {
        Map<Long, String> seeds = avatarSeedsFor(page.getContent());
        return page.map(s -> StudentResponse.from(s, seeds.get(s.getUserId())));
    }

    private Map<Long, String> avatarSeedsFor(Collection<Student> students) {
        if (students.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = students.stream().map(Student::getUserId).toList();
        return authService.getAvatarSeeds(userIds);
    }

    public long countActiveStudents() {
        return studentRepository.countByIsActive(true);
    }

    public EnrollmentTrendResponse getEnrollmentTrend(int months) {
        int window = Math.max(1, Math.min(months, 24));
        ZoneId zone = ZoneId.systemDefault();
        YearMonth currentMonth = YearMonth.now(zone);
        YearMonth firstMonth = currentMonth.minusMonths(window - 1L);
        Instant windowStart = firstMonth.atDay(1).atStartOfDay(zone).toInstant();

        Map<String, Long> bucketed = new HashMap<>();
        for (Object[] row : studentRepository.countMonthlyEnrollmentsSince(windowStart)) {
            bucketed.put((String) row[0], ((Number) row[1]).longValue());
        }

        Map<String, Long> dropoutBucketed = new HashMap<>();
        for (Object[] row : studentRepository.countMonthlyDropoutsSince(windowStart)) {
            dropoutBucketed.put((String) row[0], ((Number) row[1]).longValue());
        }

        long cumulative = studentRepository.countByCreatedAtBefore(windowStart);
        List<EnrollmentTrendResponse.MonthlyEnrollment> series = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            YearMonth ym = firstMonth.plusMonths(i);
            String key = ym.toString();
            long monthlyNew = bucketed.getOrDefault(key, 0L);
            long monthlyDropouts = dropoutBucketed.getOrDefault(key, 0L);
            cumulative += monthlyNew;
            series.add(new EnrollmentTrendResponse.MonthlyEnrollment(
                    key, monthlyNew, cumulative, monthlyDropouts, monthlyNew - monthlyDropouts));
        }
        return new EnrollmentTrendResponse(series);
    }

    /**
     * Returns a dropout summary for the current and previous calendar month in KST
     * (Asia/Seoul).
     *
     * <p>Dropout count is approximated via {@code updated_at} on rows where
     * {@code is_active = false}, since the {@code students} table has no dedicated
     * {@code deactivated_at} column. The denominator for {@code thisMonthDropoutRate}
     * is the estimated count of students active at the start of the current month:
     * currently-active students plus those deactivated during the current month.
     */
    public DropoutSummaryResponse getDropoutSummary() {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        YearMonth currentMonth = YearMonth.now(kst);
        YearMonth lastMonth = currentMonth.minusMonths(1);

        Instant thisMonthStart = currentMonth.atDay(1).atStartOfDay(kst).toInstant();
        Instant thisMonthEnd = currentMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(kst).toInstant();
        Instant lastMonthStart = lastMonth.atDay(1).atStartOfDay(kst).toInstant();
        Instant lastMonthEnd = lastMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(kst).toInstant();

        long thisMonthDropouts = studentRepository.countByIsActiveAndUpdatedAtBetween(
                false, thisMonthStart, thisMonthEnd);
        long lastMonthDropouts = studentRepository.countByIsActiveAndUpdatedAtBetween(
                false, lastMonthStart, lastMonthEnd);

        // Active at start of this month = currently active + those deactivated during this month
        long currentlyActive = studentRepository.countByIsActive(true);
        long activeAtMonthStart = currentlyActive + thisMonthDropouts;

        double dropoutRate = activeAtMonthStart == 0
                ? 0.0
                : (double) thisMonthDropouts / activeAtMonthStart;

        return new DropoutSummaryResponse(thisMonthDropouts, dropoutRate, lastMonthDropouts);
    }

    public boolean existsByUserLoginId(String userLoginId) {
        return studentRepository.existsByUserLoginId(userLoginId);
    }

    private static final List<String> EXPORT_HEADERS = List.of(
        "이름", "휴대폰", "학부모 휴대폰", "학교", "출생연도", "메모", "상태", "등록일"
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    public void writeCsv(OutputStream out) {
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(EXPORT_HEADERS);

            int pageSize = 500;
            int pageNumber = 0;
            Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by("createdAt").descending());
            Page<Student> page;
            do {
                page = studentRepository.findAll(pageable);
                for (Student s : page.getContent()) {
                    csv.writeRow(toRow(s));
                }
                csv.flush();
                pageable = pageable.next();
            } while (page.hasNext());
        }
    }

    private List<String> toRow(Student s) {
        return List.of(
            spreadsheetSafe(nullToEmpty(s.getName())),
            spreadsheetSafe(nullToEmpty(s.getPhone())),
            spreadsheetSafe(nullToEmpty(s.getParentPhone())),
            spreadsheetSafe(nullToEmpty(s.getStudentHighschool())),
            spreadsheetSafe(s.getStudentBirthYear() == null ? "" : s.getStudentBirthYear().toString()),
            spreadsheetSafe(nullToEmpty(s.getStudentMemo())),
            s.isActive() ? "재원" : "휴원",
            s.getCreatedAt() == null ? "" : LocalDate.ofInstant(s.getCreatedAt(), ZoneId.systemDefault()).format(DATE_FMT)
        );
    }

    private static String nullToEmpty(String v) {
        return Objects.requireNonNullElse(v, "");
    }

    private static String spreadsheetSafe(String value) {
        if (value.isBlank()) {
            return value;
        }
        char first = value.stripLeading().charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }
}
