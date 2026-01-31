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

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "is_answered", nullable = false)
    private Boolean isAnswered;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @OneToMany(mappedBy = "qnaBoard", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<QnaComment> comments = new ArrayList<>();

    @Builder
    private QnaBoard(String title, String content, Long studentId, String studentName) {
        this.title = title;
        this.content = content;
        this.studentId = studentId;
        this.studentName = studentName;
        this.isAnswered = false;
        this.viewCount = 0;
    }

    public static QnaBoard create(String title, String content, Long studentId, String studentName) {
        return QnaBoard.builder()
                .title(title)
                .content(content)
                .studentId(studentId)
                .studentName(studentName)
                .build();
    }

    public void update(String title, String content) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
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
        if (comment.isFromAdmin() && !this.isAnswered) {
            this.markAsAnswered();
        }
    }

    public boolean hasAdminComment() {
        return comments.stream().anyMatch(QnaComment::isFromAdmin);
    }
}
