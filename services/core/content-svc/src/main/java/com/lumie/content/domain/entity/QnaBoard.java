package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qna_boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaBoard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qna_user_id", nullable = false)
    private Long qnaUserId;

    @Column(name = "qna_title", nullable = false, length = 200)
    private String qnaTitle;

    @Column(name = "qna_content", nullable = false, columnDefinition = "TEXT")
    private String qnaContent;

    @Column(name = "is_it_answered", nullable = false)
    private Boolean isItAnswered;

    @OneToMany(mappedBy = "qnaBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QnaComment> comments = new ArrayList<>();

    @Builder
    private QnaBoard(Long qnaUserId, String qnaTitle, String qnaContent) {
        this.qnaUserId = qnaUserId;
        this.qnaTitle = qnaTitle;
        this.qnaContent = qnaContent;
        this.isItAnswered = false;
    }

    public static QnaBoard create(Long qnaUserId, String qnaTitle, String qnaContent) {
        return QnaBoard.builder()
                .qnaUserId(qnaUserId)
                .qnaTitle(qnaTitle)
                .qnaContent(qnaContent)
                .build();
    }

    public void update(String qnaTitle, String qnaContent) {
        if (qnaTitle != null && !qnaTitle.isBlank()) {
            this.qnaTitle = qnaTitle;
        }
        if (qnaContent != null && !qnaContent.isBlank()) {
            this.qnaContent = qnaContent;
        }
    }

    public void markAsAnswered() {
        this.isItAnswered = true;
    }

    public void addComment(QnaComment comment) {
        this.comments.add(comment);
        if (!this.isItAnswered && comment.getAdminId() != null) {
            this.markAsAnswered();
        }
    }

    public boolean hasAdminComment() {
        return comments.stream().anyMatch(c -> c.getAdminId() != null);
    }
}
