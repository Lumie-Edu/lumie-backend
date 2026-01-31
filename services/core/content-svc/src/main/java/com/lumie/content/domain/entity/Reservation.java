package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Entity
@Table(name = "counseling_reservations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @Column(name = "topic", length = 200)
    private String topic;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Builder
    private Reservation(Schedule schedule, Long studentId, LocalTime reservationTime,
                        String topic, String notes) {
        this.schedule = schedule;
        this.studentId = studentId;
        this.reservationTime = reservationTime;
        this.topic = topic;
        this.notes = notes;
        this.status = ReservationStatus.PENDING;
    }

    public static Reservation create(Schedule schedule, Long studentId, LocalTime reservationTime,
                                       String topic, String notes) {
        return Reservation.builder()
                .schedule(schedule)
                .studentId(studentId)
                .reservationTime(reservationTime)
                .topic(topic)
                .notes(notes)
                .build();
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    public void updateNotes(String notes) {
        if (notes != null) {
            this.notes = notes;
        }
    }

    public boolean isPending() {
        return this.status == ReservationStatus.PENDING;
    }

    public boolean isConfirmed() {
        return this.status == ReservationStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return this.status == ReservationStatus.CANCELLED;
    }

    public boolean isCompleted() {
        return this.status == ReservationStatus.COMPLETED;
    }
}
