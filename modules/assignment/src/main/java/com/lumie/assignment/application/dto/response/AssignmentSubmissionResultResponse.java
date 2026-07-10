package com.lumie.assignment.application.dto.response;

import java.util.List;

public record AssignmentSubmissionResultResponse(
    SubmissionResponse submission,
    boolean showScore,
    boolean showPassFail,
    boolean showGrade,
    boolean showQuestionResults,
    boolean showCorrectAnswers,
    List<AssignmentQuestionResultResponse> questionResults
) {
}
