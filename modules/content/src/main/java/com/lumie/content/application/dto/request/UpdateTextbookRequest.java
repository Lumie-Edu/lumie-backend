package com.lumie.content.application.dto.request;

import java.math.BigDecimal;

public record UpdateTextbookRequest(
        String name,
        String description,
        String author,
        String publisher,
        String isbn,
        String subject,
        String gradeLevel,
        BigDecimal price,
        String coverImagePath
) {
}
