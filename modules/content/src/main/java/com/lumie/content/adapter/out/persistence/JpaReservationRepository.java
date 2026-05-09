package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JpaReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.schedule WHERE r.id = :id")
    Optional<Reservation> findByIdWithSchedule(@Param("id") Long id);

    List<Reservation> findByStudentId(Long studentId);

    List<Reservation> findByScheduleId(Long scheduleId);

    List<Reservation> findByStudentIdAndStatus(Long studentId, ReservationStatus status);

    boolean existsByScheduleIdAndStudentIdAndStatusNot(Long scheduleId, Long studentId, ReservationStatus status);
}
