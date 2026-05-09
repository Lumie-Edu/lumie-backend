package com.lumie.ai.adapter.out.notification;

import com.lumie.ai.application.port.out.TelegramNotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Component
public class TelegramNotificationClient implements TelegramNotificationPort {

    private final WebClient webClient;
    private final String botToken;

    public TelegramNotificationClient(
            WebClient.Builder webClientBuilder,
            @Value("${telegram.bot-token:}") String botToken) {
        this.webClient = webClientBuilder.build();
        this.botToken = botToken;
    }

    @Override
    public void send(String chatId, String message) {
        if (botToken == null || botToken.isBlank()) {
            log.info("[TELEGRAM STUB] ChatId: {}, Message: {}", chatId, message);
            return;
        }

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        webClient.post()
                .uri(url)
                .bodyValue(Map.of(
                        "chat_id", chatId,
                        "text", message,
                        "parse_mode", "Markdown"
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();

        log.info("Telegram message sent to chat {}", chatId);
    }
}
