package com.lumie.lecture.application.dto;

import java.time.LocalDate;

public record UpdateLectureRequest(
        String title,
        String description,
        String visibility,
        String status,
        LocalDate lectureDate
) {}
