package com.lumie.notification.domain.vo;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.exception.SmsErrorCode;

public record Recipient(
        String phone,
        String name,
        Long studentId,
        String className,
        RecipientRole recipientRole,
        String renderedContent,
        SmsStatus deliveryStatus,
        String failureReason
) {
    public static Recipient pending(String phone, String name, Long studentId,
                                    String className, RecipientRole recipientRole, String renderedContent) {
        if (recipientRole == null) {
            throw new BusinessException(SmsErrorCode.INVALID_RECIPIENTS, "Recipient role is required");
        }
        return new Recipient(phone, name, studentId, className,
                recipientRole,
                renderedContent, SmsStatus.PENDING, null);
    }

    public Recipient sent() {
        return new Recipient(phone, name, studentId, className, recipientRole, renderedContent, SmsStatus.SENT, null);
    }

    public Recipient failed(String reason) {
        return new Recipient(phone, name, studentId, className, recipientRole, renderedContent, SmsStatus.FAILED, reason);
    }
}
