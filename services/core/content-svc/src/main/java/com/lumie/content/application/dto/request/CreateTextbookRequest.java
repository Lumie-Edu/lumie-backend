package com.lumie.content.application.dto.request;

import com.lumie.content.domain.vo.TextbookCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTextbookRequest(
        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Category is required")
        TextbookCategory category,

        Long fileId,

        String fileName,

        String fileUrl,

        Long fileSize,

        Long authorId,

        String authorName,

        Boolean isImportant
) {
}
