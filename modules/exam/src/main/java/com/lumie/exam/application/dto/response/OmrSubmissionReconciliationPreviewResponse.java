package com.lumie.exam.application.dto.response;

import com.lumie.exam.domain.entity.ExamResult;
import com.lumie.student.api.StudentService;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record OmrSubmissionReconciliationPreviewResponse(
        Student student,
        @Schema(nullable = true) CurrentResult currentResult,
        boolean replaceRequired
) {
    public static OmrSubmissionReconciliationPreviewResponse of(
            StudentService.StudentData student, ExamResult currentResult) {
        return new OmrSubmissionReconciliationPreviewResponse(
                new Student(student.id(), student.studentName(), student.studentPhone()),
                currentResult == null ? null : CurrentResult.from(currentResult),
                currentResult != null
        );
    }

    public record Student(Long id, String name, String phoneNumber) {
    }

    public record CurrentResult(
            Long resultId,
            Integer score,
            @Schema(nullable = true) Integer grade,
            @Schema(nullable = true) Long currentSubmissionId,
            @Schema(nullable = true) Instant manualOverrideAt,
            Instant createdAt
    ) {
        static CurrentResult from(ExamResult result) {
            return new CurrentResult(
                    result.getId(),
                    result.getTotalScore(),
                    result.getExam().isPassFail() ? null : result.getGrade(),
                    result.getCurrentSubmissionId(),
                    result.getManualOverrideAt(),
                    result.getCreatedAt()
            );
        }
    }
}
