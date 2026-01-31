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

    @Column(name = "academy_id")
    private Long academyId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "is_answered", nullable = false)
    private Boolean isAnswered;

    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @OneToMany(mappedBy = "qnaBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QnaComment> comments = new ArrayList<>();

    @Builder
    private QnaBoard(Long academyId, Long studentId, String title, String content,
                     String category, Boolean isPrivate) {
        this.academyId = academyId;
        this.studentId = studentId;
        this.title = title;
        this.content = content;
        this.category = category;
        this.isAnswered = false;
        this.isPrivate = isPrivate != null ? isPrivate : false;
        this.viewCount = 0;
    }

    public static QnaBoard create(Long academyId, Long studentId, String title, String content,
                                   String category, Boolean isPrivate) {
        return QnaBoard.builder()
                .academyId(academyId)
                .studentId(studentId)
                .title(title)
                .content(content)
                .category(category)
                .isPrivate(isPrivate)
                .build();
    }

    public void update(String title, String content, String category, Boolean isPrivate) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
        if (category != null) {
            this.category = category;
        }
        if (isPrivate != null) {
            this.isPrivate = isPrivate;
        }
    }

    public void markAsAnswered() {
        this.isAnswered = true;
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void addComment(QnaComment comment) {
        this.comments.add(comment);
        if (comment.isAnswer() && !this.isAnswered) {
            this.markAsAnswered();
        }
    }

    public boolean hasAnswerComment() {
        return comments.stream().anyMatch(QnaComment::isAnswer);
    }
}
