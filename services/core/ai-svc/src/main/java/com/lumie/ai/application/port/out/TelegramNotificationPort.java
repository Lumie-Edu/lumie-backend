package com.lumie.ai.application.port.out;

public interface TelegramNotificationPort {

    void send(String chatId, String message);
}
