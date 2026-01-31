package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;

import java.time.LocalDateTime;

public record QnaBoardResponse(
        Long id,
        String title,
        String content,
        Long studentId,
        String studentName,
        Boolean isAnswered,
        Integer viewCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static QnaBoardResponse from(QnaBoard qnaBoard) {
        return new QnaBoardResponse(
                qnaBoard.getId(),
                qnaBoard.getTitle(),
                qnaBoard.getContent(),
                qnaBoard.getStudentId(),
                qnaBoard.getStudentName(),
                qnaBoard.getIsAnswered(),
                qnaBoard.getViewCount(),
                qnaBoard.getComments() != null ? qnaBoard.getComments().size() : 0,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }
}
