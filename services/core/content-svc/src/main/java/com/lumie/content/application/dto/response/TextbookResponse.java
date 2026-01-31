package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.vo.TextbookStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TextbookResponse(
        Long id,
        Long academyId,
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TextbookResponse from(Textbook textbook) {
        return new TextbookResponse(
                textbook.getId(),
                textbook.getAcademyId(),
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
                textbook.getCreatedAt(),
                textbook.getUpdatedAt()
        );
    }
}
