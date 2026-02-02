package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JpaScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s LEFT JOIN FETCH s.reservations WHERE s.id = :id")
    Optional<Schedule> findByIdWithReservations(@Param("id") Long id);

    List<Schedule> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByDateBetweenAndIsAvailableTrue(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByAdminIdAndDateBetween(Long adminId, LocalDate startDate, LocalDate endDate);
}
