package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.vo.TextbookStatus;

import java.math.BigDecimal;
import java.time.Instant;
import com.lumie.common.util.Timestamps;

public record TextbookResponse(
        Long id,
        String name,
        String description,
        String author,
        String publisher,
        String isbn,
        String subject,
        String gradeLevel,
        BigDecimal price,
        String coverImagePath,
        TextbookStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TextbookResponse from(Textbook textbook) {
        return new TextbookResponse(
                textbook.getId(),
                textbook.getName(),
                textbook.getDescription(),
                textbook.getAuthor(),
                textbook.getPublisher(),
                textbook.getIsbn(),
                textbook.getSubject(),
                textbook.getGradeLevel(),
                textbook.getPrice(),
                textbook.getCoverImagePath(),
                textbook.getStatus(),
                Timestamps.utcInstant(textbook.getCreatedAt()),
                Timestamps.utcInstant(textbook.getUpdatedAt())
        );
    }
}
