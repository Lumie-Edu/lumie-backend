package com.lumie.content.domain.repository;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.vo.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    Optional<Reservation> findByIdWithSchedule(Long id);

    Page<Reservation> findAll(Pageable pageable);

    List<Reservation> findByStudentId(Long studentId);

    List<Reservation> findByScheduleId(Long scheduleId);

    List<Reservation> findByStudentIdAndStatus(Long studentId, ReservationStatus status);

    boolean existsByScheduleIdAndStudentIdAndStatusNot(Long scheduleId, Long studentId, ReservationStatus status);

    void deleteById(Long id);

    boolean existsById(Long id);
}
