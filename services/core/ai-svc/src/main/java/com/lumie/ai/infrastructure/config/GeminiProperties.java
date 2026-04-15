package com.lumie.ai.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai";
    private String model = "gemini-2.5-flash";
    private int maxTokens = 4096;
    private double temperature = 0.7;

    // -1 = dynamic (model decides), 0 = disabled, >0 = explicit cap
    private int thinkingBudget = -1;
}
