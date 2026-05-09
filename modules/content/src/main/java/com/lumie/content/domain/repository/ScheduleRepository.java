package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Schedule;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @EntityGraph(attributePaths = {"reservations"})
    @Query("SELECT s FROM Schedule s WHERE s.id = :id")
    Optional<Schedule> findByIdWithReservations(Long id);

    List<Schedule> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByDateBetweenAndIsAvailableTrue(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByUserIdAndDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}
