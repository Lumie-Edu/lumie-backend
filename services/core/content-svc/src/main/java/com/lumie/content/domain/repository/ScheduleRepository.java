package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository {

    Schedule save(Schedule schedule);

    Optional<Schedule> findById(Long id);

    Optional<Schedule> findByIdWithReservations(Long id);

    Page<Schedule> findAll(Pageable pageable);

    List<Schedule> findByScheduleDateBetween(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByScheduleDateBetweenAndIsAvailableTrue(LocalDate startDate, LocalDate endDate);

    List<Schedule> findByCounselorIdAndScheduleDateBetween(Long counselorId, LocalDate startDate, LocalDate endDate);

    void deleteById(Long id);

    boolean existsById(Long id);
}
