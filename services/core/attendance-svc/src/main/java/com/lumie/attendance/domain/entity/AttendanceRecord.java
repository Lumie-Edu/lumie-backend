package com.lumie.attendance.domain.entity;

import com.lumie.attendance.domain.vo.AttendanceStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "student_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AttendanceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @Builder
    private AttendanceRecord(AttendanceSession session, Long studentId, AttendanceStatus status,
                             LocalDateTime checkedAt, Long checkedBy, String memo) {
        this.session = session;
        this.studentId = studentId;
        this.status = status != null ? status : AttendanceStatus.ABSENT;
        this.checkedAt = checkedAt;
        this.checkedBy = checkedBy;
        this.memo = memo;
    }

    public static AttendanceRecord create(AttendanceSession session, Long studentId) {
        return AttendanceRecord.builder()
                .session(session)
                .studentId(studentId)
                .status(AttendanceStatus.ABSENT)
                .build();
    }

    public void updateStatus(AttendanceStatus status, String memo) {
        this.status = status;
        this.memo = memo;
    }

    public void checkIn(AttendanceStatus status, Long checkedBy) {
        this.status = status;
        this.checkedAt = LocalDateTime.now();
        this.checkedBy = checkedBy;
    }
}
