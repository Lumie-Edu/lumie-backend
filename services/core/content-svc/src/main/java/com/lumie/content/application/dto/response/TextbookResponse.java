package com.lumie.content.application.dto.response;

import com.lumie.content.domain.entity.Textbook;
import com.lumie.content.domain.vo.TextbookCategory;

import java.time.LocalDateTime;

public record TextbookResponse(
        Long id,
        String title,
        String description,
        TextbookCategory category,
        Long fileId,
        String fileName,
        String fileUrl,
        Long fileSize,
        Long authorId,
        String authorName,
        Boolean isImportant,
        Integer downloadCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TextbookResponse from(Textbook textbook) {
        return new TextbookResponse(
                textbook.getId(),
                textbook.getTitle(),
                textbook.getDescription(),
                textbook.getCategory(),
                textbook.getFileId(),
                textbook.getFileName(),
                textbook.getFileUrl(),
                textbook.getFileSize(),
                textbook.getAuthorId(),
                textbook.getAuthorName(),
                textbook.getIsImportant(),
                textbook.getDownloadCount(),
                textbook.getCreatedAt(),
                textbook.getUpdatedAt()
        );
    }
}
