package com.lumie.file.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameTextbookFolderRequest(
        @NotBlank(message = "Folder name is required")
        @Size(max = 100, message = "Folder name must be 100 characters or less")
        String name
) {
}
