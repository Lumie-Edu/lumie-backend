package com.lumie.admin.domain.entity;

import com.lumie.admin.domain.vo.AttendanceStatus;
import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "attendances",
    uniqueConstraints = @UniqueConstraint(columnNames = {"admin_id", "work_date"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in_time")
    private LocalDateTime clockInTime;

    @Column(name = "clock_out_time")
    private LocalDateTime clockOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status = AttendanceStatus.PENDING;

    @Column(name = "late_minutes")
    private Integer lateMinutes = 0;

    @Column(name = "early_leave_minutes")
    private Integer earlyLeaveMinutes = 0;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes = 0;

    @Column(name = "note", length = 500)
    private String note;

    @Builder
    private Attendance(Admin admin, LocalDate workDate, LocalDateTime clockInTime,
                       LocalDateTime clockOutTime, AttendanceStatus status,
                       Integer lateMinutes, Integer earlyLeaveMinutes,
                       Integer overtimeMinutes, String note) {
        this.admin = admin;
        this.workDate = workDate;
        this.clockInTime = clockInTime;
        this.clockOutTime = clockOutTime;
        this.status = status != null ? status : AttendanceStatus.PENDING;
        this.lateMinutes = lateMinutes != null ? lateMinutes : 0;
        this.earlyLeaveMinutes = earlyLeaveMinutes != null ? earlyLeaveMinutes : 0;
        this.overtimeMinutes = overtimeMinutes != null ? overtimeMinutes : 0;
        this.note = note;
    }

    public void clockIn(LocalDateTime time, LocalTime scheduledStart) {
        this.clockInTime = time;
        if (scheduledStart != null && time.toLocalTime().isAfter(scheduledStart)) {
            this.lateMinutes = (int) Duration.between(scheduledStart, time.toLocalTime()).toMinutes();
            this.status = AttendanceStatus.LATE;
        } else {
            this.status = AttendanceStatus.PRESENT;
        }
    }

    public void clockOut(LocalDateTime time, LocalTime scheduledEnd) {
        this.clockOutTime = time;
        if (scheduledEnd != null) {
            if (time.toLocalTime().isBefore(scheduledEnd)) {
                this.earlyLeaveMinutes = (int) Duration.between(time.toLocalTime(), scheduledEnd).toMinutes();
            } else if (time.toLocalTime().isAfter(scheduledEnd)) {
                this.overtimeMinutes = (int) Duration.between(scheduledEnd, time.toLocalTime()).toMinutes();
            }
        }
    }

    public void markAbsent() {
        this.status = AttendanceStatus.ABSENT;
    }

    public void update(AttendanceStatus status, Integer lateMinutes, Integer earlyLeaveMinutes,
                       Integer overtimeMinutes, String note) {
        if (status != null) this.status = status;
        if (lateMinutes != null) this.lateMinutes = lateMinutes;
        if (earlyLeaveMinutes != null) this.earlyLeaveMinutes = earlyLeaveMinutes;
        if (overtimeMinutes != null) this.overtimeMinutes = overtimeMinutes;
        if (note != null) this.note = note;
    }
}
