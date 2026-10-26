package com.lumie.file.application.dto.request;

import com.lumie.file.api.TemporaryAttachment;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

public record RegisterTextbookFilesRequest(UUID folderId, List<@Valid TemporaryAttachment> attachments,
                                           List<UUID> uploadSessionIds) {
    public RegisterTextbookFilesRequest(UUID folderId, List<@Valid TemporaryAttachment> attachments) {
        this(folderId, attachments, null);
    }
}
