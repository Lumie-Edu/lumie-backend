package com.lumie.lecture.application.dto;

import com.lumie.lecture.domain.entity.Lecture;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record LectureResponse(
        Long id,
        Long classId,
        Long academyId,
        Long teacherId,
        String title,
        String description,
        String videoFileName,
        Long videoFileSize,
        Integer videoDuration,
        String videoMimeType,
        String transcript,
        String aiSummary,
        List<String> topicsExtracted,
        String processingStatus,
        String processingError,
        String visibility,
        String status,
        boolean uploadCompleted,
        LocalDate lectureDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static LectureResponse from(Lecture lecture) {
        return new LectureResponse(
                lecture.getId(),
                lecture.getClassId(),
                lecture.getAcademyId(),
                lecture.getTeacherId(),
                lecture.getTitle(),
                lecture.getDescription(),
                lecture.getVideoFileName(),
                lecture.getVideoFileSize(),
                lecture.getVideoDuration(),
                lecture.getVideoMimeType(),
                lecture.getTranscript(),
                lecture.getAiSummary(),
                lecture.getTopicsExtracted(),
                lecture.getProcessingStatus().name(),
                lecture.getProcessingError(),
                lecture.getVisibility().name(),
                lecture.getStatus().name(),
                lecture.isUploadCompleted(),
                lecture.getLectureDate(),
                lecture.getCreatedAt(),
                lecture.getUpdatedAt()
        );
    }
}
