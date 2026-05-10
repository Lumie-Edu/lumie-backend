package com.lumie.lecture.application.dto;

import com.lumie.lecture.domain.entity.Lecture;

import java.time.LocalDate;
import java.time.Instant;

public record LectureResponse(
        Long id,
        Long classId,
        Long teacherId,
        String title,
        String description,
        String youtubeUrl,
        String visibility,
        String status,
        LocalDate lectureDate,
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
                lecture.getVisibility().name(),
                lecture.getStatus().name(),
                lecture.getLectureDate(),
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
        );
    }
}
