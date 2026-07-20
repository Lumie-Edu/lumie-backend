package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.common.exception.BusinessException;
import com.lumie.exam.domain.exception.ExamErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExamResult extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "student_deleted", nullable = false)
    private boolean studentDeleted;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    @Column(name = "grade")
    private Integer grade;

    @Column(name = "omr_image_key", length = 500)
    private String omrImageKey;

    @Column(name = "omr_grading_job_id")
    private Long omrGradingJobId;

    @OneToMany(mappedBy = "examResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionResult> questionResults = new ArrayList<>();

    @Builder
    private ExamResult(Exam exam, Long studentId, String phoneNumber, Integer totalScore, Integer grade) {
        this.exam = exam;
        this.studentId = studentId;
        this.phoneNumber = phoneNumber;
        this.totalScore = totalScore;
        this.grade = grade;
    }

    public void assignOmrImage(String omrImageKey) {
        this.omrImageKey = omrImageKey;
    }

    public void assignPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updatePhoneMatch(String phoneNumber, Long studentId) {
        this.phoneNumber = phoneNumber;
        this.studentId = studentId;
        this.studentDeleted = false;
    }

    public void assignOmrGradingJob(Long omrGradingJobId) {
        this.omrGradingJobId = omrGradingJobId;
    }

    public static ExamResult create(Exam exam, Long studentId, Integer totalScore) {
        return ExamResult.builder()
                .exam(exam)
                .studentId(studentId)
                .totalScore(totalScore)
                .build();
    }

    public static ExamResult createForUnregistered(Exam exam, String phoneNumber, Integer totalScore) {
        return ExamResult.builder()
                .exam(exam)
                .phoneNumber(phoneNumber)
                .totalScore(totalScore)
                .build();
    }

    public void assignGrade(Integer grade) {
        this.grade = grade;
    }

    public void addQuestionResult(QuestionResult questionResult) {
        this.questionResults.add(questionResult);
        questionResult.setExamResult(this);
    }

    public void updateScore(Integer newTotalScore) {
        this.totalScore = newTotalScore;
    }

    public void regrade(Integer newTotalScore, List<OmrQuestionUpdate> updates) {
        if (this.questionResults.size() != updates.size()) {
            throw new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA,
                    "Question count mismatch: existing=%d, new=%d"
                            .formatted(this.questionResults.size(), updates.size()));
        }
        java.util.Map<Integer, OmrQuestionUpdate> updateMap = updates.stream()
                .collect(java.util.stream.Collectors.toMap(OmrQuestionUpdate::questionNumber, u -> u));

        this.totalScore = newTotalScore;
        this.grade = null;
        for (QuestionResult existing : this.questionResults) {
            OmrQuestionUpdate update = updateMap.get(existing.getQuestionNumber());
            if (update == null) {
                throw new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA,
                        "No update for question number: " + existing.getQuestionNumber());
            }
            existing.update(update.selectedChoice(), update.correctAnswer(), update.maxScore());
        }
    }

    public QuestionResult updateQuestionAnswer(Long questionResultId, String newSelectedChoice) {
        QuestionResult target = this.questionResults.stream()
                .filter(qr -> qr.getId() != null && qr.getId().equals(questionResultId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ExamErrorCode.INVALID_ANSWER_DATA,
                        "QuestionResult not found: " + questionResultId));

        int questionNumber = target.getQuestionNumber();
        String correctAnswer = this.exam.getCorrectAnswerForQuestion(questionNumber);
        int maxScore = this.exam.getScoreForQuestion(questionNumber);

        target.update(newSelectedChoice, correctAnswer, maxScore);

        this.totalScore = this.questionResults.stream()
                .mapToInt(QuestionResult::getEarnedScore)
                .sum();
        this.grade = null;

        return target;
    }

    public record OmrQuestionUpdate(int questionNumber, String selectedChoice,
                                    String correctAnswer, Integer maxScore) {}

    public boolean isPassed() {
        if (!exam.isPassFail() || exam.getPassScore() == null) {
            return false;
        }
        return totalScore >= exam.getPassScore();
    }

    public int getCorrectCount() {
        return (int) questionResults.stream()
                .filter(QuestionResult::isCorrect)
                .count();
    }

    public int getIncorrectCount() {
        return (int) questionResults.stream()
                .filter(qr -> !qr.isCorrect())
                .count();
    }
}
