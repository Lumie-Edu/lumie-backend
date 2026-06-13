package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH r.schedule WHERE r.id = :id")
    Optional<Reservation> findByIdWithSchedule(@Param("id") Long id);

    @EntityGraph(attributePaths = {"schedule"})
    List<Reservation> findByStudentId(Long studentId);

    @EntityGraph(attributePaths = {"schedule"})
    List<Reservation> findByScheduleId(Long scheduleId);

    @EntityGraph(attributePaths = {"schedule"})
    List<Reservation> findByStudentIdAndStatus(Long studentId, ReservationStatus status);

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM Reservation r
        WHERE r.studentId = :studentId
          AND r.status IN (
              com.lumie.content.domain.vo.ReservationStatus.PENDING,
              com.lumie.content.domain.vo.ReservationStatus.CONFIRMED
          )
        """)
    boolean existsActiveOrUpcomingByStudentId(@Param("studentId") Long studentId);

    boolean existsByScheduleIdAndStudentIdAndStatusNot(Long scheduleId, Long studentId, ReservationStatus status);

    @Query("""
        SELECT r FROM Reservation r
        JOIN FETCH r.schedule s
        WHERE s.date = :date
          AND r.status <> com.lumie.content.domain.vo.ReservationStatus.CANCELLED
        ORDER BY s.startTime ASC
        """)
    List<Reservation> findByScheduleDateExcludingCancelled(@Param("date") LocalDate date);
}
