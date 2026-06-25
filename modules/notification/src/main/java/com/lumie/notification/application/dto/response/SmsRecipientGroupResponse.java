package com.lumie.notification.application.dto.response;

import com.lumie.notification.domain.entity.SmsRecipientGroup;

import java.time.Instant;
import java.util.List;

public record SmsRecipientGroupResponse(
        Long id,
        String name,
        String icon,
        List<RecipientResponse> recipients,
        Instant createdAt,
        Instant updatedAt
) {
    public static SmsRecipientGroupResponse from(SmsRecipientGroup group) {
        return new SmsRecipientGroupResponse(
                group.getId(),
                group.getName(),
                group.getIcon(),
                group.getRecipients().stream().map(RecipientResponse::from).toList(),
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
