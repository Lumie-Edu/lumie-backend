package com.lumie.lecture.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateLectureRequest(
        Long classId,
        Long teacherId,
        @NotBlank String title,
        String description,
        @NotBlank String youtubeUrl,
        String visibility,
        LocalDate lectureDate
) {}
