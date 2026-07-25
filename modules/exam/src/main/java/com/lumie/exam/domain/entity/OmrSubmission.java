package com.lumie.exam.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import com.lumie.exam.domain.vo.OmrGradingStatus;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "omr_submissions", uniqueConstraints = @UniqueConstraint(columnNames = {"job_id", "image_index"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OmrSubmission extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "image_index", nullable = false)
    private Integer imageIndex;

    @Column(name = "batch_accepted_at", nullable = false, updatable = false)
    private Instant batchAcceptedAt;

    @Column(name = "source_image_key", nullable = false, length = 500)
    private String sourceImageKey;

    @Column(name = "omr_image_key", length = 500)
    private String omrImageKey;

    @Column(name = "ocr_phone_number", length = 20)
    private String ocrPhoneNumber;

    @Column(name = "reconciliation_phone_number", length = 20)
    private String reconciliationPhoneNumber;

    @Column(name = "matched_student_id")
    private Long matchedStudentId;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "grade")
    private Integer grade;

    @Enumerated(EnumType.STRING)
    @Column(name = "grading_status", nullable = false, length = 20)
    private OmrGradingStatus gradingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "reconciliation_status", length = 20)
    private OmrReconciliationStatus reconciliationStatus;

    @Column(name = "reconciliation_reason", length = 100)
    private String reconciliationReason;

    @OneToMany(mappedBy = "omrSubmission", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("questionNumber ASC")
    private List<OmrSubmissionQuestion> questions = new ArrayList<>();

    private OmrSubmission(Exam exam, Long jobId, int imageIndex, String sourceImageKey, Instant batchAcceptedAt) {
        this.exam = exam;
        this.jobId = jobId;
        this.imageIndex = imageIndex;
        this.sourceImageKey = sourceImageKey;
        this.batchAcceptedAt = batchAcceptedAt;
        this.gradingStatus = OmrGradingStatus.PENDING;
    }

    public static OmrSubmission pending(
            Exam exam, Long jobId, int imageIndex, String sourceImageKey, Instant batchAcceptedAt) {
        return new OmrSubmission(exam, jobId, imageIndex, sourceImageKey, batchAcceptedAt);
    }

    public void complete(String phoneNumber, int score, int grade, String permanentImageKey,
                         List<OmrSubmissionQuestion> gradedQuestions) {
        this.gradingStatus = OmrGradingStatus.SUCCEEDED;
        this.ocrPhoneNumber = phoneNumber;
        this.totalScore = score;
        this.grade = grade;
        this.omrImageKey = permanentImageKey;
        this.questions.clear();
        gradedQuestions.forEach(this::addQuestion);
    }

    public void fail(String reason) {
        this.gradingStatus = OmrGradingStatus.FAILED;
        this.reconciliationReason = reason;
    }

    public void markUnmatched(String reason) {
        this.matchedStudentId = null;
        this.reconciliationStatus = OmrReconciliationStatus.UNMATCHED;
        this.reconciliationReason = reason;
    }

    public void markMatched(Long studentId) {
        this.matchedStudentId = studentId;
        this.reconciliationStatus = OmrReconciliationStatus.MATCHED;
        this.reconciliationReason = null;
    }

    public void markAmbiguous() {
        this.matchedStudentId = null;
        this.reconciliationStatus = OmrReconciliationStatus.AMBIGUOUS;
        this.reconciliationReason = "MULTIPLE_SUBMISSIONS_FOR_PHONE";
    }

    public void reject(String reason) {
        this.matchedStudentId = null;
        this.reconciliationStatus = OmrReconciliationStatus.REJECTED;
        this.reconciliationReason = reason;
    }

    public void correctPhone(String phoneNumber) {
        this.reconciliationPhoneNumber = phoneNumber;
        this.matchedStudentId = null;
        this.reconciliationStatus = OmrReconciliationStatus.UNMATCHED;
        this.reconciliationReason = null;
    }

    public String getEffectivePhoneNumber() {
        return reconciliationPhoneNumber != null ? reconciliationPhoneNumber : ocrPhoneNumber;
    }

    public boolean isNewerThan(OmrSubmission other) {
        if (other == null) {
            return true;
        }
        int batchOrder = this.batchAcceptedAt.compareTo(other.batchAcceptedAt);
        return batchOrder > 0 || (batchOrder == 0 && this.imageIndex > other.imageIndex);
    }

    private void addQuestion(OmrSubmissionQuestion question) {
        this.questions.add(question);
        question.setOmrSubmission(this);
    }
}
