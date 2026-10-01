package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaComment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record QnaCommentResponse(
        Long id,
        Long qnaId,
        @Schema(nullable = true) Long studentId,
        @Schema(nullable = true) Long userId,
        @Schema(nullable = true) String staffName,
        String commentContent,
        Instant createdAt,
        Instant updatedAt,
        boolean isReadByAuthor
) {
    public static QnaCommentResponse from(QnaComment comment, String staffName) {
        boolean isRead = false;
        if (comment.getQnaBoard().getAuthorLastReadAt() != null) {
            isRead = !comment.getCreatedAt().isAfter(comment.getQnaBoard().getAuthorLastReadAt());
        }

        return new QnaCommentResponse(
                comment.getId(),
                comment.getQnaBoard().getId(),
                comment.getStudentId(),
                comment.getUserId(),
                staffName,
                comment.getCommentContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                isRead
        );
    }
}
