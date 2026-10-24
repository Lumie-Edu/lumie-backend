package com.lumie.exam.domain.repository;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamSource;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select exam from Exam exam where exam.id = :id")
    Optional<Exam> findByIdForUpdate(@Param("id") Long id);

    Page<Exam> findBySource(ExamSource source, Pageable pageable);

    List<Exam> findBySource(ExamSource source);
}
