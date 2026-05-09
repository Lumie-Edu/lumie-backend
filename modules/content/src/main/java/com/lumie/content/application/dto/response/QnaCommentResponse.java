package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaComment;

import java.time.LocalDateTime;

public record QnaCommentResponse(
        Long id,
        Long qnaId,
        Long studentId,
        Long userId,
        String adminName,
        String commentContent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
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
