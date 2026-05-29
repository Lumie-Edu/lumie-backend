package com.lumie.lecture.application.dto.response;

import com.lumie.lecture.domain.entity.Lecture;
import com.lumie.lecture.domain.vo.LectureStatus;
import com.lumie.lecture.domain.vo.LectureVisibility;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.Instant;

public record LectureResponse(
        Long id,
        Long classId,
        Long teacherId,
        String title,
        @Schema(nullable = true) String description,
        @Schema(nullable = true) String youtubeUrl,
        LectureVisibility visibility,
        LectureStatus status,
        @Schema(nullable = true) LocalDate lectureDate,
        Instant createdAt,
        Instant updatedAt
) {
    public static LectureResponse from(Lecture lecture) {
        return new LectureResponse(
                lecture.getId(),
                lecture.getClassId(),
                lecture.getTeacherId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getYoutubeUrl(),
                lecture.getVisibility(),
                lecture.getStatus(),
                lecture.getLectureDate(),
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
        );
    }
}
