package com.lumie.content.application.dto.request;

import java.util.List;
import java.util.UUID;
import com.lumie.file.api.TemporaryAttachment;

public record UpdateQnaRequest(
        String qnaTitle,
        String qnaContent,
        List<UUID> attachmentFileIds,
        List<TemporaryAttachment> temporaryAttachments
) {
}
