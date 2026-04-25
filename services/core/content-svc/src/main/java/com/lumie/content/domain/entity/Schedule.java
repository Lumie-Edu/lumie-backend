package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "counseling_schedules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"admin_id", "date", "start_time"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Reservation> reservations = new ArrayList<>();

    @Builder
    private Schedule(Long adminId, LocalDate date, LocalTime startTime, LocalTime endTime, Boolean isAvailable) {
        this.adminId = adminId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }

    public static Schedule create(Long adminId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        return Schedule.builder()
                .adminId(adminId)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .isAvailable(true)
                .build();
    }

    public void update(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date != null) {
            this.date = date;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
    }

    public void close() {
        this.isAvailable = false;
    }

    public void open() {
        this.isAvailable = true;
    }

    public boolean canAcceptReservation() {
        if (!this.isAvailable) {
            return false;
        }
        return reservations.stream()
                .noneMatch(r -> r.isConfirmed() || r.isPending());
    }

    public boolean hasReservation() {
        return reservations.stream()
                .anyMatch(r -> r.isConfirmed() || r.isPending());
    }

    public int getConfirmedCount() {
        return (int) reservations.stream()
                .filter(Reservation::isConfirmed)
                .count();
    }
}
