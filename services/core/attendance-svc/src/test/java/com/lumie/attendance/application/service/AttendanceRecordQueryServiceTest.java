package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.StudentAttendanceStatisticsResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceRecordQueryService 단위 테스트")
class AttendanceRecordQueryServiceTest {

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private StudentReadRepository studentReadRepository;

    @InjectMocks
    private AttendanceRecordQueryService queryService;

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName + " not found in hierarchy of " + target.getClass());
    }

    private StudentReadModel buildStudent(Long id, String name) throws Exception {
        StudentReadModel student = new StudentReadModel() {};
        setField(student, "id", id);
        setField(student, "name", name);
        setField(student, "academyId", 1L);
        setField(student, "isActive", true);
        setField(student, "userId", id + 100L);
        return student;
    }

    private AttendanceRecord buildRecord(Long studentId, AttendanceStatus status) throws Exception {
        AttendanceSession session = AttendanceSession.create(1L, "수업", LocalDate.now(), null, null, 10, null);
        setField(session, "id", 1L);
        AttendanceRecord record = AttendanceRecord.create(session, studentId);
        setField(record, "id", studentId * 10L);
        record.checkIn(status, 1L);
        return record;
    }

    @Test
    @DisplayName("getRecordsBySession() 호출 시 세션의 모든 레코드와 학생 이름을 반환한다")
    void getRecordsBySession_returnsRecordsWithStudentNames() throws Exception {
        // given
        Long sessionId = 1L;
        StudentReadModel student = buildStudent(5L, "홍길동");
        AttendanceRecord record = buildRecord(5L, AttendanceStatus.PRESENT);

        given(recordRepository.findBySessionId(sessionId)).willReturn(List.of(record));
        given(studentReadRepository.findById(5L)).willReturn(Optional.of(student));

        // when
        List<AttendanceRecordResponse> result = queryService.getRecordsBySession(sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("홍길동");
        assertThat(result.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("getRecordsBySession() 호출 시 학생 정보가 없으면 이름이 'Unknown'이다")
    void getRecordsBySession_unknownStudent_usesDefaultName() throws Exception {
        // given
        Long sessionId = 1L;
        AttendanceRecord record = buildRecord(99L, AttendanceStatus.ABSENT);

        given(recordRepository.findBySessionId(sessionId)).willReturn(List.of(record));
        given(studentReadRepository.findById(99L)).willReturn(Optional.empty());

        // when
        List<AttendanceRecordResponse> result = queryService.getRecordsBySession(sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("getStudentStatistics() 호출 시 출석 통계가 올바르게 계산된다")
    void getStudentStatistics_calculatesCorrectly() throws Exception {
        // given
        Long studentId = 5L;
        AttendanceRecord r1 = buildRecord(studentId, AttendanceStatus.PRESENT);
        AttendanceRecord r2 = buildRecord(studentId, AttendanceStatus.ABSENT);
        AttendanceRecord r3 = buildRecord(studentId, AttendanceStatus.LATE);
        AttendanceRecord r4 = buildRecord(studentId, AttendanceStatus.EXCUSED);

        given(recordRepository.findByStudentIdWithSession(studentId))
            .willReturn(List.of(r1, r2, r3, r4));

        // when
        StudentAttendanceStatisticsResponse stats = queryService.getStudentStatistics(studentId);

        // then
        assertThat(stats.studentId()).isEqualTo(studentId);
        assertThat(stats.totalSessions()).isEqualTo(4);
        assertThat(stats.presentCount()).isEqualTo(1);
        assertThat(stats.absentCount()).isEqualTo(1);
        assertThat(stats.lateCount()).isEqualTo(1);
        assertThat(stats.excusedCount()).isEqualTo(1);
        // (PRESENT + EXCUSED) / totalSessions = 2/4 = 50.0
        assertThat(stats.attendanceRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getStudentStatistics() 호출 시 레코드가 없으면 출석률이 0.0이다")
    void getStudentStatistics_noRecords_rateIsZero() {
        // given
        Long studentId = 5L;
        given(recordRepository.findByStudentIdWithSession(studentId)).willReturn(List.of());

        // when
        StudentAttendanceStatisticsResponse stats = queryService.getStudentStatistics(studentId);

        // then
        assertThat(stats.totalSessions()).isEqualTo(0);
        assertThat(stats.attendanceRate()).isEqualTo(0.0);
    }
}
