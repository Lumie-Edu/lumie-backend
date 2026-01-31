package com.lumie.content.domain.entity;

import com.lumie.common.domain.BaseEntity;
import com.lumie.content.domain.vo.TextbookStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "textbooks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Textbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author", length = 100)
    private String author;

    @Column(name = "publisher", length = 100)
    private String publisher;

    @Column(name = "isbn", length = 20)
    private String isbn;

    @Column(name = "subject", length = 100)
    private String subject;

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "cover_image_path", length = 500)
    private String coverImagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TextbookStatus status;

    @Builder
    private Textbook(Long academyId, String name, String description, String author,
                     String publisher, String isbn, String subject, String gradeLevel,
                     BigDecimal price, String coverImagePath, TextbookStatus status) {
        this.academyId = academyId;
        this.name = name;
        this.description = description;
        this.author = author;
        this.publisher = publisher;
        this.isbn = isbn;
        this.subject = subject;
        this.gradeLevel = gradeLevel;
        this.price = price;
        this.coverImagePath = coverImagePath;
        this.status = status != null ? status : TextbookStatus.ACTIVE;
    }

    public static Textbook create(Long academyId, String name, String description, String author,
                                   String publisher, String isbn, String subject, String gradeLevel,
                                   BigDecimal price, String coverImagePath) {
        return Textbook.builder()
                .academyId(academyId)
                .name(name)
                .description(description)
                .author(author)
                .publisher(publisher)
                .isbn(isbn)
                .subject(subject)
                .gradeLevel(gradeLevel)
                .price(price)
                .coverImagePath(coverImagePath)
                .status(TextbookStatus.ACTIVE)
                .build();
    }

    public void update(String name, String description, String author, String publisher,
                       String isbn, String subject, String gradeLevel, BigDecimal price,
                       String coverImagePath) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (author != null) {
            this.author = author;
        }
        if (publisher != null) {
            this.publisher = publisher;
        }
        if (isbn != null) {
            this.isbn = isbn;
        }
        if (subject != null) {
            this.subject = subject;
        }
        if (gradeLevel != null) {
            this.gradeLevel = gradeLevel;
        }
        if (price != null) {
            this.price = price;
        }
        if (coverImagePath != null) {
            this.coverImagePath = coverImagePath;
        }
    }

    public void activate() {
        this.status = TextbookStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = TextbookStatus.INACTIVE;
    }

    public boolean isActive() {
        return this.status == TextbookStatus.ACTIVE;
    }
}
