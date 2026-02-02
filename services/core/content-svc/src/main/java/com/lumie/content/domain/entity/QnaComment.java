package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "qna_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaComment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private QnaBoard qnaBoard;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "comment_content", nullable = false, columnDefinition = "TEXT")
    private String commentContent;

    @Builder
    private QnaComment(QnaBoard qnaBoard, Long studentId, Long adminId, String commentContent) {
        this.qnaBoard = qnaBoard;
        this.studentId = studentId;
        this.adminId = adminId;
        this.commentContent = commentContent;
    }

    public static QnaComment createByStudent(QnaBoard qnaBoard, Long studentId, String commentContent) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .studentId(studentId)
                .commentContent(commentContent)
                .build();
    }

    public static QnaComment createByAdmin(QnaBoard qnaBoard, Long adminId, String commentContent) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .adminId(adminId)
                .commentContent(commentContent)
                .build();
    }

    public boolean isAdminComment() {
        return this.adminId != null;
    }

    public boolean isStudentComment() {
        return this.studentId != null;
    }
}
