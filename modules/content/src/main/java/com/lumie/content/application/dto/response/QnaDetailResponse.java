package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.QnaBoard;
import com.lumie.content.domain.entity.QnaComment;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record QnaDetailResponse(
        Long id,
        Long qnaUserId,
        String qnaUserName,
        String qnaTitle,
        String qnaContent,
        Boolean isItAnswered,
        List<QnaCommentResponse> comments,
        Instant createdAt,
        Instant updatedAt
) {
    private static final String DELETED_USER_NAME = "삭제된 유저";

    public static QnaDetailResponse from(QnaBoard qnaBoard, String qnaUserName, Map<Long, String> adminNames) {
        List<QnaCommentResponse> commentResponses = qnaBoard.getComments().stream()
                .map(comment -> QnaCommentResponse.from(comment, resolveAdminName(comment, adminNames)))
                .toList();

        return new QnaDetailResponse(
                qnaBoard.getId(),
                qnaBoard.getQnaUserId(),
                qnaUserName,
                qnaBoard.getQnaTitle(),
                qnaBoard.getQnaContent(),
                qnaBoard.getIsItAnswered(),
                commentResponses,
                qnaBoard.getCreatedAt(),
                qnaBoard.getUpdatedAt()
        );
    }

    private static String resolveAdminName(QnaComment comment, Map<Long, String> adminNames) {
        if (comment.getStudentId() != null) {
            return null;
        }
        if (comment.getUserId() == null) {
            return DELETED_USER_NAME;
        }
        return adminNames.getOrDefault(comment.getUserId(), DELETED_USER_NAME);
    }
}
