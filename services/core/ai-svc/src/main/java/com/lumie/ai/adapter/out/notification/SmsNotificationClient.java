package com.lumie.ai.adapter.out.notification;

import com.lumie.ai.application.port.out.SmsNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SmsNotificationClient implements SmsNotificationPort {

    @Override
    public void send(String phoneNumber, String message) {
        // SMS provider not yet configured - stub implementation
        log.info("[SMS STUB] To: {}, Message: {}", phoneNumber, message);
    }
}
