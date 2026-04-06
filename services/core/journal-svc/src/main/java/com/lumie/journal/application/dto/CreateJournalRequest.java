package com.lumie.journal.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateJournalRequest(
    @NotNull(message = "Class ID is required")
    Long classId,

    @NotNull(message = "Academy ID is required")
    Long academyId,

    @NotNull(message = "Teacher ID is required")
    Long teacherId,

    @NotNull(message = "Lesson date is required")
    LocalDate lessonDate,

    @NotBlank(message = "Title is required")
    String title,

    String content,
    List<String> topicsCovered,
    String homeworkNote,
    String visibility,
    List<Long> attachments
) {
}
