package com.lumie.ai.application.port.out;

public interface SmsNotificationPort {

    void send(String phoneNumber, String message);
}
