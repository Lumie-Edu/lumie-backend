package com.lumie.notification.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpsertSmsSenderSettingRequest(
        @NotBlank(message = "Sender number is required")
        @Size(max = 20, message = "Sender number must be at most 20 characters")
        @Pattern(
                regexp = "^(?:\\d{10,11}|\\d{2,3}-\\d{3,4}-\\d{4})$",
                message = "Sender number must be a valid Korean phone number"
        )
        String senderNumber
) {
}
