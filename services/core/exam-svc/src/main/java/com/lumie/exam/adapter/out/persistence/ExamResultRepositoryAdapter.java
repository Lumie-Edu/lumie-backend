package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamResultRepositoryAdapter implements ExamResultRepository {

    private final JpaExamResultRepository jpaRepository;

    @Override
    public ExamResult save(ExamResult result) {
        return jpaRepository.save(result);
    }

    @Override
    public List<ExamResult> saveAll(List<ExamResult> results) {
        return jpaRepository.saveAll(results);
    }

    @Override
    public Optional<ExamResult> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId) {
        return jpaRepository.findByExamIdAndStudentId(examId, studentId);
    }

    @Override
    public List<ExamResult> findByExamId(Long examId) {
        return jpaRepository.findByExamId(examId);
    }

    @Override
    public List<ExamResult> findByStudentId(Long studentId) {
        return jpaRepository.findByStudentId(studentId);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByExamIdAndStudentId(Long examId, Long studentId) {
        return jpaRepository.existsByExamIdAndStudentId(examId, studentId);
    }
}
