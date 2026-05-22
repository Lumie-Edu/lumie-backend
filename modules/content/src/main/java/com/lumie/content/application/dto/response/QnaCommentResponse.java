package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaComment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record QnaCommentResponse(
        Long id,
        Long qnaId,
        @Schema(nullable = true) Long studentId,
        @Schema(nullable = true) Long userId,
        @Schema(nullable = true) String adminName,
        String commentContent,
        Instant createdAt,
        Instant updatedAt
) {
    public static QnaCommentResponse from(QnaComment comment, String adminName) {
        return new QnaCommentResponse(
                comment.getId(),
                comment.getQnaBoard().getId(),
                comment.getStudentId(),
                comment.getUserId(),
                adminName,
                comment.getCommentContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
