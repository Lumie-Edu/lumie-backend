package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.notification.application.dto.request.CreateSmsRecipientGroupRequest;
import com.lumie.notification.application.dto.response.SmsRecipientGroupResponse;
import com.lumie.notification.application.dto.response.SmsRecipientGroupSummaryResponse;
import com.lumie.notification.application.port.out.SmsRecipientGroupPersistencePort;
import com.lumie.notification.domain.entity.SmsRecipientGroup;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.staff.api.StaffService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SmsRecipientGroupService {

    private final SmsRecipientGroupPersistencePort groupPersistencePort;
    private final StaffService staffService;

    public SmsRecipientGroupResponse createGroup(CreateSmsRecipientGroupRequest request) {
        String slug = TenantContextHolder.getRequiredTenant();
        Long ownerStaffId = resolveCurrentStaffId(slug);
        List<Recipient> recipients = normalizeRecipients(request);
        validateRecipients(recipients);

        SmsRecipientGroup group = SmsRecipientGroup.create(
                ownerStaffId,
                request.name().trim(),
                request.icon(),
                recipients
        );

        return SmsRecipientGroupResponse.from(groupPersistencePort.save(group));
    }

    @Transactional(readOnly = true)
    public List<SmsRecipientGroupSummaryResponse> listGroupSummaries() {
        String slug = TenantContextHolder.getRequiredTenant();
        Long ownerStaffId = resolveCurrentStaffId(slug);
        return groupPersistencePort.findSummariesByOwnerStaffId(ownerStaffId).stream()
                .map(SmsRecipientGroupSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SmsRecipientGroupResponse getGroup(Long id) {
        String slug = TenantContextHolder.getRequiredTenant();
        Long ownerStaffId = resolveCurrentStaffId(slug);
        SmsRecipientGroup group = groupPersistencePort.findByIdAndOwnerStaffId(id, ownerStaffId)
                .orElseThrow(() -> new BusinessException(
                        SmsErrorCode.RECIPIENT_GROUP_NOT_FOUND,
                        "SMS recipient group not found with id: %d".formatted(id)
                ));
        return SmsRecipientGroupResponse.from(group);
    }

    private Long resolveCurrentStaffId(String slug) {
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(SmsErrorCode.SENDER_NOT_FOUND, "Current user is not authenticated");
        }
        return staffService.getStaffByUserId(slug, userId)
                .map(StaffService.StaffData::id)
                .orElseThrow(() -> new BusinessException(SmsErrorCode.SENDER_NOT_FOUND,
                        "Staff sender not found for current user"));
    }

    private List<Recipient> normalizeRecipients(CreateSmsRecipientGroupRequest request) {
        return request.recipients().stream()
                .map(recipientRequest -> recipientRequest.toVo())
                .map(recipient -> Recipient.pending(
                        PhoneNumberUtils.toDigits(recipient.phone()),
                        recipient.name(),
                        recipient.studentId(),
                        recipient.className(),
                        null
                ))
                .toList();
    }

    private void validateRecipients(List<Recipient> recipients) {
        if (recipients.isEmpty()) {
            throw new BusinessException(SmsErrorCode.EMPTY_RECIPIENTS, "Recipients list cannot be empty");
        }
        boolean hasInvalidPhone = recipients.stream()
                .anyMatch(recipient -> !PhoneNumberUtils.isValid(recipient.phone()));
        if (hasInvalidPhone) {
            throw new BusinessException(SmsErrorCode.INVALID_RECIPIENTS, "Recipients contain invalid phone numbers");
        }
    }
}
