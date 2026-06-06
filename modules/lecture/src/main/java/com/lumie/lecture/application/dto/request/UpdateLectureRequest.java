package com.lumie.lecture.application.dto.request;

import java.time.LocalDate;
import java.util.List;

public record UpdateLectureRequest(
        Long classId,
        List<Long> classIds,
        String title,
        String description,
        String youtubeUrl,
        String visibility,
        LocalDate lectureDate
) {}
