package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.vo.Recipient;
import io.swagger.v3.oas.annotations.media.Schema;

public record RecipientResponse(
        String phone,
        String name,
        @Schema(nullable = true) Long studentId
) {
    public static RecipientResponse from(Recipient vo) {
        if (vo == null) {
            return null;
        }
        return new RecipientResponse(vo.phone(), vo.name(), vo.studentId());
    }
}
