package com.lumie.content.adapter.out.persistence;

import com.lumie.content.domain.entity.Schedule;
import com.lumie.content.domain.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ScheduleRepositoryAdapter implements ScheduleRepository {

    private final JpaScheduleRepository jpaRepository;

    @Override
    public Schedule save(Schedule schedule) {
        return jpaRepository.save(schedule);
    }

    @Override
    public Optional<Schedule> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Schedule> findByIdWithReservations(Long id) {
        return jpaRepository.findByIdWithReservations(id);
    }

    @Override
    public Page<Schedule> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<Schedule> findByScheduleDateBetween(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByScheduleDateBetween(startDate, endDate);
    }

    @Override
    public List<Schedule> findByScheduleDateBetweenAndIsAvailableTrue(LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByScheduleDateBetweenAndIsAvailableTrue(startDate, endDate);
    }

    @Override
    public List<Schedule> findByCounselorIdAndScheduleDateBetween(Long counselorId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByCounselorIdAndScheduleDateBetween(counselorId, startDate, endDate);
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
