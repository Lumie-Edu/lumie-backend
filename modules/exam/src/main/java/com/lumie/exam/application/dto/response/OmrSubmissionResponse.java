package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.OmrSubmission;
import com.lumie.exam.domain.entity.OmrSubmissionQuestion;
import com.lumie.exam.domain.vo.OmrGradingStatus;
import com.lumie.exam.domain.vo.OmrReconciliationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record OmrSubmissionResponse(
        Long id,
        Long examId,
        Long jobId,
        Integer imageIndex,
        @Schema(nullable = true) String phoneNumber,
        @Schema(nullable = true) String ocrPhoneNumber,
        @Schema(nullable = true) String reconciliationPhoneNumber,
        @Schema(nullable = true) Long matchedStudentId,
        @Schema(nullable = true) String matchedStudentName,
        @Schema(nullable = true) Integer score,
        @Schema(nullable = true) Integer grade,
        OmrGradingStatus gradingStatus,
        @Schema(nullable = true) OmrReconciliationStatus reconciliationStatus,
        @Schema(nullable = true) String reconciliationReason,
        boolean hasOmrImage,
        boolean isOfficial,
        List<Question> questions,
        Instant createdAt
) {
    public static OmrSubmissionResponse from(OmrSubmission submission, String studentName, boolean official) {
        return new OmrSubmissionResponse(
                submission.getId(),
                submission.getExam().getId(),
                submission.getJobId(),
                submission.getImageIndex(),
                submission.getEffectivePhoneNumber(),
                submission.getOcrPhoneNumber(),
                submission.getReconciliationPhoneNumber(),
                submission.getMatchedStudentId(),
                studentName,
                submission.getTotalScore(),
                submission.getExam().isPassFail() ? null : submission.getGrade(),
                submission.getGradingStatus(),
                submission.getReconciliationStatus(),
                submission.getReconciliationReason(),
                submission.getOmrImageKey() != null,
                official,
                submission.getQuestions().stream().map(Question::from).toList(),
                submission.getCreatedAt()
        );
    }

    public record Question(
            Integer questionNumber,
            @Schema(nullable = true) String studentAnswer,
            @Schema(nullable = true) String correctAnswer,
            Integer maxScore,
            Integer earnedScore,
            @Schema(nullable = true) String questionType
    ) {
        static Question from(OmrSubmissionQuestion question) {
            return new Question(
                    question.getQuestionNumber(), question.getStudentAnswer(), question.getCorrectAnswer(),
                    question.getMaxScore(), question.getEarnedScore(), question.getQuestionType());
        }
    }
}
