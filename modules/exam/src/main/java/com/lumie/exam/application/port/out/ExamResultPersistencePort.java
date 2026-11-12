package com.lumie.exam.application.port.out;

import com.lumie.exam.domain.entity.ExamResult;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Application-owned result persistence operations and query projections. */
public interface ExamResultPersistencePort {

    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);

    Optional<ExamResult> findByIdAndExamId(Long id, Long examId);

    Optional<ExamResult> findByCurrentSubmissionId(Long currentSubmissionId);

    int countOfficializedByJobId(Long jobId);

    List<JobSavedCountRow> countOfficializedByJobIds(Collection<Long> jobIds);

    List<ExamResult> findByExamId(Long examId);

    List<ExamResult> findByOmrGradingJobId(Long omrGradingJobId);

    List<ExamResult> findByExamIdIn(Collection<Long> examIds);

    List<ExamResult> findByExamIdAndStudentIdIsNotNull(Long examId);

    List<ExamResult> findByStudentId(Long studentId);

    List<ExamResult> findByStudentIdIn(Collection<Long> studentIds);

    boolean existsByExamIdAndStudentId(Long examId, Long studentId);

    List<ExamResult> findByPhoneNumberAndStudentIdIsNullAndStudentDeletedFalse(String phoneNumber);

    Optional<ExamResult> findByQuestionResultId(Long questionResultId);

    Optional<ExamResult> findWithQuestionResultsById(Long id);

    List<TopStudentAggregateRow> findTopStudentsForCurrentMonth(int limit);

    ResultAggregateRow aggregateResults();

    List<GradeBucketRow> aggregateGradeBuckets();

    List<QuestionChoiceCountRow> aggregateQuestionChoices();

    void deleteById(Long id);

    Optional<ExamResult> findById(Long id);

    ExamResult save(ExamResult result);

    List<ExamResult> saveAll(Iterable<ExamResult> results);

    ExamResult saveAndFlush(ExamResult result);

    interface TopStudentAggregateRow {
        Long getStudentId();
        Double getAvgScore();
        Integer getAttemptCount();
    }

    interface JobSavedCountRow {
        Long getJobId();
        long getSavedCount();
    }

    interface ResultAggregateRow {
        long getResultCount();
        long getScoreSum();
    }

    interface GradeBucketRow {
        String getGradeScale();
        Integer getGrade();
        Integer getCnt();
    }

    interface QuestionChoiceCountRow {
        Long getExamId();
        Integer getQuestionNumber();
        String getSelectedChoice();
        Boolean getCorrect();
        Integer getCnt();
    }
}
