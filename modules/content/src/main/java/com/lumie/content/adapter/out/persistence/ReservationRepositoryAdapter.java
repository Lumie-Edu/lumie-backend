package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Reservation;
import com.lumie.content.domain.repository.ReservationRepository;
import com.lumie.content.domain.vo.ReservationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryAdapter implements ReservationRepository {

    private final JpaReservationRepository jpaRepository;

    @Override
    public Reservation save(Reservation reservation) {
        return jpaRepository.save(reservation);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Reservation> findByIdWithSchedule(Long id) {
        return jpaRepository.findByIdWithSchedule(id);
    }

    @Override
    public Page<Reservation> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<Reservation> findByStudentId(Long studentId) {
        return jpaRepository.findByStudentId(studentId);
    }

    @Override
    public List<Reservation> findByScheduleId(Long scheduleId) {
        return jpaRepository.findByScheduleId(scheduleId);
    }

    @Override
    public List<Reservation> findByStudentIdAndStatus(Long studentId, ReservationStatus status) {
        return jpaRepository.findByStudentIdAndStatus(studentId, status);
    }

    @Override
    public boolean existsByScheduleIdAndStudentIdAndStatusNot(Long scheduleId, Long studentId, ReservationStatus status) {
        return jpaRepository.existsByScheduleIdAndStudentIdAndStatusNot(scheduleId, studentId, status);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }
}
