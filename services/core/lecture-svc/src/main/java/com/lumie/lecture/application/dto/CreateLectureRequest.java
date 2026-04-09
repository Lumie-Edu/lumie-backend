package com.lumie.lecture.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateLectureRequest(
        Long classId,
        @NotNull Long academyId,
        Long teacherId,
        @NotBlank String title,
        String description,
        @NotBlank String youtubeUrl,
        String visibility,
        LocalDate lectureDate
) {}
