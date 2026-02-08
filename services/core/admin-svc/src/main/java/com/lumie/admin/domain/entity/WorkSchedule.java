package com.lumie.admin.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "work_schedules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"admin_id", "day_of_week", "effective_from"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "is_day_off", nullable = false)
    private Boolean isDayOff = false;

    @Builder
    private WorkSchedule(Admin admin, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                         LocalTime breakStartTime, LocalTime breakEndTime,
                         LocalDate effectiveFrom, LocalDate effectiveTo, Boolean isDayOff) {
        this.admin = admin;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.isDayOff = isDayOff != null ? isDayOff : false;
    }

    public long calculateWorkMinutes() {
        if (Boolean.TRUE.equals(isDayOff)) {
            return 0;
        }
        long totalMinutes = Duration.between(startTime, endTime).toMinutes();
        if (breakStartTime != null && breakEndTime != null) {
            totalMinutes -= Duration.between(breakStartTime, breakEndTime).toMinutes();
        }
        return totalMinutes;
    }

    public boolean isEffectiveOn(LocalDate date) {
        if (date.isBefore(effectiveFrom)) {
            return false;
        }
        return effectiveTo == null || !date.isAfter(effectiveTo);
    }

    public void update(LocalTime startTime, LocalTime endTime,
                       LocalTime breakStartTime, LocalTime breakEndTime,
                       LocalDate effectiveTo, Boolean isDayOff) {
        if (startTime != null) this.startTime = startTime;
        if (endTime != null) this.endTime = endTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.effectiveTo = effectiveTo;
        if (isDayOff != null) this.isDayOff = isDayOff;
    }
}
