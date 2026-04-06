package com.lumie.notification.domain.vo;

public record SmsSearchCriteria(
    Long academyId,
    SmsStatus status,
    RecipientType recipientType,
    String search
) {
}
