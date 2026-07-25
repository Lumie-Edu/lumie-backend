package com.lumie.exam.domain.entity;

import com.lumie.common.tenant.TenantContextHolder;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "omr_submission_questions", uniqueConstraints = @UniqueConstraint(columnNames = {"omr_submission_id", "question_number"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OmrSubmissionQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "omr_submission_id", nullable = false)
    private OmrSubmission omrSubmission;

    @Column(name = "question_number", nullable = false)
    private Integer questionNumber;

    @Column(name = "student_answer", length = 20)
    private String studentAnswer;

    @Column(name = "correct_answer", length = 20)
    private String correctAnswer;

    @Column(name = "max_score", nullable = false)
    private Integer maxScore;

    @Column(name = "earned_score", nullable = false)
    private Integer earnedScore;

    @Column(name = "question_type", length = 30)
    private String questionType;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private Long tenantId;

    private OmrSubmissionQuestion(int questionNumber, String studentAnswer, String correctAnswer,
                                  int maxScore, int earnedScore, String questionType) {
        this.questionNumber = questionNumber;
        this.studentAnswer = studentAnswer;
        this.correctAnswer = correctAnswer;
        this.maxScore = maxScore;
        this.earnedScore = earnedScore;
        this.questionType = questionType;
    }

    public static OmrSubmissionQuestion create(int questionNumber, String studentAnswer, String correctAnswer,
                                                int maxScore, int earnedScore, String questionType) {
        return new OmrSubmissionQuestion(questionNumber, studentAnswer, correctAnswer, maxScore, earnedScore, questionType);
    }

    void setOmrSubmission(OmrSubmission omrSubmission) {
        this.omrSubmission = omrSubmission;
    }

    @PrePersist
    private void initTenantId() {
        if (tenantId == null) {
            tenantId = TenantContextHolder.getRequiredTenantId();
        }
    }
}
