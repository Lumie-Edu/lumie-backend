package com.lumie.lecture.application.dto.request;

import java.time.LocalDate;

public record UpdateLectureRequest(
        String title,
        String description,
        String visibility,
        String status,
        LocalDate lectureDate
) {}
