package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamPersistencePort;
import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.vo.ExamSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamPersistenceAdapter implements ExamPersistencePort {

    private final ExamJpaRepository jpaRepository;

    @Override
    public Optional<Exam> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Exam> findByIdForUpdate(Long id) {
        return jpaRepository.findByIdForUpdate(id);
    }

    @Override
    public Page<Exam> findBySource(ExamSource source, Pageable pageable) {
        return jpaRepository.findBySource(source, pageable);
    }

    @Override
    public List<Exam> findBySource(ExamSource source) {
        return jpaRepository.findBySource(source);
    }

    @Override
    public Exam save(Exam exam) {
        return jpaRepository.save(exam);
    }

    @Override
    public void delete(Exam exam) {
        jpaRepository.delete(exam);
    }

    @Override
    public void flush() {
        jpaRepository.flush();
    }
}
