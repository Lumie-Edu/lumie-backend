package com.lumie.ai.adapter.out.config;

import com.lumie.ai.application.service.ChatSystemPrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AiChatConfig {

    /**
     * Spring-managed executor for SSE chat streaming. Replaces the previous
     * ad-hoc {@code Executors.newVirtualThreadPerTaskExecutor()} instance
     * field so the threads participate in container lifecycle (graceful
     * shutdown) and carry a recognizable name in thread dumps / metrics.
     */
    @Bean
    public AsyncTaskExecutor aiChatExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-chat-");
        executor.setVirtualThreads(true);
        return executor;
    }

    @Bean
    public ChatSystemPrompt chatSystemPrompt(
            @Value("classpath:prompts/chat-system.txt") Resource resource) {
        try {
            String prefix = resource.getContentAsString(StandardCharsets.UTF_8);
            return new ChatSystemPrompt(prefix);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Failed to load AI chat system prompt from " + resource.getDescription(), e);
        }
    }
}
