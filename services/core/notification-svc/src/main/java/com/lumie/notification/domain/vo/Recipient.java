package com.lumie.notification.domain.vo;

public record Recipient(
        String phone,
        String name,
        Long studentId
) {
}
