package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaComment;

import java.time.LocalDateTime;

public record QnaCommentResponse(
        Long id,
        Long qnaId,
        Long studentId,
        Long adminId,
        String commentContent,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaCommentResponse from(QnaComment comment) {
        return new QnaCommentResponse(
                comment.getId(),
                comment.getQnaBoard().getId(),
                comment.getStudentId(),
                comment.getAdminId(),
                comment.getCommentContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
