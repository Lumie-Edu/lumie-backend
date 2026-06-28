package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.SmsStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsDispatchService {

    private final SmsMessageRepository messageRepository;
    private final SmsProviderRegistry providerRegistry;
    private final SmsProperties properties;
    private final TransactionTemplate transactionTemplate;

    public void dispatchAfterCommit(Long messageId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            String tenantSlug = TenantContextHolder.getRequiredTenant();
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        TenantContextHolder.withinContext(tenantSlug, tenantId, () -> dispatchMessage(messageId));
                    } catch (Exception e) {
                        log.error("SMS after-commit dispatch failed: messageId={}, error={}",
                                messageId, e.getMessage(), e);
                    }
                }
            });
            return;
        }
        dispatchMessage(messageId);
    }

    public void dispatchMessage(Long messageId) {
        SmsProviderPort provider = providerRegistry.provider();
        DispatchSnapshot snapshot = prepareDispatch(messageId, provider.providerName());
        if (snapshot == null) {
            return;
        }

        try {
            SmsProviderPort.SendResult result = provider.send(snapshot.toCommand());
            applyProviderResult(snapshot.messageId(), result);
        } catch (BusinessException e) {
            recordProviderFailure(snapshot.messageId(), provider.providerName(), e.getMessage());
        }
    }

    public int dispatchDueMessages(Instant now) {
        List<Long> ids = transactionTemplate.execute(status -> messageRepository.findDueDispatchIds(
                SmsStatus.PENDING,
                now,
                properties.maxDispatchAttempts(),
                PageRequest.of(0, properties.dispatchBatchSize())
        ));
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        ids.forEach(this::dispatchMessage);
        return ids.size();
    }

    private DispatchSnapshot prepareDispatch(Long messageId, String providerName) {
        return transactionTemplate.execute(status -> {
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(
                            SmsErrorCode.MESSAGE_NOT_FOUND,
                            "SMS message not found with id: " + messageId
                    ));
            if (message.getStatus() != SmsStatus.PENDING) {
                return null;
            }
            if (message.getDispatchAttemptCount() >= properties.maxDispatchAttempts()) {
                return null;
            }
            message.recordDispatchAttempt(providerName);
            messageRepository.save(message);
            return DispatchSnapshot.from(TenantContextHolder.getRequiredTenant(), message);
        });
    }

    private void applyProviderResult(Long messageId, SmsProviderPort.SendResult result) {
        transactionTemplate.executeWithoutResult(status -> {
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND));
            List<Recipient> updatedRecipients = mergeProviderResults(message.getRecipients(), result.recipients());
            applyAggregateState(message, result.provider(), result.providerGroupId(), updatedRecipients);
            messageRepository.save(message);
        });
    }

    private void recordProviderFailure(Long messageId, String providerName, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND));
            if (message.getDispatchAttemptCount() >= properties.maxDispatchAttempts()) {
                message.recordTerminalDispatchFailure(providerName, errorMessage);
            } else {
                message.recordRetryableDispatchFailure(providerName, errorMessage);
            }
            messageRepository.save(message);
        });
    }

    SmsMessageResponse applyWebhookReport(SolapiWebhookReport report) {
        return transactionTemplate.execute(status -> {
            SmsMessage message = messageRepository.findById(report.messageId())
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND));
            List<Recipient> recipients = message.getRecipients();
            if (report.recipientIndex() < 0 || report.recipientIndex() >= recipients.size()) {
                throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Recipient index is outside message recipients");
            }
            List<Recipient> updated = new java.util.ArrayList<>(recipients);
            Recipient current = updated.get(report.recipientIndex());
            updated.set(report.recipientIndex(), withProviderStatus(
                    current,
                    report.providerMessageId(),
                    report.statusCode(),
                    report.statusMessage()
            ));
            applyAggregateState(message, "solapi", report.providerGroupId(), updated);
            return SmsMessageResponse.from(messageRepository.save(message));
        });
    }

    private List<Recipient> mergeProviderResults(
            List<Recipient> recipients,
            List<SmsProviderPort.RecipientResult> results
    ) {
        List<Recipient> updated = new java.util.ArrayList<>(recipients);
        results.stream()
                .filter(result -> result.index() >= 0 && result.index() < updated.size())
                .sorted(Comparator.comparingInt(SmsProviderPort.RecipientResult::index))
                .forEach(result -> updated.set(result.index(), withProviderStatus(
                        updated.get(result.index()),
                        result.providerMessageId(),
                        result.statusCode(),
                        result.statusMessage()
                )));
        return List.copyOf(updated);
    }

    private Recipient withProviderStatus(Recipient recipient, String providerMessageId,
                                         String statusCode, String statusMessage) {
        if ("4000".equals(statusCode)) {
            return recipient.sent(providerMessageId, statusCode, statusMessage);
        }
        if (statusCode != null && statusCode.startsWith("5")) {
            return recipient.failed(statusMessage, providerMessageId, statusCode, statusMessage);
        }
        return recipient.dispatched(providerMessageId, statusCode, statusMessage);
    }

    private void applyAggregateState(SmsMessage message, String provider, String providerGroupId,
                                     List<Recipient> recipients) {
        long dispatched = recipients.stream()
                .filter(recipient -> recipient.deliveryStatus() == SmsStatus.DISPATCHED
                        || recipient.deliveryStatus() == SmsStatus.PENDING)
                .count();
        int success = (int) recipients.stream()
                .filter(recipient -> recipient.deliveryStatus() == SmsStatus.SENT)
                .count();
        int failed = (int) recipients.stream()
                .filter(recipient -> recipient.deliveryStatus() == SmsStatus.FAILED)
                .count();

        if (dispatched > 0) {
            message.markAsDispatched(provider, providerGroupId, recipients, "DISPATCHED");
            return;
        }
        message.markAsDispatched(provider, providerGroupId, recipients, "DISPATCHED");
        if (failed == 0) {
            message.markAsSent(recipients);
        } else if (success == 0) {
            message.markAsFailed(recipients);
        } else {
            message.markAsPartial(recipients, success, failed);
        }
    }

    public record SolapiWebhookReport(
            Long messageId,
            int recipientIndex,
            String providerMessageId,
            String providerGroupId,
            String statusCode,
            String statusMessage
    ) {
    }

    private record DispatchSnapshot(
            Long messageId,
            String tenantSlug,
            String from,
            com.lumie.notification.domain.vo.SmsMessageType messageType,
            List<SmsProviderPort.RecipientCommand> recipients
    ) {
        static DispatchSnapshot from(String tenantSlug, SmsMessage message) {
            List<SmsProviderPort.RecipientCommand> recipients = new java.util.ArrayList<>();
            for (int i = 0; i < message.getRecipients().size(); i++) {
                Recipient recipient = message.getRecipients().get(i);
                recipients.add(new SmsProviderPort.RecipientCommand(
                        i,
                        recipient.phone(),
                        Objects.requireNonNullElse(recipient.renderedContent(), message.getContent())
                ));
            }
            return new DispatchSnapshot(
                    message.getId(),
                    tenantSlug,
                    null,
                    message.getMessageType(),
                    List.copyOf(recipients)
            );
        }

        SmsProviderPort.SendCommand toCommand() {
            return new SmsProviderPort.SendCommand(messageId, tenantSlug, from, messageType, recipients);
        }
    }
}
