package com.lumie.notification.application.service;

import com.lumie.common.auth.Role;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tenant.UserContextHolder;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.dto.response.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.common.exception.BusinessException;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import com.lumie.notification.domain.vo.SmsStatus;
import com.lumie.staff.api.StaffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmsQueryService {

    private final SmsMessageRepository messageRepository;
    private final SmsTemplateRepository templateRepository;
    private final StaffService staffService;

    public SmsMessageResponse getMessage(Long id) {
        Long scopedSenderId = scopedSenderId();
        SmsMessage message = (scopedSenderId == null
                ? messageRepository.findById(id)
                : messageRepository.findByIdAndSenderId(id, scopedSenderId))
            .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND, "SMS message not found with id: %d".formatted(id)));

        return SmsMessageResponse.from(message);
    }

    public Page<SmsMessageResponse> listMessages(SmsStatus status,
                                                   RecipientType recipientType, String search, Long recipientGroupId,
                                                   Pageable pageable) {
        SmsSearchCriteria criteria = new SmsSearchCriteria(status, recipientType, search, recipientGroupId, scopedSenderId());

        return messageRepository.findAll(criteria, pageable)
            .map(SmsMessageResponse::summary);
    }

    public Page<SmsTemplateResponse> listTemplates(Pageable pageable) {
        return templateRepository.findAll(pageable)
            .map(SmsTemplateResponse::from);
    }

    private Long scopedSenderId() {
        Role role = UserContextHolder.getUserRole();
        if (role == Role.OWNER || role == Role.MANAGER) {
            return null;
        }
        String slug = TenantContextHolder.getRequiredTenant();
        Long userId = UserContextHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(SmsErrorCode.SENDER_NOT_FOUND, "Current user is not authenticated");
        }
        return staffService.getStaffByUserId(slug, userId)
                .map(StaffService.StaffData::id)
                .orElseThrow(() -> new BusinessException(SmsErrorCode.SENDER_NOT_FOUND,
                        "Staff sender not found for current user"));
    }
}
