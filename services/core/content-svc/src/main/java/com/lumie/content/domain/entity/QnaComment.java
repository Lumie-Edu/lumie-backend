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

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_answer", nullable = false)
    private Boolean isAnswer;

    @Builder
    private QnaComment(QnaBoard qnaBoard, Long authorId, String content, Boolean isAnswer) {
        this.qnaBoard = qnaBoard;
        this.authorId = authorId;
        this.content = content;
        this.isAnswer = isAnswer != null ? isAnswer : false;
    }

    public static QnaComment create(QnaBoard qnaBoard, Long authorId, String content, Boolean isAnswer) {
        return QnaComment.builder()
                .qnaBoard(qnaBoard)
                .authorId(authorId)
                .content(content)
                .isAnswer(isAnswer)
                .build();
    }

    public boolean isAnswer() {
        return Boolean.TRUE.equals(this.isAnswer);
    }

    public void markAsAnswer() {
        this.isAnswer = true;
    }
}
