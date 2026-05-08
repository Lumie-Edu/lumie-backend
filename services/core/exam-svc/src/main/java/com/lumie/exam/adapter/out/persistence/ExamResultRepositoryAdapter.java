package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.exam.domain.exception.DuplicateExamResultException;
import com.lumie.exam.domain.repository.ExamResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamResultRepositoryAdapter implements ExamResultRepository {

    private final JpaExamResultRepository jpaRepository;

    @Override
    public ExamResult save(ExamResult result) {
        try {
            return jpaRepository.save(result);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateExamResultException(e);
        }
    }

    @Override
    public List<ExamResult> saveAll(List<ExamResult> results) {
        try {
            return jpaRepository.saveAll(results);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateExamResultException(e);
        }
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
    public Optional<ExamResult> findByExamIdAndPhoneNumber(Long examId, String phoneNumber) {
        return jpaRepository.findByExamIdAndPhoneNumber(examId, phoneNumber);
    }

    @Override
    public List<ExamResult> findByExamId(Long examId) {
        return jpaRepository.findByExamId(examId);
    }

    @Override
    public List<ExamResult> findByExamIdIn(java.util.Collection<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) return List.of();
        return jpaRepository.findByExamIdIn(examIds);
    }

    @Override
    public List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId) {
        return jpaRepository.findByExamIdAndStudentIdIsNotNull(examId);
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

    @Override
    public int linkUnregisteredByPhone(String phoneNumber, Long studentId) {
        return jpaRepository.linkUnregisteredByPhone(phoneNumber, studentId);
    }
}
