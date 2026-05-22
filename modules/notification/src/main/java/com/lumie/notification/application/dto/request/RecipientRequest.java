package com.lumie.notification.application.dto.request;

import com.lumie.notification.domain.vo.Recipient;
import jakarta.validation.constraints.NotBlank;

public record RecipientRequest(
        @NotBlank(message = "Recipient phone is required")
        String phone,

        @NotBlank(message = "Recipient name is required")
        String name,

        Long studentId
) {
    public Recipient toVo() {
        return new Recipient(phone, name, studentId);
    }
}
