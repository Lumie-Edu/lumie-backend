package com.lumie.notification.application.service;

import com.lumie.common.exception.BusinessException;
import com.lumie.common.tenant.RlsTenantTransactionBinder;
import com.lumie.common.tenant.TenantContextHolder;
import com.lumie.common.tx.RequiresNewTransaction;
import com.lumie.notification.adapter.out.config.SmsProperties;
import com.lumie.notification.application.dto.response.SmsMessageResponse;
import com.lumie.notification.application.port.out.SmsProviderPort;
import com.lumie.notification.domain.entity.SmsMessage;
import com.lumie.notification.domain.exception.SmsErrorCode;
import com.lumie.notification.domain.repository.SmsMessageRepository;
import com.lumie.notification.domain.vo.Recipient;
import com.lumie.notification.domain.vo.SmsStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class SmsDispatchService {

    private static final Duration DISPATCH_CLAIM_TIMEOUT = Duration.ofMinutes(5);

    private final SmsMessageRepository messageRepository;
    private final SmsProviderRegistry providerRegistry;
    private final SmsProperties properties;
    private final SmsSenderSettingService senderSettingService;
    private final TransactionTemplate dispatchTransactionTemplate;
    private final RlsTenantTransactionBinder rlsTenantTransactionBinder;

    public SmsDispatchService(
            SmsMessageRepository messageRepository,
            SmsProviderRegistry providerRegistry,
            SmsProperties properties,
            SmsSenderSettingService senderSettingService,
            @RequiresNewTransaction TransactionTemplate dispatchTransactionTemplate,
            RlsTenantTransactionBinder rlsTenantTransactionBinder
    ) {
        this.messageRepository = messageRepository;
        this.providerRegistry = providerRegistry;
        this.properties = properties;
        this.senderSettingService = senderSettingService;
        this.dispatchTransactionTemplate = dispatchTransactionTemplate;
        this.rlsTenantTransactionBinder = rlsTenantTransactionBinder;
    }

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
        String providerName = provider.providerName();
        DispatchSnapshot snapshot;
        try {
            snapshot = prepareDispatch(messageId, providerName);
        } catch (BusinessException e) {
            recordProviderFailure(messageId, providerName, e.getMessage());
            return;
        }
        if (snapshot == null) {
            return;
        }

        try {
            SmsProviderPort.SendResult result = provider.send(snapshot.toCommand());
            applyProviderResult(snapshot.messageId(), result);
        } catch (BusinessException e) {
            recordProviderFailure(snapshot.messageId(), providerName, e.getMessage());
        }
    }

    public int dispatchDueMessages(Instant now) {
        List<Long> ids = executeDispatchTransaction(status -> messageRepository.findDueDispatchIds(
                SmsStatus.PENDING,
                now,
                properties.maxDispatchAttempts(),
                now.minus(DISPATCH_CLAIM_TIMEOUT),
                PageRequest.of(0, properties.dispatchBatchSize())
        ));
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        ids.forEach(this::dispatchMessage);
        return ids.size();
    }

    private DispatchSnapshot prepareDispatch(Long messageId, String providerName) {
        return executeDispatchTransaction(status -> {
            Instant now = Instant.now();
            int claimed = messageRepository.claimDispatchAttempt(
                    messageId,
                    SmsStatus.PENDING,
                    providerName,
                    properties.maxDispatchAttempts(),
                    now.minus(DISPATCH_CLAIM_TIMEOUT),
                    now
            );
            if (claimed == 0) {
                return null;
            }
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(
                            SmsErrorCode.MESSAGE_NOT_FOUND,
                            "SMS message not found with id: " + messageId
                    ));
            return DispatchSnapshot.from(
                    TenantContextHolder.getRequiredTenant(),
                    message,
                    resolveSenderNumberForDispatch(message, providerName)
            );
        });
    }

    private String resolveSenderNumberForDispatch(SmsMessage message, String providerName) {
        if ("mock".equalsIgnoreCase(providerName)) {
            return message.getSenderNumber();
        }
        return senderSettingService.resolveSenderNumberForDispatch(message.getSenderNumber());
    }

    private void applyProviderResult(Long messageId, SmsProviderPort.SendResult result) {
        executeDispatchTransaction(status -> {
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND));
            List<Recipient> updatedRecipients = mergeProviderResults(message.getRecipients(), result.recipients());
            applyAggregateState(message, result.provider(), result.providerGroupId(), updatedRecipients);
            messageRepository.save(message);
            return null;
        });
    }

    private void recordProviderFailure(Long messageId, String providerName, String errorMessage) {
        executeDispatchTransaction(status -> {
            SmsMessage message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.MESSAGE_NOT_FOUND));
            if (message.getDispatchAttemptCount() >= properties.maxDispatchAttempts()) {
                message.recordTerminalDispatchFailure(providerName, errorMessage);
            } else {
                message.recordRetryableDispatchFailure(providerName, errorMessage);
            }
            messageRepository.save(message);
            return null;
        });
    }

    SmsMessageResponse applyWebhookReport(SolapiWebhookReport report) {
        return executeDispatchTransaction(status -> {
            SmsMessage message = messageRepository.findByProviderGroupId(report.providerGroupId())
                    .orElseThrow(() -> new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook provider group not found"));
            validateWebhookBinding(message, report);
            List<Recipient> recipients = message.getRecipients();
            if (report.recipientIndex() < 0 || report.recipientIndex() >= recipients.size()) {
                throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Recipient index is outside message recipients");
            }
            List<Recipient> updated = new java.util.ArrayList<>(recipients);
            Recipient current = updated.get(report.recipientIndex());
            validateRecipientBinding(current, report.providerMessageId());
            if (isTerminal(current.deliveryStatus())) {
                return SmsMessageResponse.from(message);
            }
            updated.set(report.recipientIndex(), withProviderStatus(current, report));
            applyAggregateState(message, "solapi", report.providerGroupId(), updated);
            return SmsMessageResponse.from(messageRepository.save(message));
        });
    }

    private <T> T executeDispatchTransaction(TransactionCallback<T> callback) {
        return dispatchTransactionTemplate.execute(status -> {
            rlsTenantTransactionBinder.bindRequiredTenantToCurrentTransaction();
            return callback.doInTransaction(status);
        });
    }

    private void validateWebhookBinding(SmsMessage message, SolapiWebhookReport report) {
        if (!"solapi".equals(message.getProvider())) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook provider does not match message provider");
        }
        if (!Objects.equals(message.getId(), report.messageId())) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook message id does not match provider group");
        }
        if (!Objects.equals(message.getProviderGroupId(), report.providerGroupId())) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook provider group does not match message");
        }
    }

    private void validateRecipientBinding(Recipient recipient, String providerMessageId) {
        if (!Objects.equals(recipient.providerMessageId(), providerMessageId)) {
            throw new BusinessException(SmsErrorCode.WEBHOOK_IGNORED, "Webhook provider message does not match recipient");
        }
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

    private Recipient withProviderStatus(Recipient recipient, SolapiWebhookReport report) {
        if (isTerminal(recipient.deliveryStatus())) {
            return recipient;
        }
        return withProviderStatus(
                recipient,
                report.providerMessageId(),
                report.statusCode(),
                report.statusMessage()
        );
    }

    private Recipient withProviderStatus(Recipient recipient, String providerMessageId,
                                         String statusCode, String statusMessage) {
        if ("4000".equals(statusCode)) {
            return recipient.sent(providerMessageId, statusCode, statusMessage);
        }
        if (statusCode != null && !"2000".equals(statusCode) && !"3000".equals(statusCode)) {
            return recipient.failed(statusMessage, providerMessageId, statusCode, statusMessage);
        }
        return recipient.dispatched(providerMessageId, statusCode, statusMessage);
    }

    private boolean isTerminal(SmsStatus status) {
        return status == SmsStatus.SENT || status == SmsStatus.FAILED;
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
        static DispatchSnapshot from(String tenantSlug, SmsMessage message, String from) {
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
                    from,
                    message.getMessageType(),
                    List.copyOf(recipients)
            );
        }

        SmsProviderPort.SendCommand toCommand() {
            return new SmsProviderPort.SendCommand(messageId, tenantSlug, from, messageType, recipients);
        }
    }
}
