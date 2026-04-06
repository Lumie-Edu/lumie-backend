package com.lumie.notification.application.service;

import com.lumie.notification.application.dto.SmsMessageResponse;
import com.lumie.notification.application.dto.SmsTemplateResponse;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.exception.SmsMessageNotFoundException;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.repository.SmsTemplateRepository;
import com.lumie.notification.domain.vo.RecipientType;
import com.lumie.notification.domain.vo.SmsSearchCriteria;
import com.lumie.notification.domain.vo.SmsStatus;
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

    public SmsMessageResponse getMessage(Long id) {
        SmsMessage message = messageRepository.findById(id)
            .orElseThrow(() -> new SmsMessageNotFoundException(id));

        return SmsMessageResponse.from(message);
    }

    public Page<SmsMessageResponse> listMessages(Long academyId, SmsStatus status,
                                                   RecipientType recipientType, String search,
                                                   Pageable pageable) {
        SmsSearchCriteria criteria = new SmsSearchCriteria(academyId, status, recipientType, search);

        return messageRepository.findAll(criteria, pageable)
            .map(SmsMessageResponse::from);
    }

    public Page<SmsTemplateResponse> listTemplates(Long academyId, Pageable pageable) {
        return templateRepository.findByAcademyId(academyId, pageable)
            .map(SmsTemplateResponse::from);
    }
}
