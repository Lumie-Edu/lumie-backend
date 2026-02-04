package com.lumie.exam.application.dto.response;

import java.util.List;

public record BatchOmrGradingResponse(
        int totalImages,
        int successCount,
        int failCount,
        int savedCount,
        List<BatchOmrResult> results
) {
    public record BatchOmrResult(
            String fileName,
            boolean success,
            boolean saved,
            String phoneNumber,
            Long studentId,
            String studentName,
            Integer totalScore,
            Integer grade,
            String error
    ) {
        public static BatchOmrResult success(String fileName, String phoneNumber, Long studentId,
                                              String studentName, int totalScore, int grade, boolean saved) {
            return new BatchOmrResult(fileName, true, saved, phoneNumber, studentId, studentName, totalScore, grade, null);
        }

        public static BatchOmrResult failure(String fileName, String error) {
            return new BatchOmrResult(fileName, false, false, null, null, null, null, null, error);
        }

        public static BatchOmrResult gradedButNotSaved(String fileName, String phoneNumber, int totalScore, int grade, String reason) {
            return new BatchOmrResult(fileName, true, false, phoneNumber, null, null, totalScore, grade, reason);
        }
    }
}
