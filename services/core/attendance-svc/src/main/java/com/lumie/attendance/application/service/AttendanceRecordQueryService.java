package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.StudentAttendanceRecordResponse;
import com.lumie.attendance.application.dto.StudentAttendanceStatisticsResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRecordQueryService {

    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;

    public List<AttendanceRecordResponse> getRecordsBySession(Long sessionId) {
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);

        List<Long> studentIds = records.stream()
            .map(AttendanceRecord::getStudentId)
            .distinct()
            .toList();

        Map<Long, StudentReadModel> studentMap = studentIds.stream()
            .map(id -> studentReadRepository.findById(id).orElse(null))
            .filter(s -> s != null)
            .collect(Collectors.toMap(StudentReadModel::getId, Function.identity()));

        return records.stream()
            .map(record -> {
                StudentReadModel student = studentMap.get(record.getStudentId());
                String studentName = student != null ? student.getName() : "Unknown";
                return AttendanceRecordResponse.from(record, studentName);
            })
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

        double attendanceRate = total > 0
            ? Math.round((double) (presentCount + excusedCount) / total * 1000) / 10.0
            : 0.0;

        return new StudentAttendanceStatisticsResponse(
            studentId, total, presentCount, absentCount, lateCount, excusedCount, attendanceRate
        );
    }
}
