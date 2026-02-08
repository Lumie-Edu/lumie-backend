package com.lumie.attendance.domain.entity;

import com.lumie.attendance.domain.vo.SessionStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Entity
@Table(name = "attendance_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "attendance_code", nullable = false, length = 6)
    private String attendanceCode;

    @Column(name = "code_expires_at")
    private LocalDateTime codeExpiresAt;

    @Column(name = "late_threshold_minutes", nullable = false)
    private Integer lateThresholdMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "session", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<AttendanceRecord> records = new ArrayList<>();

    @Builder
    private AttendanceSession(Long academyId, String name, LocalDate sessionDate, String subject,
                              String attendanceCode, LocalDateTime codeExpiresAt,
                              Integer lateThresholdMinutes, SessionStatus status, Long createdBy) {
        this.academyId = academyId;
        this.name = name;
        this.sessionDate = sessionDate;
        this.subject = subject;
        this.attendanceCode = attendanceCode;
        this.codeExpiresAt = codeExpiresAt;
        this.lateThresholdMinutes = lateThresholdMinutes != null ? lateThresholdMinutes : 10;
        this.status = status != null ? status : SessionStatus.OPEN;
        this.createdBy = createdBy;
    }

    public static AttendanceSession create(Long academyId, String name, LocalDate sessionDate,
                                           String subject, Integer lateThresholdMinutes, Long createdBy) {
        return AttendanceSession.builder()
                .academyId(academyId)
                .name(name)
                .sessionDate(sessionDate)
                .subject(subject)
                .attendanceCode(generateCode())
                .lateThresholdMinutes(lateThresholdMinutes)
                .status(SessionStatus.OPEN)
                .createdBy(createdBy)
                .build();
    }

    public void close() {
        this.status = SessionStatus.CLOSED;
    }

    public void regenerateCode() {
        this.attendanceCode = generateCode();
    }

    public boolean isExpired() {
        return codeExpiresAt != null && LocalDateTime.now().isAfter(codeExpiresAt);
    }

    public boolean isOpen() {
        return this.status == SessionStatus.OPEN;
    }

    private static String generateCode() {
        int code = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(code);
    }
}
