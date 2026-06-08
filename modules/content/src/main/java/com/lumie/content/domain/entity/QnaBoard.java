package com.lumie.content.domain.entity;

import com.lumie.common.domain.TenantScopedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "qna_boards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaBoard extends TenantScopedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qna_user_id", nullable = false)
    private Long qnaUserId;

    @Column(name = "qna_title", nullable = false, length = 200)
    private String qnaTitle;

    @Column(name = "qna_content", nullable = false, columnDefinition = "TEXT")
    private String qnaContent;

    @Version
    private Long version;

    @Column(name = "is_answered", nullable = false)
    private Boolean isAnswered;

    @Column(name = "author_last_read_at")
    private java.time.Instant authorLastReadAt;

    // @BatchSize avoids N+1 lazy-load when a Page<QnaBoard> iterates over comments.
    // Fetch joining via @EntityGraph would force in-memory pagination (HHH000104),
    // so batch fetching is the canonical pattern for Page + OneToMany.
    @BatchSize(size = 25)
    @OneToMany(mappedBy = "qnaBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QnaComment> comments = new ArrayList<>();

    @Builder
    private QnaBoard(Long qnaUserId, String qnaTitle, String qnaContent) {
        this.qnaUserId = qnaUserId;
        this.qnaTitle = qnaTitle;
        this.qnaContent = qnaContent;
        this.isAnswered = false;
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
        this.isAnswered = true;
    }

    public void markAsUnanswered() {
        this.isAnswered = false;
    }

    public void addComment(QnaComment comment) {
        this.comments.add(comment);
        if (!this.isAnswered && comment.getUserId() != null) {
            this.markAsAnswered();
        }
    }

    public boolean hasAdminComment() {
        return comments.stream().anyMatch(c -> c.getUserId() != null);
    }

    public void markAsReadByAuthor() {
        this.authorLastReadAt = java.time.Instant.now();
    }
}
