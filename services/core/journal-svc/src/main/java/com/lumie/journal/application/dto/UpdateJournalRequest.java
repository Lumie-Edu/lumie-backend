package com.lumie.journal.application.dto;

import java.time.LocalDate;
import java.util.List;

public record UpdateJournalRequest(
    String title,
    String content,
    List<String> topicsCovered,
    String homeworkNote,
    String visibility,
    String status,
    LocalDate lessonDate,
    List<Long> attachments
) {
}
