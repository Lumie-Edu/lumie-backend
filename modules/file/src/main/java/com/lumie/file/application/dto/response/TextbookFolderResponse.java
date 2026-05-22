package com.lumie.file.application.dto.response;

import com.lumie.file.domain.entity.TextbookFolder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record TextbookFolderResponse(
        UUID id,
        String name,
        @Schema(nullable = true) UUID parentId,
        int depth,
        Instant createdAt
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
