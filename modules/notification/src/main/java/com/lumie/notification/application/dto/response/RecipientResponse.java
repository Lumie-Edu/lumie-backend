package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.SmsStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record RecipientResponse(
        String phone,
        String name,
        @Schema(nullable = true) Long studentId,
        @Schema(nullable = true) String className,
        @Schema(nullable = true) SmsStatus deliveryStatus
) {
    public RecipientResponse(String phone, String name, Long studentId) {
        this(phone, name, studentId, null, null);
    }

    public static RecipientResponse from(Recipient vo) {
        if (vo == null) {
            return null;
        }
        return new RecipientResponse(
                maskPhone(vo.phone()),
                vo.name(),
                vo.studentId(),
                vo.className(),
                vo.deliveryStatus()
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return phone;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() < 7) {
            return phone;
        }
        return digits.substring(0, Math.min(3, digits.length()))
                + "-****-"
                + digits.substring(digits.length() - 4);
    }
}
