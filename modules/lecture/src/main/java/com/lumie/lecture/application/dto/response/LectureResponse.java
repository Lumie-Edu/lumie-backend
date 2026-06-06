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
        List<Long> classIds = lecture.getClassId() == null ? List.of() : List.of(lecture.getClassId());
        return from(lecture, classIds);
    }

    public static LectureResponse from(Lecture lecture, List<Long> classIds) {
        return new LectureResponse(
                lecture.getId(),
                classIds == null || classIds.isEmpty() ? lecture.getClassId() : classIds.get(0),
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
