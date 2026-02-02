package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "counseling_schedules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"admin_id", "date", "time_slot_id"})
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

    @Column(name = "time_slot_id", nullable = false)
    private Integer timeSlotId;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Reservation> reservations = new ArrayList<>();

    @Builder
    private Schedule(Long adminId, LocalDate date, Integer timeSlotId, Boolean isAvailable) {
        this.adminId = adminId;
        this.date = date;
        this.timeSlotId = timeSlotId;
        this.isAvailable = isAvailable != null ? isAvailable : true;
    }

    public static Schedule create(Long adminId, LocalDate date, Integer timeSlotId) {
        return Schedule.builder()
                .adminId(adminId)
                .date(date)
                .timeSlotId(timeSlotId)
                .isAvailable(true)
                .build();
    }

    public void update(LocalDate date, Integer timeSlotId) {
        if (date != null) {
            this.date = date;
        }
        if (timeSlotId != null) {
            this.timeSlotId = timeSlotId;
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
