package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.AuthorType;
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
    @JoinColumn(name = "qna_board_id", nullable = false)
    private QnaBoard qnaBoard;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_type", nullable = false, length = 20)
    private AuthorType authorType;

    @Builder
    private QnaComment(QnaBoard qnaBoard, String content, Long authorId, String authorName,
                       AuthorType authorType) {
        this.qnaBoard = qnaBoard;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorType = authorType;
    }

    public static QnaComment createFromStudent(QnaBoard qnaBoard, String content, Long studentId,
                                                String studentName) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .content(content)
                .authorId(studentId)
                .authorName(studentName)
                .authorType(AuthorType.STUDENT)
                .build();
    }

    public static QnaComment createFromAdmin(QnaBoard qnaBoard, String content, Long adminId,
                                              String adminName) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .content(content)
                .authorId(adminId)
                .authorName(adminName)
                .authorType(AuthorType.ADMIN)
                .build();
    }

    public boolean isFromAdmin() {
        return this.authorType == AuthorType.ADMIN;
    }
}
