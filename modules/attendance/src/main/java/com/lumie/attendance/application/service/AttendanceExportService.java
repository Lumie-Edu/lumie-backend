package com.lumie.attendance.application.service;

import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.common.csv.CsvWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceExportService {

    private static final List<String> HEADERS = List.of(
        "날짜", "세션명", "수업명", "학생명", "출결상태", "체크인 시간", "메모"
    );
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentReadRepository studentReadRepository;

    public void writeCsv(OutputStream out) {
        try (CsvWriter csv = new CsvWriter(out)) {
            csv.writeHeader(HEADERS);

            int pageSize = 200;
            Pageable pageable = PageRequest.of(0, pageSize,
                Sort.by("sessionDate").descending().and(Sort.by("id").descending()));
            Page<AttendanceSession> page;
            do {
                page = sessionRepository.search(null, null, null, pageable);
                writePage(csv, page.getContent());
                csv.flush();
                pageable = pageable.next();
            } while (page.hasNext());
        }
    }

    private void writePage(CsvWriter csv, List<AttendanceSession> sessions) {
        if (sessions.isEmpty()) return;

        Map<Long, List<AttendanceRecord>> recordsBySession = new HashMap<>();
        Set<Long> studentIds = new HashSet<>();
        for (AttendanceSession session : sessions) {
            List<AttendanceRecord> records = recordRepository.findBySessionId(session.getId());
            recordsBySession.put(session.getId(), records);
            for (AttendanceRecord r : records) studentIds.add(r.getStudentId());
        }

        Map<Long, String> studentNames = studentReadRepository.findAllById(List.copyOf(studentIds))
            .stream()
            .collect(Collectors.toMap(StudentReadModel::getId, StudentReadModel::getName));

        for (AttendanceSession session : sessions) {
            for (AttendanceRecord record : recordsBySession.get(session.getId())) {
                csv.writeRow(List.of(
                    session.getSessionDate().format(DATE_FMT),
                    nullToEmpty(session.getName()),
                    nullToEmpty(session.getClassName()),
                    studentNames.getOrDefault(record.getStudentId(), "학생 " + record.getStudentId()),
                    statusLabel(record.getStatus()),
                    record.getCheckedAt() == null ? ""
                        : LocalTime.ofInstant(record.getCheckedAt(), ZoneId.systemDefault()).format(TIME_FMT),
                    nullToEmpty(record.getMemo())
                ));
            }
        }
    }

    private static String statusLabel(AttendanceStatus status) {
        if (status == null) return "";
        return switch (status) {
            case PRESENT -> "출석";
            case ABSENT -> "결석";
            case LATE -> "지각";
            case EXCUSED -> "공결";
        };
    }

    private static String nullToEmpty(String v) {
        return v == null ? "" : v;
    }
}
