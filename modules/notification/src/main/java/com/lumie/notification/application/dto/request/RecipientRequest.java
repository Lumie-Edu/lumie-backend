package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.Recipient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecipientRequest(
        @NotBlank(message = "Recipient phone is required")
        @Size(max = 30, message = "Recipient phone must be at most 30 characters")
        String phone,

        @NotBlank(message = "Recipient name is required")
        @Size(max = 100, message = "Recipient name must be at most 100 characters")
        String name,

        Long studentId,

        @Size(max = 100, message = "Recipient class name must be at most 100 characters")
        String className
) {
    public RecipientRequest(String phone, String name, Long studentId) {
        this(phone, name, studentId, null);
    }

    public Recipient toVo() {
        return Recipient.pending(phone, name, studentId, className, null);
    }
}
