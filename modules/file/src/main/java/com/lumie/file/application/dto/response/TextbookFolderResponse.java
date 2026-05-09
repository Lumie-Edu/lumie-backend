package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.TextbookFolder;

import java.time.LocalDateTime;
import java.util.UUID;

public record TextbookFolderResponse(
        UUID id,
        String name,
        UUID parentId,
        int depth,
        LocalDateTime createdAt
) {
    public static TextbookFolderResponse from(TextbookFolder folder) {
        return new TextbookFolderResponse(
                folder.getId(),
                folder.getName(),
                folder.getParentId(),
                folder.getDepth(),
                folder.getCreatedAt()
        );
    }
}
