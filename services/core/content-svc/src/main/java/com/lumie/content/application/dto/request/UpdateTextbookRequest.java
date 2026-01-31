package com.lumie.content.application.dto.request;

import com.lumie.content.domain.vo.TextbookCategory;

public record UpdateTextbookRequest(
        String title,
        String description,
        TextbookCategory category,
        Long fileId,
        String fileName,
        String fileUrl,
        Long fileSize,
        Boolean isImportant
) {
}
