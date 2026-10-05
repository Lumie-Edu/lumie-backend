package com.lumie.content.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qna_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaComment extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private QnaBoard qnaBoard;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "comment_content", nullable = false, columnDefinition = "TEXT")
    private String commentContent;

    @Builder
    private QnaComment(QnaBoard qnaBoard, Long userId, String commentContent) {
        this.qnaBoard = qnaBoard;
        this.userId = userId;
        this.commentContent = commentContent;
    }

    public static QnaComment createByStaff(QnaBoard qnaBoard, Long userId, String commentContent) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .userId(userId)
                .commentContent(commentContent)
                .build();
    }

    public boolean isStaffComment() {
        return this.studentId == null;
    }

    public boolean isStudentComment() {
        return this.studentId != null;
    }
}
