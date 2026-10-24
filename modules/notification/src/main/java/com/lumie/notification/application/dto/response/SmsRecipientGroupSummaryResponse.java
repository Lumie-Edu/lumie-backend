package com.lumie.notification.application.dto.response;

import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;

import java.time.Instant;

public record SmsRecipientGroupSummaryResponse(
        Long id,
        String name,
        String icon,
        int recipientCount,
        int studentRecipientCount,
        int parentRecipientCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static SmsRecipientGroupSummaryResponse from(
            SmsRecipientGroupPersistencePort.SmsRecipientGroupSummaryData summary) {
        return new SmsRecipientGroupSummaryResponse(
                summary.id(),
                summary.name(),
                summary.icon(),
                summary.recipientCount(),
                summary.studentRecipientCount(),
                summary.parentRecipientCount(),
                summary.createdAt(),
                summary.updatedAt()
        );
    }
}
