package com.lumie.attendance.application.service;

import com.lumie.attendance.application.dto.request.BulkUpdateStatusRequest;
import com.lumie.attendance.application.dto.request.CheckInRequest;
import com.lumie.attendance.application.dto.request.UpdateAttendanceStatusRequest;
import com.lumie.attendance.application.dto.response.CheckInResponse;
import com.lumie.attendance.domain.entity.AttendanceRecord;
import com.lumie.attendance.domain.entity.AttendanceSession;
import com.lumie.attendance.domain.entity.StudentReadModel;
import com.lumie.attendance.domain.exception.InvalidCodeException;
import com.lumie.attendance.domain.exception.RecordNotFoundException;
import com.lumie.attendance.domain.repository.AttendanceRecordRepository;
import com.lumie.attendance.domain.repository.AttendanceSessionRepository;
import com.lumie.attendance.domain.repository.StudentReadRepository;
import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lumie.common.test.ReflectionTestUtils;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceRecordCommandService 단위 테스트")
class AttendanceRecordCommandServiceTest {

    @Mock
    private AttendanceSessionRepository sessionRepository;

    @Mock
    private AttendanceRecordRepository recordRepository;

    @Mock
    private StudentReadRepository studentReadRepository;

    @InjectMocks
    private AttendanceRecordCommandService commandService;

    // ------------------------------------------------------------------
    // 헬퍼: 리플렉션으로 읽기 전용 필드 설정
    // ------------------------------------------------------------------

    private StudentReadModel buildStudent(Long id, Long userId) throws Exception {
        StudentReadModel student = new StudentReadModel() {};
        // StudentReadModel has package-private no-args constructor via Lombok
        // Use reflection to set private fields
        ReflectionTestUtils.setField(student, "id", id);
        ReflectionTestUtils.setField(student, "name", "홍길동");
        ReflectionTestUtils.setField(student, "userId", userId);
        ReflectionTestUtils.setField(student, "isActive", true);
        return student;
    }

    private AttendanceSession buildOpenSession(String code) {
        return AttendanceSession.create("수업", LocalDate.now(), null, null, 10);
    }

    // ------------------------------------------------------------------
    // checkInByCode
    // ------------------------------------------------------------------

    @Test
    @DisplayName("유효한 코드로 정시 체크인 시 PRESENT 상태로 응답한다")
    void checkInByCode_onTime_returnsPresent() throws Exception {
        // given
        Long userId = 10L;
        StudentReadModel student = buildStudent(5L, userId);
        AttendanceSession session = buildOpenSession("123456");

        // PRESENT: lateThresholdMinutes=10, sessionDate=today, so minutes since start of today
        // is large (>> 10 minutes) — set sessionDate to future to ensure PRESENT branch
        // Instead, set lateThresholdMinutes to a very large value via a session with big threshold
        AttendanceSession earlySession = AttendanceSession.create("수업", LocalDate.now(), null, null, 99999);
        AttendanceRecord record = AttendanceRecord.create(earlySession, student.getId());

        given(studentReadRepository.findByUserId(userId)).willReturn(Optional.of(student));
        given(sessionRepository.findByAttendanceCode("123456")).willReturn(Optional.of(earlySession));
        given(recordRepository.findBySessionIdAndStudentId(any(), any())).willReturn(Optional.of(record));
        given(recordRepository.save(record)).willReturn(record);

        // when
        CheckInResponse response = commandService.checkInByCode(userId, new CheckInRequest("123456"));

        // then
        assertThat(response.status()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(response.message()).isEqualTo("출석이 완료되었습니다.");
        verify(recordRepository).save(record);
    }

    @Test
    @DisplayName("지각 임계값 초과 시 LATE 상태로 응답한다")
    void checkInByCode_late_returnsLate() throws Exception {
        // given
        Long userId = 10L;
        StudentReadModel student = buildStudent(5L, userId);
        // lateThresholdMinutes=0 → always LATE when checking in during the day
        AttendanceSession lateSession = AttendanceSession.create("수업", LocalDate.now(), null, null, 0);
        AttendanceRecord record = AttendanceRecord.create(lateSession, student.getId());

        given(studentReadRepository.findByUserId(userId)).willReturn(Optional.of(student));
        given(sessionRepository.findByAttendanceCode("654321")).willReturn(Optional.of(lateSession));
        given(recordRepository.findBySessionIdAndStudentId(any(), any())).willReturn(Optional.of(record));
        given(recordRepository.save(record)).willReturn(record);

        // when
        CheckInResponse response = commandService.checkInByCode(userId, new CheckInRequest("654321"));

        // then
        assertThat(response.status()).isEqualTo(AttendanceStatus.LATE);
        assertThat(response.message()).isEqualTo("지각으로 처리되었습니다.");
    }

    @Test
    @DisplayName("학생이 존재하지 않으면 BusinessException을 던진다")
    void checkInByCode_studentNotFound_throwsBusinessException() {
        // given
        Long userId = 10L;
        given(studentReadRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
            commandService.checkInByCode(userId, new CheckInRequest("123456")))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("잘못된 코드이면 InvalidCodeException을 던진다")
    void checkInByCode_invalidCode_throwsInvalidCodeException() throws Exception {
        // given
        Long userId = 10L;
        StudentReadModel student = buildStudent(5L, userId);
        given(studentReadRepository.findByUserId(userId)).willReturn(Optional.of(student));
        given(sessionRepository.findByAttendanceCode("000000")).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
            commandService.checkInByCode(userId, new CheckInRequest("000000")))
            .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    @DisplayName("출석 레코드가 없으면 BusinessException을 던진다")
    void checkInByCode_recordNotFound_throwsBusinessException() throws Exception {
        // given
        Long userId = 10L;
        StudentReadModel student = buildStudent(5L, userId);
        AttendanceSession session = AttendanceSession.create("수업", LocalDate.now(), null, null, 10);

        given(studentReadRepository.findByUserId(userId)).willReturn(Optional.of(student));
        given(sessionRepository.findByAttendanceCode("123456")).willReturn(Optional.of(session));
        given(recordRepository.findBySessionIdAndStudentId(any(), any())).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
            commandService.checkInByCode(userId, new CheckInRequest("123456")))
            .isInstanceOf(BusinessException.class);
    }

    // ------------------------------------------------------------------
    // updateStatus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus() 호출 시 레코드 상태가 갱신되고 저장된다")
    void updateStatus_updatesAndSaves() throws Exception {
        // given
        Long recordId = 1L;
        AttendanceSession session = buildOpenSession("111111");
        AttendanceRecord record = AttendanceRecord.create(session, 5L);
        UpdateAttendanceStatusRequest request = new UpdateAttendanceStatusRequest(AttendanceStatus.EXCUSED, "병결");

        given(recordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(recordRepository.save(record)).willReturn(record);

        // when
        commandService.updateStatus(recordId, request);

        // then
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(record.getMemo()).isEqualTo("병결");
        verify(recordRepository).save(record);
    }

    @Test
    @DisplayName("updateStatus() 호출 시 레코드가 없으면 RecordNotFoundException을 던진다")
    void updateStatus_recordNotFound_throwsRecordNotFoundException() {
        // given
        Long recordId = 999L;
        given(recordRepository.findById(recordId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
            commandService.updateStatus(recordId, new UpdateAttendanceStatusRequest(AttendanceStatus.PRESENT, null)))
            .isInstanceOf(RecordNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // bulkUpdateStatus
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bulkUpdateStatus() 호출 시 해당 세션의 모든 레코드 상태가 갱신된다")
    void bulkUpdateStatus_updatesAllRecords() throws Exception {
        // given
        Long sessionId = 1L;
        AttendanceSession session = buildOpenSession("111111");
        AttendanceRecord r1 = AttendanceRecord.create(session, 1L);
        AttendanceRecord r2 = AttendanceRecord.create(session, 2L);
        List<Long> ids = List.of(1L, 2L);
        BulkUpdateStatusRequest request = new BulkUpdateStatusRequest(ids, AttendanceStatus.ABSENT);

        given(sessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(recordRepository.findAllById(ids)).willReturn(List.of(r1, r2));

        // when
        commandService.bulkUpdateStatus(sessionId, request);

        // then
        assertThat(r1.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(r2.getStatus()).isEqualTo(AttendanceStatus.ABSENT);

        ArgumentCaptor<List<AttendanceRecord>> captor = ArgumentCaptor.forClass(List.class);
        verify(recordRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    @DisplayName("bulkUpdateStatus() 호출 시 세션이 없으면 SessionNotFoundException을 던진다")
    void bulkUpdateStatus_sessionNotFound_throwsSessionNotFoundException() {
        // given
        Long sessionId = 999L;
        given(sessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() ->
            commandService.bulkUpdateStatus(sessionId,
                new BulkUpdateStatusRequest(List.of(1L), AttendanceStatus.ABSENT)))
            .isInstanceOf(BusinessException.class);
    }
}
