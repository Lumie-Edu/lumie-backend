package com.lumie.notification.application.port.out;

import com.lumie.notification.domain.vo.SmsMessageType;

import java.util.List;

public interface SmsProviderPort {

    String providerName();

    void validateConfiguration();

    SendResult send(SendCommand command);

    record SendCommand(
            Long messageId,
            String tenantSlug,
            String from,
            SmsMessageType messageType,
            List<RecipientCommand> recipients
    ) {
    }

    record RecipientCommand(
            int index,
            String to,
            String text
    ) {
    }

    record SendResult(
            String provider,
            String providerGroupId,
            List<RecipientResult> recipients
    ) {
    }

    record RecipientResult(
            int index,
            String providerMessageId,
            String statusCode,
            String statusMessage
    ) {
    }
}
