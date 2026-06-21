package com.lumie.exam.application.dto.response;

import com.lumie.exam.application.dto.response.statistics.ExamStatisticsResponse;
import com.lumie.exam.application.dto.response.statistics.StabilityIndexResponse;
import com.lumie.exam.application.dto.response.statistics.StudentRankResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ReportBatchDataResponse(
        ExamStatisticsResponse examStatistics,
        List<Item> items
) {
    public record Item(
            Long studentId,
            boolean success,
            @Schema(nullable = true) String error,
            @Schema(nullable = true) StudentInfo student,
            @Schema(nullable = true) ExamResultResponse examResult,
            @Schema(nullable = true) StudentRankResponse studentRank,
            StabilityIndexResponse stabilityIndex,
            List<QuestionResultResponse> questionResults
    ) {
        public static Item success(
                Long studentId,
                StudentInfo student,
                ExamResultResponse examResult,
                StudentRankResponse studentRank,
                StabilityIndexResponse stabilityIndex,
                List<QuestionResultResponse> questionResults
        ) {
            return new Item(
                    studentId, true, null, student, examResult, studentRank, stabilityIndex, questionResults);
        }

        public static Item failure(Long studentId, String error) {
            return new Item(studentId, false, error, null, null, null, null, List.of());
        }
    }

    public record StudentInfo(
            Long id,
            String studentName,
            @Schema(nullable = true) String studentPhone
    ) {
    }
}
