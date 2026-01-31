package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id")
    private Long academyId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic;

    @Column(name = "view_count", nullable = false)
    private Integer viewCount;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Builder
    private Announcement(Long academyId, Long authorId, String title, String content,
                         Boolean isImportant, Boolean isPublic, LocalDateTime publishedAt) {
        this.academyId = academyId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.isImportant = isImportant != null ? isImportant : false;
        this.isPublic = isPublic != null ? isPublic : true;
        this.viewCount = 0;
        this.publishedAt = publishedAt;
    }

    public static Announcement create(Long academyId, Long authorId, String title, String content,
                                       Boolean isImportant, Boolean isPublic) {
        return Announcement.builder()
                .academyId(academyId)
                .authorId(authorId)
                .title(title)
                .content(content)
                .isImportant(isImportant)
                .isPublic(isPublic)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    public void update(String title, String content, Boolean isImportant, Boolean isPublic) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (content != null && !content.isBlank()) {
            this.content = content;
        }
        if (isImportant != null) {
            this.isImportant = isImportant;
        }
        if (isPublic != null) {
            this.isPublic = isPublic;
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void markImportant() {
        this.isImportant = true;
    }

    public void unmarkImportant() {
        this.isImportant = false;
    }
}
