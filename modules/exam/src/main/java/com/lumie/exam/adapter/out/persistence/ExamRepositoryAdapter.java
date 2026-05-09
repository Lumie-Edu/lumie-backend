package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.Exam;
import com.lumie.exam.domain.repository.ExamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamRepositoryAdapter implements ExamRepository {

    private final JpaExamRepository jpaRepository;

    @Override
    public Exam save(Exam exam) {
        return jpaRepository.save(exam);
    }

    @Override
    public Optional<Exam> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Exam> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Page<Exam> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
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
