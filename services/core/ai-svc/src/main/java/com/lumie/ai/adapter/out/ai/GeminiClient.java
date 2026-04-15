package com.lumie.ai.adapter.out.ai;

import com.lumie.ai.application.port.out.AiServicePort;
import com.lumie.ai.domain.exception.AiErrorCode;
import com.lumie.ai.domain.exception.AiException;
import com.lumie.ai.infrastructure.config.GeminiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiClient implements AiServicePort {

    private final WebClient webClient;
    private final GeminiProperties properties;

    public GeminiClient(GeminiProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", properties.getModel());
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", properties.getMaxTokens());
        requestBody.put("temperature", properties.getTemperature());

        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
            requestBody.put("tool_choice", "auto");
        }

        try {
            Map<String, Object> response = webClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new AiException(AiErrorCode.GEMINI_API_ERROR, "Empty response from Gemini");
            }

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new AiException(AiErrorCode.GEMINI_API_ERROR, "No choices in Gemini response");
            }

            return (Map<String, Object>) choices.get(0).get("message");
        } catch (AiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new AiException(AiErrorCode.GEMINI_API_ERROR, "Gemini API call failed: " + e.getMessage());
        }
    }
}
