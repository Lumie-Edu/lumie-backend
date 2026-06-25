package com.lumie.notification.domain.vo;

public record SmsSearchCriteria(
    SmsStatus status,
    RecipientType recipientType,
    String search,
    Long recipientGroupId,
    Long senderId
) {
}
