package com.lumie.student.application.service;

import com.lumie.student.application.dto.EnrollmentTrendResponse;
import com.lumie.student.application.dto.StudentResponse;
import com.lumie.student.domain.entity.Student;
import com.lumie.student.domain.exception.StudentNotFoundException;
import com.lumie.student.domain.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentQueryService {

    private final StudentRepository studentRepository;

    public StudentResponse getStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new StudentNotFoundException(id));
        return StudentResponse.from(student);
    }

    public List<StudentResponse> getStudentsByIds(List<Long> ids) {
        return studentRepository.findAllByIds(ids).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public List<StudentResponse> getStudentsByUserIds(List<Long> userIds) {
        return studentRepository.findAllByUserIds(userIds).stream()
                .map(StudentResponse::from)
                .toList();
    }

    public StudentResponse getStudentByUserLoginId(String userLoginId) {
        Student student = studentRepository.findByUserLoginId(userLoginId)
                .orElseThrow(() -> new StudentNotFoundException(userLoginId));
        return StudentResponse.from(student);
    }

    public StudentResponse getStudentByPhone(String phone) {
        Student student = studentRepository.findByPhone(phone)
                .orElseThrow(() -> new StudentNotFoundException("phone: " + phone));
        return StudentResponse.from(student);
    }

    public Optional<StudentResponse> findStudentByPhone(String phone) {
        return studentRepository.findByPhone(phone)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllActiveStudents(Pageable pageable) {
        return studentRepository.findAllByIsActive(true, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getAllStudents(Boolean isActive, Pageable pageable) {
        if (isActive == null) {
            return studentRepository.findAll(pageable)
                    .map(StudentResponse::from);
        }
        return studentRepository.findAllByIsActive(isActive, pageable)
                .map(StudentResponse::from);
    }

    public Page<StudentResponse> getStudents(Boolean isActive, Pageable pageable) {
        return searchStudents(isActive, null, pageable);
    }

    public Page<StudentResponse> searchStudents(Boolean isActive, String search, Pageable pageable) {
        return studentRepository.search(isActive, search, pageable)
                .map(StudentResponse::from);
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
}
