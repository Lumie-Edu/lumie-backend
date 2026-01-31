package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;
import java.util.List;

public record QnaDetailResponse(
        Long id,
        Long academyId,
        Long studentId,
        String title,
        String content,
        String category,
        Boolean isAnswered,
        Boolean isPrivate,
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
                qnaBoard.getAcademyId(),
                qnaBoard.getStudentId(),
                qnaBoard.getTitle(),
                qnaBoard.getContent(),
                qnaBoard.getCategory(),
                qnaBoard.getIsAnswered(),
                qnaBoard.getIsPrivate(),
                qnaBoard.getViewCount(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
