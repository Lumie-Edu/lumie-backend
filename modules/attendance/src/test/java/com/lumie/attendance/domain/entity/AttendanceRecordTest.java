package com.lumie.attendance.domain.entity;

import com.lumie.attendance.domain.vo.AttendanceStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AttendanceRecord 도메인 엔티티 테스트")
class AttendanceRecordTest {

    private AttendanceSession buildSession() {
        return AttendanceSession.create("테스트 세션", LocalDate.now(), null, null, 10);
    }

    @Test
    @DisplayName("create() 호출 시 기본 상태는 ABSENT이다")
    void create_defaultStatusIsAbsent() {
        AttendanceSession session = buildSession();

        AttendanceRecord record = AttendanceRecord.create(session, 42L);

        assertThat(record.getStudentId()).isEqualTo(42L);
        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(record.getCheckedAt()).isNull();
        assertThat(record.getCheckedBy()).isNull();
    }

    @Test
    @DisplayName("checkIn() 호출 시 상태와 checkedBy가 갱신된다")
    void checkIn_updatesStatusAndCheckedBy() {
        AttendanceSession session = buildSession();
        AttendanceRecord record = AttendanceRecord.create(session, 42L);

        record.checkIn(AttendanceStatus.PRESENT, 99L);

        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(record.getCheckedBy()).isEqualTo(99L);
        assertThat(record.getCheckedAt()).isNotNull();
    }

    @Test
    @DisplayName("checkIn() 호출 시 LATE 상태도 정상 저장된다")
    void checkIn_withLateStatus() {
        AttendanceSession session = buildSession();
        AttendanceRecord record = AttendanceRecord.create(session, 42L);

        record.checkIn(AttendanceStatus.LATE, 99L);

        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.LATE);
    }

    @Test
    @DisplayName("updateStatus() 호출 시 상태와 메모가 갱신된다")
    void updateStatus_updatesStatusAndMemo() {
        AttendanceSession session = buildSession();
        AttendanceRecord record = AttendanceRecord.create(session, 42L);

        record.updateStatus(AttendanceStatus.EXCUSED, "병결");

        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.EXCUSED);
        assertThat(record.getMemo()).isEqualTo("병결");
    }

    @Test
    @DisplayName("updateStatus() 호출 시 메모가 null이어도 상태는 갱신된다")
    void updateStatus_withNullMemo() {
        AttendanceSession session = buildSession();
        AttendanceRecord record = AttendanceRecord.create(session, 42L);

        record.updateStatus(AttendanceStatus.PRESENT, null);

        assertThat(record.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(record.getMemo()).isNull();
    }
}
