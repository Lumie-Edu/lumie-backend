package com.lumie.lecture.application.dto.response;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record LectureResponse(
        Long id,
        @Schema(nullable = true) Long classId,
        List<Long> classIds,
        @Schema(nullable = true)
        Long teacherId,
        String title,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String youtubeUrl,
        LectureVisibility visibility,
        @Schema(nullable = true) LocalDate lectureDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static LectureResponse from(Lecture lecture) {
        return from(lecture, List.of());
    }

    public static LectureResponse from(Lecture lecture, List<Long> classIds) {
        return new LectureResponse(
                lecture.getId(),
                classIds == null || classIds.isEmpty() ? null : classIds.get(0),
                classIds == null ? List.of() : classIds,
                lecture.getTeacherId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getYoutubeUrl(),
                lecture.getVisibility(),
                lecture.getLectureDate(),
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
        );
    }
}
