package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.response.AttendanceRecordResponse;
import com.lumie.attendance.application.dto.response.ClassStudentAttendanceResponse;
import com.lumie.attendance.application.dto.response.StudentAttendanceStatisticsResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lumie.common.test.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceRecordQueryService 단위 테스트")
class AttendanceRecordQueryServiceTest {

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private StudentReadRepository studentReadRepository;

    @InjectMocks
    private AttendanceRecordQueryService queryService;

    private StudentReadModel buildStudent(Long id, String name) throws Exception {
        StudentReadModel student = new StudentReadModel() {};
        ReflectionTestUtils.setField(student, "id", id);
        ReflectionTestUtils.setField(student, "name", name);
        ReflectionTestUtils.setField(student, "isActive", true);
        ReflectionTestUtils.setField(student, "userId", id + 100L);
        return student;
    }

    private AttendanceRecord buildRecord(Long studentId, AttendanceStatus status) throws Exception {
        AttendanceSession session = AttendanceSession.create("수업", LocalDate.now(), null, null, 10);
        ReflectionTestUtils.setField(session, "id", 1L);
        AttendanceRecord record = AttendanceRecord.create(session, studentId);
        ReflectionTestUtils.setField(record, "id", studentId == null ? 999L : studentId * 10L);
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
        given(studentReadRepository.findAllById(List.of(5L))).willReturn(List.of(student));

        // when
        List<AttendanceRecordResponse> result = queryService.getRecordsBySession(sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("홍길동");
        assertThat(result.get(0).status()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("getRecordsBySession() 호출 시 학생 정보가 없으면 이름이 '삭제된 학생'이다")
    void getRecordsBySession_unknownStudent_usesDefaultName() throws Exception {
        // given
        Long sessionId = 1L;
        AttendanceRecord record = buildRecord(99L, AttendanceStatus.ABSENT);

        given(recordRepository.findBySessionId(sessionId)).willReturn(List.of(record));
        given(studentReadRepository.findAllById(List.of(99L))).willReturn(List.of());

        // when
        List<AttendanceRecordResponse> result = queryService.getRecordsBySession(sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("삭제된 학생");
    }

    @Test
    @DisplayName("getRecordsBySession() 호출 시 studentId가 null이면 이름이 '삭제된 학생'이다")
    void getRecordsBySession_nullStudent_usesDeletedStudentName() throws Exception {
        // given
        Long sessionId = 1L;
        AttendanceRecord record = buildRecord(null, AttendanceStatus.ABSENT);

        given(recordRepository.findBySessionId(sessionId)).willReturn(List.of(record));
        given(studentReadRepository.findAllById(List.of())).willReturn(List.of());

        // when
        List<AttendanceRecordResponse> result = queryService.getRecordsBySession(sessionId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).studentName()).isEqualTo("삭제된 학생");
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
        // (PRESENT + LATE + EXCUSED) / totalSessions = 3/4 = 75.0
        assertThat(stats.attendanceRate()).isEqualTo(75.0);
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

    @Test
    @DisplayName("getClassStudentStatistics() 호출 시 학생 이름 가나다순으로 반환한다")
    void getClassStudentStatistics_sortsByStudentName() throws Exception {
        // given
        Long classId = 10L;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 6, 1);
        AttendanceSession session = AttendanceSession.create("수업", to, classId, "수학", 10);
        ReflectionTestUtils.setField(session, "id", 1L);

        AttendanceRecord r1 = buildRecord(3L, AttendanceStatus.PRESENT);
        AttendanceRecord r2 = buildRecord(1L, AttendanceStatus.PRESENT);
        AttendanceRecord r3 = buildRecord(2L, AttendanceStatus.PRESENT);

        given(sessionRepository.findByClassIdAndSessionDateBetween(classId, from, to))
            .willReturn(List.of(session));
        given(recordRepository.findBySessionIdIn(List.of(1L)))
            .willReturn(List.of(r1, r2, r3));
        given(studentReadRepository.findAllById(any()))
            .willReturn(List.of(
                buildStudent(1L, "다나"),
                buildStudent(2L, "가나"),
                buildStudent(3L, "나나")
            ));

        // when
        List<ClassStudentAttendanceResponse> result = queryService.getClassStudentStatistics(classId, from, to);

        // then
        assertThat(result)
            .extracting(ClassStudentAttendanceResponse::studentName)
            .containsExactly("가나", "나나", "다나");
    }
}
