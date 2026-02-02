package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "consultation_content", columnDefinition = "TEXT")
    private String consultationContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Builder
    private Reservation(Schedule schedule, Long studentId, Long adminId, String consultationContent) {
        this.schedule = schedule;
        this.studentId = studentId;
        this.adminId = adminId;
        this.consultationContent = consultationContent;
        this.status = ReservationStatus.PENDING;
    }

    public static Reservation create(Schedule schedule, Long studentId, Long adminId, String consultationContent) {
        return Reservation.builder()
                .schedule(schedule)
                .studentId(studentId)
                .adminId(adminId)
                .consultationContent(consultationContent)
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

    public void updateConsultationContent(String consultationContent) {
        if (consultationContent != null) {
            this.consultationContent = consultationContent;
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
