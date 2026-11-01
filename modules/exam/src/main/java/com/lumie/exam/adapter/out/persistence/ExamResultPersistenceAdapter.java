package com.lumie.exam.adapter.out.persistence;

import com.lumie.exam.application.port.out.ExamResultPersistencePort;
import com.lumie.exam.domain.entity.ExamResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExamResultPersistenceAdapter implements ExamResultPersistencePort {

    private final ExamResultJpaRepository jpaRepository;

    @Override public Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId) {
        return jpaRepository.findByExamIdAndStudentId(examId, studentId);
    }
    @Override public Optional<ExamResult> findByIdAndExamId(Long id, Long examId) {
        return jpaRepository.findByIdAndExamId(id, examId);
    }
    @Override public Optional<ExamResult> findByCurrentSubmissionId(Long currentSubmissionId) {
        return jpaRepository.findByCurrentSubmissionId(currentSubmissionId);
    }
    @Override public int countOfficializedByJobId(Long jobId) {
        return jpaRepository.countOfficializedByJobId(jobId);
    }
    @Override public List<JobSavedCountRow> countOfficializedByJobIds(Collection<Long> jobIds) {
        return jpaRepository.countOfficializedByJobIds(jobIds);
    }
    @Override public List<ExamResult> findByExamId(Long examId) { return jpaRepository.findByExamId(examId); }
    @Override public List<ExamResult> findByOmrGradingJobId(Long jobId) {
        return jpaRepository.findByOmrGradingJobId(jobId);
    }
    @Override public List<ExamResult> findByExamIdIn(Collection<Long> examIds) {
        return jpaRepository.findByExamIdIn(examIds);
    }
    @Override public List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId) {
        return jpaRepository.findByExamIdAndStudentIdIsNotNull(examId);
    }
    @Override public List<ExamResult> findByStudentId(Long studentId) {
        return jpaRepository.findByStudentId(studentId);
    }
    @Override public List<ExamResult> findByStudentIdIn(Collection<Long> studentIds) {
        return jpaRepository.findByStudentIdIn(studentIds);
    }
    @Override public boolean existsByExamIdAndStudentId(Long examId, Long studentId) {
        return jpaRepository.existsByExamIdAndStudentId(examId, studentId);
    }
    @Override public List<ExamResult> findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse(String phone) {
        return jpaRepository.findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse(phone);
    }
    @Override public Optional<ExamResult> findByQuestionResultId(Long questionResultId) {
        return jpaRepository.findByQuestionResultId(questionResultId);
    }
    @Override public Optional<ExamResult> findWithQuestionResultsById(Long id) {
        return jpaRepository.findWithQuestionResultsById(id);
    }
    @Override public List<TopStudentAggregateRow> findTopStudentsForCurrentMonth(int limit) {
        return jpaRepository.findTopStudentsForCurrentMonth(limit);
    }
    @Override public ResultAggregateRow aggregateResults() { return jpaRepository.aggregateResults(); }
    @Override public List<GradeBucketRow> aggregateGradeBuckets() { return jpaRepository.aggregateGradeBuckets(); }
    @Override public List<QuestionChoiceCountRow> aggregateQuestionChoices() {
        return jpaRepository.aggregateQuestionChoices();
    }
    @Override public void deleteById(Long id) { jpaRepository.deleteById(id); }
    @Override public Optional<ExamResult> findById(Long id) { return jpaRepository.findById(id); }
    @Override public ExamResult save(ExamResult result) { return jpaRepository.save(result); }
    @Override public ExamResult saveAndFlush(ExamResult result) { return jpaRepository.saveAndFlush(result); }
}
