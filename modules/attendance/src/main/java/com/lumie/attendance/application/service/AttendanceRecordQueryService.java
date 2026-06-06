package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.response.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.response.ClassAttendanceStatisticsResponse;
import com.lumie.attendance.application.dto.response.ClassAttendanceStatisticsResponse.WeeklyRate;
import com.lumie.attendance.application.dto.response.ClassStudentAttendanceResponse;
import com.lumie.attendance.application.dto.response.StudentAttendanceRecordResponse;
import com.lumie.attendance.application.dto.response.StudentAttendanceStatisticsResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Collator;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordQueryService {

    private static final Collator KOREAN_COLLATOR = Collator.getInstance(Locale.KOREAN);

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentReadRepository studentReadRepository;

    public List<AttendanceRecordResponse> getRecordsBySession(Long sessionId) {
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);

        List<Long> studentIds = records.stream()
            .map(AttendanceRecord::getStudentId)
            .distinct()
            .toList();

        Map<Long, StudentReadModel> studentMap = studentReadRepository.findAllById(studentIds).stream()
            .collect(Collectors.toMap(StudentReadModel::getId, Function.identity()));

        return records.stream()
            .map(record -> {
                StudentReadModel student = studentMap.get(record.getStudentId());
                String studentName = student != null ? student.getName() : "Unknown";
                return AttendanceRecordResponse.from(record, studentName);
            })
            .sorted(Comparator.comparing(AttendanceRecordResponse::studentName, KOREAN_COLLATOR))
            .toList();
    }

    public Page<AttendanceRecordResponse> getRecordsByStudent(Long studentId, Pageable pageable) {
        StudentReadModel student = studentReadRepository.findById(studentId).orElse(null);
        String studentName = student != null ? student.getName() : "Unknown";

        return recordRepository.findByStudentId(studentId, pageable)
            .map(record -> AttendanceRecordResponse.from(record, studentName));
    }

    public List<StudentAttendanceRecordResponse> getStudentRecordsWithSession(Long studentId) {
        StudentReadModel student = studentReadRepository.findById(studentId).orElse(null);
        String studentName = student != null ? student.getName() : "Unknown";

        return recordRepository.findByStudentIdWithSession(studentId).stream()
            .map(record -> StudentAttendanceRecordResponse.from(record, studentName))
            .toList();
    }

    public ClassAttendanceStatisticsResponse getClassStatistics(Long classId, LocalDate from, LocalDate to) {
        List<AttendanceSession> sessions = sessionRepository.findByClassIdAndSessionDateBetween(classId, from, to);

        if (sessions.isEmpty()) {
            String className = sessions.isEmpty() ? "" : sessions.get(0).getClassName();
            return new ClassAttendanceStatisticsResponse(classId, className, 0, 0, 0, 0, 0, 0, 0.0, List.of());
        }

        String className = sessions.get(0).getClassName();
        int totalSessions = sessions.size();
        Set<Long> studentIds = new HashSet<>();
        int presentCount = 0, absentCount = 0, lateCount = 0, excusedCount = 0;

        List<Long> sessionIds = sessions.stream()
            .map(AttendanceSession::getId)
            .toList();
        Map<Long, List<AttendanceRecord>> recordsBySessionId = recordRepository.findBySessionIdIn(sessionIds).stream()
            .collect(Collectors.groupingBy(record -> record.getSession().getId()));

        Map<Integer, List<AttendanceSession>> weeklyGroups = new TreeMap<>();

        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> records = recordsBySessionId.getOrDefault(session.getId(), List.of());
            for (AttendanceRecord record : records) {
                studentIds.add(record.getStudentId());
                switch (record.getStatus()) {
                    case PRESENT -> presentCount++;
                    case ABSENT -> absentCount++;
                    case LATE -> lateCount++;
                    case EXCUSED -> excusedCount++;
                    case PENDING -> {
                        // Pending records count toward total students only.
                    }
                }
            }
            int weekNum = session.getSessionDate().get(WeekFields.ISO.weekOfYear());
            weeklyGroups.computeIfAbsent(weekNum, k -> new ArrayList<>()).add(session);
        }

        int totalRecords = presentCount + absentCount + lateCount + excusedCount;
        double attendanceRate = totalRecords > 0
            ? Math.round((double) (presentCount + lateCount + excusedCount) / totalRecords * 1000) / 10.0
            : 0.0;

        List<WeeklyRate> weeklyTrend = weeklyGroups.entrySet().stream()
            .map(entry -> {
                int wp = 0, wt = 0;
                for (AttendanceSession s : entry.getValue()) {
                    List<AttendanceRecord> recs = recordsBySessionId.getOrDefault(s.getId(), List.of());
                    for (AttendanceRecord r : recs) {
                        if (r.getStatus() == AttendanceStatus.PENDING) continue;
                        wt++;
                        if (r.getStatus() != AttendanceStatus.ABSENT) wp++;
                    }
                }
                double rate = wt > 0 ? Math.round((double) wp / wt * 1000) / 10.0 : 0.0;
                return new WeeklyRate(entry.getKey() + "주차", rate);
            })
            .toList();

        return new ClassAttendanceStatisticsResponse(
            classId, className, totalSessions, studentIds.size(),
            presentCount, absentCount, lateCount, excusedCount,
            attendanceRate, weeklyTrend
        );
    }

    public List<ClassStudentAttendanceResponse> getClassStudentStatistics(Long classId, LocalDate from, LocalDate to) {
        List<AttendanceSession> sessions = sessionRepository.findByClassIdAndSessionDateBetween(classId, from, to);

        Map<Long, int[]> studentCounts = new LinkedHashMap<>();
        List<Long> sessionIds = sessions.stream()
            .map(AttendanceSession::getId)
            .toList();

        if (!sessionIds.isEmpty()) {
            for (AttendanceRecord record : recordRepository.findBySessionIdIn(sessionIds)) {
                int[] counts = studentCounts.computeIfAbsent(record.getStudentId(), k -> new int[5]);
                counts[0]++; // total
                switch (record.getStatus()) {
                    case PRESENT -> counts[1]++;
                    case ABSENT -> counts[2]++;
                    case LATE -> counts[3]++;
                    case EXCUSED -> counts[4]++;
                    case PENDING -> {
                        // Pending records count toward total attendance opportunities only.
                    }
                }
            }
        }

        Map<Long, String> studentNames = studentReadRepository.findAllById(studentCounts.keySet()).stream()
            .collect(Collectors.toMap(StudentReadModel::getId, StudentReadModel::getName));

        return studentCounts.entrySet().stream()
            .map(entry -> {
                Long studentId = entry.getKey();
                int[] c = entry.getValue();
                int decided = c[1] + c[2] + c[3] + c[4]; // exclude PENDING (미인증) from the rate
                double rate = decided > 0
                    ? Math.round((double) (c[1] + c[3] + c[4]) / decided * 1000) / 10.0
                    : 0.0;
                return new ClassStudentAttendanceResponse(
                    studentId, studentNames.getOrDefault(studentId, "Unknown"),
                    c[0], c[1], c[2], c[3], c[4], rate
                );
            })
            .sorted(Comparator.comparing(ClassStudentAttendanceResponse::studentName, KOREAN_COLLATOR))
            .toList();
    }

    public StudentAttendanceStatisticsResponse getStudentStatistics(Long studentId) {
        List<AttendanceRecord> records = recordRepository.findByStudentIdWithSession(studentId);

        int total = records.size();
        int presentCount = (int) records.stream()
            .filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int absentCount = (int) records.stream()
            .filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();
        int lateCount = (int) records.stream()
            .filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int excusedCount = (int) records.stream()
            .filter(r -> r.getStatus() == AttendanceStatus.EXCUSED).count();

        int decided = presentCount + absentCount + lateCount + excusedCount; // exclude PENDING
        double attendanceRate = decided > 0
            ? Math.round((double) (presentCount + lateCount + excusedCount) / decided * 1000) / 10.0
            : 0.0;

        return new StudentAttendanceStatisticsResponse(
            studentId, total, presentCount, absentCount, lateCount, excusedCount, attendanceRate
        );
    }
}
