package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservations")
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

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "student_phone", length = 20)
    private String studentPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "memo", length = 500)
    private String memo;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Builder
    private Reservation(Schedule schedule, Long studentId, String studentName, String studentPhone,
                        String memo) {
        this.schedule = schedule;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentPhone = studentPhone;
        this.status = ReservationStatus.PENDING;
        this.memo = memo;
    }

    public static Reservation create(Schedule schedule, Long studentId, String studentName,
                                       String studentPhone, String memo) {
        return Reservation.builder()
                .schedule(schedule)
                .studentId(studentId)
                .studentName(studentName)
                .studentPhone(studentPhone)
                .memo(memo)
                .build();
    }

    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelReason = reason;
    }

    public void complete() {
        this.status = ReservationStatus.COMPLETED;
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
