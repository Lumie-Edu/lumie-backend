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
        String failureReason,
        String providerMessageId,
        String providerStatusCode,
        String providerStatusMessage
) {
    public static Recipient pending(String phone, String name, Long studentId,
                                    String className, RecipientRole recipientRole, String renderedContent) {
        if (recipientRole == null) {
            throw new BusinessException(SmsErrorCode.INVALID_RECIPIENTS, "Recipient role is required");
        }
        return new Recipient(phone, name, studentId, className,
                recipientRole,
                renderedContent, SmsStatus.PENDING, null, null, null, null);
    }

    public Recipient dispatched(String providerMessageId, String statusCode, String statusMessage) {
        return new Recipient(phone, name, studentId, className, recipientRole, renderedContent,
                SmsStatus.DISPATCHED, null, providerMessageId, statusCode, statusMessage);
    }

    public Recipient sent() {
        return sent(providerMessageId, providerStatusCode, providerStatusMessage);
    }

    public Recipient sent(String providerMessageId, String statusCode, String statusMessage) {
        return new Recipient(phone, name, studentId, className, recipientRole, renderedContent,
                SmsStatus.SENT, null, providerMessageId, statusCode, statusMessage);
    }

    public Recipient failed(String reason) {
        return failed(reason, providerMessageId, providerStatusCode, providerStatusMessage);
    }

    public Recipient failed(String reason, String providerMessageId, String statusCode, String statusMessage) {
        return new Recipient(phone, name, studentId, className, recipientRole, renderedContent,
                SmsStatus.FAILED, reason, providerMessageId, statusCode, statusMessage);
    }
}
