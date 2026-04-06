package com.lumie.journal.domain.vo;

import java.time.LocalDate;

public record JournalSearchCriteria(
    Long classId,
    Long academyId,
    Long teacherId,
    JournalStatus status,
    LocalDate startDate,
    LocalDate endDate,
    String search
) {
}
