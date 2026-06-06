package com.lumie.lecture.application.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.List;

public record CreateLectureRequest(
        Long classId,
        List<Long> classIds,
        Long teacherId,
        @NotBlank String title,
        String description,
        @NotBlank String youtubeUrl,
        String visibility,
        LocalDate lectureDate
) {}
