package com.lumie.notification.domain.vo;

public record Recipient(
        String phone,
        String name,
        Long studentId,
        String className,
        String renderedContent,
        SmsStatus deliveryStatus,
        String failureReason
) {
    public Recipient(String phone, String name, Long studentId) {
        this(phone, name, studentId, null, null, SmsStatus.PENDING, null);
    }

    public static Recipient pending(String phone, String name, Long studentId, String className, String renderedContent) {
        return new Recipient(phone, name, studentId, className, renderedContent, SmsStatus.PENDING, null);
    }

    public Recipient sent() {
        return new Recipient(phone, name, studentId, className, renderedContent, SmsStatus.SENT, null);
    }

    public Recipient failed(String reason) {
        return new Recipient(phone, name, studentId, className, renderedContent, SmsStatus.FAILED, reason);
    }
}
