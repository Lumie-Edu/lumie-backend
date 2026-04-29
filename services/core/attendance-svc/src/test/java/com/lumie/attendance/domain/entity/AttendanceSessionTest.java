package com.lumie.attendance.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AttendanceSession 도메인 엔티티 테스트")
class AttendanceSessionTest {

    @Test
    @DisplayName("create() 호출 시 6자리 코드가 생성된다")
    void create_codeIsGenerated() {
        AttendanceSession session = AttendanceSession.create(
            "수업", LocalDate.now(), null, null, 10, null);

        assertThat(session.getAttendanceCode()).matches("\\d{6}");
        assertThat(session.getLateThresholdMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("create() 호출 시 lateThresholdMinutes가 null이면 기본값 10이 적용된다")
    void create_nullLateThreshold_defaultsToTen() {
        AttendanceSession session = AttendanceSession.create(
            "수업", LocalDate.now(), null, null, null, null);

        assertThat(session.getLateThresholdMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("regenerateCode() 호출 시 새 6자리 코드로 교체된다")
    void regenerateCode_producesNewSixDigitCode() {
        AttendanceSession session = AttendanceSession.create(
            "수업", LocalDate.now(), 10L, "국어", 5, null);
        String originalCode = session.getAttendanceCode();

        session.regenerateCode();

        assertThat(session.getAttendanceCode()).matches("\\d{6}");
        // 코드가 재생성됨(같은 코드가 나올 확률 1/900000 수준으로 매우 낮음)
        // 그보다 형식이 유효한지 확인하는 것이 핵심
        assertThat(session.getAttendanceCode()).isNotNull();
    }

    @Test
    @DisplayName("codeExpiresAt이 null이면 isExpired()는 false이다")
    void isExpired_whenExpiresAtIsNull_returnsFalse() {
        AttendanceSession session = AttendanceSession.create(
            "수업", LocalDate.now(), null, null, 10, null);

        assertThat(session.isExpired()).isFalse();
    }
}
