package com.lumie.notification.application.port.out;

import com.lumie.notification.domain.entity.SmsRecipientGroup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SmsRecipientGroupPersistencePort {

    SmsRecipientGroup save(SmsRecipientGroup group);

    Optional<SmsRecipientGroup> findByIdAndOwnerStaffId(Long id, Long ownerStaffId);

    List<SmsRecipientGroupSummaryData> findSummariesByOwnerStaffId(Long ownerStaffId);

    record SmsRecipientGroupSummaryData(
            Long id,
            String name,
            String icon,
            int recipientCount,
            int studentRecipientCount,
            int parentRecipientCount,
            Instant createdAt,
            Instant updatedAt
    ) {
    }
}
