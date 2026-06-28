package com.lumie.notification.adapter.out.external;

import com.lumie.common.util.PhoneNumberUtils;
import com.lumie.notification.application.port.out.SmsProviderPort;
import org.springframework.stereotype.Component;

@Component
public class MockSmsProviderAdapter implements SmsProviderPort {

    @Override
    public String providerName() {
        return "mock";
    }

    @Override
    public void validateConfiguration() {
        // No external credentials required.
    }

    @Override
    public SendResult send(SendCommand command) {
        return new SendResult(providerName(), "mock-" + command.messageId(), command.recipients().stream()
                .map(recipient -> {
                    boolean valid = PhoneNumberUtils.isValid(recipient.to());
                    return new RecipientResult(
                            recipient.index(),
                            "mock-" + command.messageId() + "-" + recipient.index(),
                            valid ? "4000" : "5000",
                            valid ? "MOCK_DELIVERED" : "INVALID_PHONE"
                    );
                })
                .toList());
    }
}
