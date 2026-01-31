package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;
import java.util.List;

public record QnaDetailResponse(
        Long id,
        String title,
        String content,
        Long studentId,
        String studentName,
        Boolean isAnswered,
        Integer viewCount,
        List<QnaCommentResponse> comments,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaDetailResponse from(QnaBoard qnaBoard) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(QnaCommentResponse::from)
                .toList();

        return new QnaDetailResponse(
                qnaBoard.getId(),
                qnaBoard.getTitle(),
                qnaBoard.getContent(),
                qnaBoard.getStudentId(),
                qnaBoard.getStudentName(),
                qnaBoard.getIsAnswered(),
                qnaBoard.getViewCount(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
