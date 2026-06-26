package com.lumie.notification.adapter.out.persistence;

import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;
import com.lumie.notification.domain.entity.SmsRecipientGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SmsRecipientGroupPersistenceAdapter implements SmsRecipientGroupPersistencePort {

    private final SmsRecipientGroupJpaRepository smsRecipientGroupJpaRepository;

    @Override
    public SmsRecipientGroup save(SmsRecipientGroup group) {
        return smsRecipientGroupJpaRepository.save(group);
    }

    @Override
    public Optional<SmsRecipientGroup> findByIdAndOwnerStaffId(Long id, Long ownerStaffId) {
        return smsRecipientGroupJpaRepository.findByIdAndOwnerStaffId(id, ownerStaffId);
    }

    @Override
    public List<SmsRecipientGroupSummaryData> findSummariesByOwnerStaffId(Long ownerStaffId) {
        return smsRecipientGroupJpaRepository.findAllSummariesByOwnerStaffIdOrderByCreatedAtDesc(ownerStaffId)
                .stream()
                .map(summary -> new SmsRecipientGroupSummaryData(
                        summary.getId(),
                        summary.getName(),
                        summary.getIcon(),
                        summary.getRecipientCount(),
                        summary.getStudentRecipientCount(),
                        summary.getGuardianRecipientCount(),
                        summary.getCreatedAt(),
                        summary.getUpdatedAt()
                ))
                .toList();
    }
}
