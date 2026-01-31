package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Builder
    private Announcement(String title, String content, Long authorId, String authorName,
                         Boolean isImportant) {
        this.title = title;
        this.content = content;
        this.authorId = authorId;
        this.authorName = authorName;
        this.isImportant = isImportant != null ? isImportant : false;
        this.viewCount = 0;
    }

    public static Announcement create(String title, String content, Long authorId, String authorName,
                                       Boolean isImportant) {
        return Announcement.builder()
                .title(title)
                .content(content)
                .authorId(authorId)
                .authorName(authorName)
                .isImportant(isImportant)
                .build();
    }

    public void update(String title, String content, Boolean isImportant) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
        if (isImportant != null) {
            this.isImportant = isImportant;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void markAsImportant(boolean important) {
        this.isImportant = important;
    }
}
