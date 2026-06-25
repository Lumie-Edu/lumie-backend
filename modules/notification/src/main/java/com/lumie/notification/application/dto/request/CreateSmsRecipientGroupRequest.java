package com.lumie.notification.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSmsRecipientGroupRequest(
        @NotBlank(message = "Recipient group name is required")
        @Size(max = 200, message = "Recipient group name must be at most 200 characters")
        String name,

        @Size(max = 50, message = "Recipient group icon must be at most 50 characters")
        String icon,

        @NotEmpty(message = "Recipients list cannot be empty")
        @Size(max = 500, message = "Recipients list must be at most 500 entries")
        List<@Valid RecipientRequest> recipients
) {
}
