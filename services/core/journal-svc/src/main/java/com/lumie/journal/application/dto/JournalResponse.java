package com.lumie.journal.application.dto;

import com.lumie.journal.domain.entity.ClassJournal;
import com.lumie.journal.domain.vo.JournalStatus;
import com.lumie.journal.domain.vo.JournalVisibility;
import com.lumie.journal.domain.vo.ProcessingStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record JournalResponse(
    Long id,
    Long classId,
    Long academyId,
    Long teacherId,
    LocalDate lessonDate,
    String title,
    String content,
    String aiSummary,
    String aiScript,
    Long mediaFileId,
    List<Long> attachments,
    List<String> topicsCovered,
    String homeworkNote,
    JournalVisibility visibility,
    ProcessingStatus processingStatus,
    JournalStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static JournalResponse from(ClassJournal journal) {
        return new JournalResponse(
            journal.getId(),
            journal.getClassId(),
            journal.getAcademyId(),
            journal.getTeacherId(),
            journal.getLessonDate(),
            journal.getTitle(),
            journal.getContent(),
            journal.getAiSummary(),
            journal.getAiScript(),
            journal.getMediaFileId(),
            journal.getAttachments(),
            journal.getTopicsCovered(),
            journal.getHomeworkNote(),
            journal.getVisibility(),
            journal.getProcessingStatus(),
            journal.getStatus(),
            journal.getCreatedAt(),
            journal.getUpdatedAt()
        );
    }
}
