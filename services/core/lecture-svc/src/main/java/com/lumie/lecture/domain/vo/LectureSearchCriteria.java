package com.lumie.lecture.domain.vo;

import java.time.LocalDate;

public record LectureSearchCriteria(
        Long classId,
        Long academyId,
        Long teacherId,
        LectureStatus status,
        ProcessingStatus processingStatus,
        LocalDate startDate,
        LocalDate endDate,
        String search
) {}
