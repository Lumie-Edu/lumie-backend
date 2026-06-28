package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.request.UpdateSmsSenderSettingApprovalRequest;
import com.lumie.notification.application.dto.request.UpsertSmsSenderSettingRequest;
import com.lumie.notification.application.dto.response.SmsSenderSettingResponse;
import com.lumie.notification.application.dto.response.SmsSenderSettingStatusResponse;
import com.lumie.notification.application.port.out.SmsSenderSettingPersistencePort;
import com.lumie.notification.domain.entity.SmsSenderSetting;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.vo.SmsSenderSettingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SmsSenderSettingService {

    static final String SOLAPI_PROVIDER = "solapi";

    private final SmsSenderSettingPersistencePort senderSettingPersistencePort;
    private final SmsProperties smsProperties;

    @Transactional(readOnly = true)
    public SmsSenderSettingResponse getSenderSetting() {
        return senderSettingPersistencePort.findByProvider(SOLAPI_PROVIDER)
                .map(SmsSenderSettingResponse::from)
                .orElseGet(() -> SmsSenderSettingResponse.notConfigured(SOLAPI_PROVIDER));
    }

    @Transactional(readOnly = true)
    public SmsSenderSettingStatusResponse getSenderSettingStatus() {
        String provider = smsProperties.provider();
        if (!SOLAPI_PROVIDER.equalsIgnoreCase(provider)) {
            return SmsSenderSettingStatusResponse.of(provider, SmsSenderSettingStatus.APPROVED);
        }
        return senderSettingPersistencePort.findByProvider(SOLAPI_PROVIDER)
                .map(setting -> SmsSenderSettingStatusResponse.of(
                        setting.getProvider(),
                        setting.getRegistrationStatus()
                ))
                .orElseGet(() -> SmsSenderSettingStatusResponse.of(
                        SOLAPI_PROVIDER,
                        SmsSenderSettingStatus.NOT_CONFIGURED
                ));
    }

    @Transactional
    public SmsSenderSettingResponse upsertSenderSetting(UpsertSmsSenderSettingRequest request) {
        SmsSenderSetting setting = senderSettingPersistencePort.findByProvider(SOLAPI_PROVIDER)
                .map(existing -> {
                    existing.submit(request.senderNumber());
                    return existing;
                })
                .orElseGet(() -> SmsSenderSetting.create(SOLAPI_PROVIDER, request.senderNumber()));
        return SmsSenderSettingResponse.from(senderSettingPersistencePort.save(setting));
    }

    @Transactional
    public SmsSenderSettingResponse updateApproval(UpdateSmsSenderSettingApprovalRequest request) {
        SmsSenderSetting setting = getRequiredSetting();
        if (request.registrationStatus() == UpdateSmsSenderSettingApprovalRequest.ApprovalStatus.APPROVED) {
            setting.approve();
        } else if (request.registrationStatus() == UpdateSmsSenderSettingApprovalRequest.ApprovalStatus.REJECTED) {
            setting.reject(request.rejectionReason());
        } else {
            throw new BusinessException(
                    SmsErrorCode.INVALID_SENDER_SETTING_STATUS,
                    "Approval endpoint only accepts APPROVED or REJECTED"
            );
        }
        return SmsSenderSettingResponse.from(senderSettingPersistencePort.save(setting));
    }

    @Transactional(readOnly = true)
    public String requireApprovedSenderNumber() {
        return senderSettingPersistencePort
                .findByProviderAndRegistrationStatus(SOLAPI_PROVIDER, SmsSenderSettingStatus.APPROVED)
                .map(SmsSenderSetting::getSenderNumber)
                .orElseThrow(() -> new BusinessException(
                        SmsErrorCode.SENDER_SETTING_NOT_APPROVED,
                        "Approved SMS sender number is required before sending"
                ));
    }

    @Transactional(readOnly = true)
    public String resolveSenderNumberForDispatch(String savedSenderNumber) {
        if (savedSenderNumber != null && !savedSenderNumber.isBlank()) {
            return savedSenderNumber;
        }
        return requireApprovedSenderNumber();
    }

    private SmsSenderSetting getRequiredSetting() {
        return senderSettingPersistencePort.findByProvider(SOLAPI_PROVIDER)
                .orElseThrow(() -> new BusinessException(
                        SmsErrorCode.SENDER_SETTING_NOT_FOUND,
                        "SMS sender setting not found for provider: " + SOLAPI_PROVIDER
                ));
    }
}
