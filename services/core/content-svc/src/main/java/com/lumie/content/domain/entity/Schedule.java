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
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_name", nullable = false, length = 100)
    private String adminName;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "max_reservations", nullable = false)
    private Integer maxReservations;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Reservation> reservations = new ArrayList<>();

    @Builder
    private Schedule(Long adminId, String adminName, LocalDate scheduleDate,
                     LocalTime startTime, LocalTime endTime, Integer maxReservations,
                     String description, Boolean isAvailable) {
        this.adminId = adminId;
        this.adminName = adminName;
        this.scheduleDate = scheduleDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.maxReservations = maxReservations;
        this.description = description;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }

    public static Schedule create(Long adminId, String adminName, LocalDate scheduleDate,
                                   LocalTime startTime, LocalTime endTime, Integer maxReservations,
                                   String description) {
        return Schedule.builder()
                .adminId(adminId)
                .adminName(adminName)
                .scheduleDate(scheduleDate)
                .startTime(startTime)
                .endTime(endTime)
                .maxReservations(maxReservations)
                .description(description)
                .isAvailable(true)
                .build();
    }

    public void update(LocalDate scheduleDate, LocalTime startTime, LocalTime endTime,
                       Integer maxReservations, String description) {
        if (scheduleDate != null) {
            this.scheduleDate = scheduleDate;
        }
        if (startTime != null) {
            this.startTime = startTime;
        }
        if (endTime != null) {
            this.endTime = endTime;
        }
        if (maxReservations != null) {
            this.maxReservations = maxReservations;
        }
        if (description != null) {
            this.description = description;
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
        long confirmedCount = reservations.stream()
                .filter(r -> r.isConfirmed() || r.isPending())
                .count();
        return confirmedCount < this.maxReservations;
    }

    public int getAvailableSlots() {
        long confirmedCount = reservations.stream()
                .filter(r -> r.isConfirmed() || r.isPending())
                .count();
        return Math.max(0, this.maxReservations - (int) confirmedCount);
    }

    public int getConfirmedCount() {
        return (int) reservations.stream()
                .filter(Reservation::isConfirmed)
                .count();
    }
}
