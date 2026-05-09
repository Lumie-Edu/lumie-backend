package com.lumie.lecture.domain.vo;

import java.time.LocalDate;

public record LectureSearchCriteria(
        Long classId,
        Long teacherId,
        LectureStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String search
) {}
