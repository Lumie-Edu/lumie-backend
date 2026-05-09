package com.lumie.student.application.service;

import com.lumie.common.csv.CsvWriter;
import com.lumie.internal.auth.AuthService;
import com.lumie.student.application.dto.EnrollmentTrendResponse;
import com.lumie.student.application.dto.StudentResponse;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.exception.StudentNotFoundException;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public StudentResponse getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
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
                .orElseThrow(() -> new StudentNotFoundException(userLoginId));
        return enrich(student);
    }

    public StudentResponse getStudentByPhone(String phone) {
        Student student = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new StudentNotFoundException("phone: " + phone));
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
        return searchStudents(isActive, null, pageable);
    }

    public Page<StudentResponse> searchStudents(Boolean isActive, String search, Pageable pageable) {
        return enrichPage(studentRepository.search(isActive, search, pageable));
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
        LocalDateTime windowStart = firstMonth.atDay(1).atStartOfDay();

        Map<String, Long> bucketed = new HashMap<>();
        for (Object[] row : studentRepository.countMonthlyEnrollmentsSince(windowStart)) {
            bucketed.put((String) row[0], ((Number) row[1]).longValue());
        }

        long cumulative = studentRepository.countByCreatedAtBefore(windowStart);
        List<EnrollmentTrendResponse.MonthlyEnrollment> series = new ArrayList<>(window);
        for (int i = 0; i < window; i++) {
            YearMonth ym = firstMonth.plusMonths(i);
            String key = ym.toString();
            long monthlyNew = bucketed.getOrDefault(key, 0L);
            cumulative += monthlyNew;
            series.add(new EnrollmentTrendResponse.MonthlyEnrollment(key, monthlyNew, cumulative));
        }
        return new EnrollmentTrendResponse(series);
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
            nullToEmpty(s.getName()),
            nullToEmpty(s.getPhone()),
            nullToEmpty(s.getParentPhone()),
            nullToEmpty(s.getStudentHighschool()),
            s.getStudentBirthYear() == null ? "" : s.getStudentBirthYear().toString(),
            nullToEmpty(s.getStudentMemo()),
            s.isActive() ? "재원" : "휴원",
            s.getCreatedAt() == null ? "" : s.getCreatedAt().toLocalDate().format(DATE_FMT)
        );
    }

    private static String nullToEmpty(String v) {
        return Objects.requireNonNullElse(v, "");
    }
}
