package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.TextbookCategory;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "textbooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Textbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private TextbookCategory category;

    @Column(name = "file_id")
    private Long fileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "is_important", nullable = false)
    private Boolean isImportant;

    @Column(name = "download_count", nullable = false)
    private Integer downloadCount;

    @Builder
    private Textbook(String title, String description, TextbookCategory category,
                     Long fileId, String fileName, String fileUrl, Long fileSize,
                     Long authorId, String authorName, Boolean isImportant) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.fileId = fileId;
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.fileSize = fileSize;
        this.authorId = authorId;
        this.authorName = authorName;
        this.isImportant = isImportant != null ? isImportant : false;
        this.downloadCount = 0;
    }

    public static Textbook create(String title, String description, TextbookCategory category,
                                   Long fileId, String fileName, String fileUrl, Long fileSize,
                                   Long authorId, String authorName, Boolean isImportant) {
        return Textbook.builder()
                .title(title)
                .description(description)
                .category(category)
                .fileId(fileId)
                .fileName(fileName)
                .fileUrl(fileUrl)
                .fileSize(fileSize)
                .authorId(authorId)
                .authorName(authorName)
                .isImportant(isImportant)
                .build();
    }

    public void update(String title, String description, TextbookCategory category,
                       Long fileId, String fileName, String fileUrl, Long fileSize,
                       Boolean isImportant) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (category != null) {
            this.category = category;
        }
        if (fileId != null) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileUrl = fileUrl;
            this.fileSize = fileSize;
        }
        if (isImportant != null) {
            this.isImportant = isImportant;
        }
    }

    public void incrementDownloadCount() {
        this.downloadCount++;
    }

    public boolean hasFile() {
        return this.fileId != null && this.fileUrl != null;
    }
}
